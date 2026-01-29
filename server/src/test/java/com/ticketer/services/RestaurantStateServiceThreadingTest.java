package com.ticketer.services;

import com.ticketer.repositories.SettingsRepository;
import com.ticketer.repositories.TicketRepository;
import com.ticketer.models.Settings;
import com.ticketer.models.Ticket;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class RestaurantStateServiceThreadingTest {

    @Test
    public void testClosingDoesNotBlock() throws InterruptedException {
        MockSettingsService settingsService = new MockSettingsService();
        BlockingTicketService ticketService = new BlockingTicketService();
        Clock fixedClock = Clock.fixed(Instant.parse("2023-01-02T18:00:00Z"), ZoneId.of("UTC"));

        RestaurantStateService service = new RestaurantStateService(settingsService, ticketService, fixedClock);
        settingsService.setHours("09:00 - 17:00");

        long start = System.currentTimeMillis();
        service.checkAndScheduleState();
        long end = System.currentTimeMillis();

        assertTrue("checkAndScheduleState should return immediately (< 100ms)", (end - start) < 100);

        assertTrue("Closing sequence should start in background", ticketService.latch.await(2, TimeUnit.SECONDS));

        service.cleanup();
    }

    private static class MockSettingsService extends SettingsService {
        private String hours = "09:00 - 17:00";

        public MockSettingsService() {
            super(new SettingsRepository() {
                public Settings getSettings() {
                    return null;
                }

                public void saveSettings(Settings s) {
                }
            });
        }

        public void setHours(String h) {
            this.hours = h;
        }

        @Override
        public String getOpenTime(String day) {
            return hours == null ? null : hours.split(" - ")[0];
        }

        @Override
        public String getCloseTime(String day) {
            return hours == null ? null : hours.split(" - ")[1];
        }
    }

    private static class BlockingTicketService extends TicketService {
        final CountDownLatch latch = new CountDownLatch(1);

        public BlockingTicketService() {
            super(new TicketRepository() {
                public Ticket save(Ticket t) {
                    return t;
                }

                public Optional<Ticket> findById(int id) {
                    return Optional.empty();
                }

                public List<Ticket> findAllActive() {
                    return Collections.emptyList();
                }

                public List<Ticket> findAllCompleted() {
                    return Collections.emptyList();
                }

                public List<Ticket> findAllClosed() {
                    return Collections.emptyList();
                }

                public boolean deleteById(int id) {
                    return true;
                }

                public void deleteAll() {
                }

                public void persistClosedTickets() {
                }

                public void moveToCompleted(int id) {
                }

                public void moveToClosed(int id) {
                }

                public void moveToActive(int id) {
                }
            });
        }

        @Override
        public boolean areAllTicketsClosed() {
            latch.countDown();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }

        @Override
        public boolean hasActiveTickets() {
            return true;
        }

        @Override
        public void moveAllToClosed() {
        }

        @Override
        public void serializeClosedTickets() {
        }

        @Override
        public void clearAllTickets() {
        }
    }
}
