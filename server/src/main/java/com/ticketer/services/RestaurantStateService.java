package com.ticketer.services;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RestaurantStateService {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantStateService.class);

    private final SettingsService settingsService;
    private final TicketService ticketService;
    private final ScheduledExecutorService scheduler;
    private final java.util.concurrent.ExecutorService closingExecutor;
    private final java.time.Clock clock;
    private boolean isOpen;
    private boolean forcedOpen;
    private LocalDate forcedClosedDate;

    public RestaurantStateService(SettingsService settingsService, TicketService ticketService) {
        this(settingsService, ticketService, java.time.Clock.systemUTC());
    }

    @Autowired

    public RestaurantStateService(SettingsService settingsService, TicketService ticketService, java.time.Clock clock) {
        this.settingsService = settingsService;
        this.ticketService = ticketService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.closingExecutor = Executors.newSingleThreadExecutor();
        this.clock = clock.withZone(java.time.ZoneId.systemDefault());
        this.isOpen = false;
        this.forcedOpen = false;
        this.forcedClosedDate = null;
    }

    @PostConstruct
    public void init() {
        checkAndScheduleState();
    }

    @PreDestroy
    public void cleanup() {
        scheduler.shutdownNow();
        closingExecutor.shutdownNow();
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void forceOpen() {
        forcedOpen = true;
        forcedClosedDate = null;
        setOpenState();
        checkAndScheduleState();
    }

    public void forceClose() {
        forcedOpen = false;
        forcedClosedDate = LocalDate.now(clock);
        handleClosing();
    }

    public void checkAndScheduleState() {
        LocalDate today = LocalDate.now(clock);

        if (forcedClosedDate != null) {
            if (forcedClosedDate.equals(today)) {
                if (isOpen) {
                    handleClosing();
                } else {
                    setClosedState();
                }
                scheduleNextDayCheck();
                return;
            } else {
                forcedClosedDate = null;
            }
        }

        DayOfWeek dayOfWeek = today.getDayOfWeek();
        String dayName = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toLowerCase();

        String openTimeStr = settingsService.getOpenTime(dayName);
        String closeTimeStr = settingsService.getCloseTime(dayName);

        logger.info("Scheduler check: Today is {} ({}), Current Time: {}", today, dayName, LocalTime.now(clock));
        logger.info("Configured Hours: Open={}, Close={}", openTimeStr, closeTimeStr);

        if (openTimeStr == null || closeTimeStr == null) {
            logger.info("No hours configured for today. Determining state based on force/default.");
            if (forcedOpen) {
                setOpenState();
                scheduleNextDayCheck();
            } else {
                setClosedState();
                scheduleNextDayCheck();
            }
            return;
        }

        try {
            LocalTime openTime = LocalTime.parse(openTimeStr);
            LocalTime closeTime = LocalTime.parse(closeTimeStr);
            LocalTime now = LocalTime.now(clock);

            if (now.isBefore(openTime)) {
                if (forcedOpen) {
                    logger.info(
                            "Current time {} is before open time {}. Forced OPEN. Scheduling next check at open time.",
                            now, openTime);
                    setOpenState();
                    long delay = java.time.Duration.between(now, openTime).toMillis();
                    scheduler.schedule(this::checkAndScheduleState, delay, TimeUnit.MILLISECONDS);
                } else {
                    logger.info("Current time {} is before open time {}. CLOSED. Scheduling opening at {}.", now,
                            openTime, openTime);
                    setClosedState();
                    long delay = java.time.Duration.between(now, openTime).toMillis();
                    scheduler.schedule(this::handleOpening, delay, TimeUnit.MILLISECONDS);
                }
            } else if (now.isAfter(openTime) && now.isBefore(closeTime)) {
                if (forcedOpen) {
                    forcedOpen = false;
                }
                logger.info("Current time {} must be open ({} - {}). OPEN. Scheduling closing at {}.", now, openTime,
                        closeTime, closeTime);
                setOpenState();
                long delay = java.time.Duration.between(now, closeTime).toMillis();
                scheduler.schedule(this::handleClosing, delay, TimeUnit.MILLISECONDS);
            } else {
                if (forcedOpen) {
                    logger.info("Current time {} is after close time {}. Forced OPEN. Scheduling next day check.", now,
                            closeTime);
                    setOpenState();
                    scheduleNextDayCheck();
                } else {
                    if (isOpen) {
                        logger.info("Current time {} is after close time {}. Closing now.", now, closeTime);
                        handleClosing();
                    } else {
                        logger.info("Current time {} is after close time {}. CLOSED. Scheduling next day check.", now,
                                closeTime);
                        setClosedState();
                        scheduleNextDayCheck();
                    }
                }
            }

        } catch (DateTimeParseException e) {
            logger.error("Error parsing time settings", e);
            setClosedState();
            scheduleNextDayCheck();
        }
    }

    private void scheduleNextDayCheck() {
        LocalTime now = LocalTime.now(clock);
        LocalTime midnight = LocalTime.MAX;
        long delay = java.time.Duration.between(now, midnight).toMillis() + 1000;
        scheduler.schedule(this::checkAndScheduleState, delay, TimeUnit.MILLISECONDS);
    }

    private void setOpenState() {
        if (!this.isOpen) {
            this.isOpen = true;
            logger.info("Restaurant is now OPEN.");
        }
    }

    private void setClosedState() {
        if (this.isOpen) {
            this.isOpen = false;
            logger.info("Restaurant is now CLOSED (New tickets disabled).");
        } else {
            this.isOpen = false;
        }
    }

    private void handleOpening() {
        setOpenState();
        checkAndScheduleState();
    }

    private void handleClosing() {
        setClosedState();
        closingExecutor.execute(this::runClosingSequence);
        scheduleNextDayCheck();
    }

    private void runClosingSequence() {
        logger.info("Starting closing sequence...");

        long startTime = System.currentTimeMillis();
        long maxWaitTime = 60000;
        long checkInterval = 30000;

        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            if (forcedClosedDate != null) {
                logger.warn("Forced shutdown detected. Skipping wait period.");
                break;
            }

            if (ticketService.areAllTicketsClosed()) {
                break;
            }
            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Closing sequence interrupted. Proceeding to immediate cleanup.");
                break;
            }
        }

        logger.info("Finalizing closing sequence. Moving remaining tickets to closed.");
        ticketService.moveAllToClosed();
        ticketService.serializeClosedTickets();
        ticketService.clearAllTickets();
        logger.info("Closing sequence completed.");
    }
}
