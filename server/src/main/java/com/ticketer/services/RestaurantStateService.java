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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RestaurantStateService {

    private final SettingsService settingsService;
    private final TicketService ticketService;
    private final ScheduledExecutorService scheduler;
    private final java.util.concurrent.ExecutorService closingExecutor;
    private final java.time.Clock clock;
    private boolean isOpen;

    @Autowired
    public RestaurantStateService(SettingsService settingsService, TicketService ticketService) {
        this(settingsService, ticketService, java.time.Clock.systemDefaultZone());
    }

    public RestaurantStateService(SettingsService settingsService, TicketService ticketService, java.time.Clock clock) {
        this.settingsService = settingsService;
        this.ticketService = ticketService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.closingExecutor = Executors.newSingleThreadExecutor();
        this.clock = clock;
        this.isOpen = false;
    }

    @PostConstruct
    public void init() {
        checkAndScheduleState();
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
        closingExecutor.shutdownNow();
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void checkAndScheduleState() {
        LocalDate today = LocalDate.now(clock);
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        String dayName = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toLowerCase();

        String openTimeStr = settingsService.getOpenTime(dayName);
        String closeTimeStr = settingsService.getCloseTime(dayName);

        if (openTimeStr == null || closeTimeStr == null) {
            setClosedState();
            scheduleNextDayCheck();
            return;
        }

        try {
            LocalTime openTime = LocalTime.parse(openTimeStr);
            LocalTime closeTime = LocalTime.parse(closeTimeStr);
            LocalTime now = LocalTime.now(clock);

            if (now.isBefore(openTime)) {
                setClosedState();
                long delay = java.time.Duration.between(now, openTime).toMillis();
                scheduler.schedule(this::handleOpening, delay, TimeUnit.MILLISECONDS);
            } else if (now.isAfter(openTime) && now.isBefore(closeTime)) {
                setOpenState();
                long delay = java.time.Duration.between(now, closeTime).toMillis();
                scheduler.schedule(this::handleClosing, delay, TimeUnit.MILLISECONDS);
            } else {
                if (isOpen) {
                    handleClosing();
                } else {
                    setClosedState();
                    scheduleNextDayCheck();
                    if (ticketService.hasActiveTickets()) {
                        closingExecutor.execute(this::runClosingSequence);
                    }
                }
            }

        } catch (DateTimeParseException e) {
            System.err.println("Error parsing time settings: " + e.getMessage());
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
            System.out.println("Restaurant is now OPEN.");
        }
    }

    private void setClosedState() {
        if (this.isOpen) {
            this.isOpen = false;
            System.out.println("Restaurant is now CLOSED (New tickets disabled).");
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
        System.out.println("Starting closing sequence...");

        long startTime = System.currentTimeMillis();
        long maxWaitTime = 60000;
        long checkInterval = 30000;

        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            if (ticketService.areAllTicketsClosed()) {
                break;
            }
            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Closing sequence interrupted. Proceeding to immediate cleanup.");
                break;
            }
        }

        System.out.println("Finalizing closing sequence. Moving remaining tickets to closed.");
        ticketService.moveAllToClosed();
        ticketService.serializeClosedTickets();
        ticketService.clearAllTickets();
        System.out.println("Closing sequence completed.");
    }
}
