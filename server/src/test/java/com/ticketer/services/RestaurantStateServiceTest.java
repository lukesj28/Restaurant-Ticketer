package com.ticketer.services;

import com.ticketer.repositories.SettingsRepository;
import com.ticketer.repositories.TicketRepository;
import com.ticketer.models.Settings;
import com.ticketer.models.Ticket;
import org.junit.Before;
import org.junit.Test;

import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RestaurantStateServiceTest {

    private MockSettingsService settingsService;
    private MockTicketService ticketService;
    private RestaurantStateService restaurantStateService;
    private Clock fixedClock;

    @Before
    public void setUp() {
        settingsService = new MockSettingsService();
        ticketService = new MockTicketService();
        fixedClock = Clock.fixed(Instant.parse("2023-01-02T12:00:00Z"), ZoneId.of("UTC"));
    }

    private void initService() {
        restaurantStateService = new RestaurantStateService(settingsService, ticketService, fixedClock);
    }

    @Test
    public void testClosedWhenNoHours() {
        settingsService.setHours(null);
        initService();
        restaurantStateService.init();
        assertFalse(restaurantStateService.isOpen());
    }

    @Test
    public void testClosedBeforeOpenTime() {
        settingsService.setHours("09:00 - 17:00");
        fixedClock = Clock.fixed(Instant.parse("2023-01-02T08:00:00Z"), ZoneId.of("UTC"));
        initService();

        restaurantStateService.checkAndScheduleState();
        assertFalse(restaurantStateService.isOpen());
    }

    @Test
    public void testOpenDuringHours() {
        settingsService.setHours("09:00 - 17:00");
        fixedClock = Clock.fixed(Instant.parse("2023-01-02T12:00:00Z"), ZoneId.of("UTC"));
        initService();

        restaurantStateService.checkAndScheduleState();
        assertTrue(restaurantStateService.isOpen());
    }

    @Test
    public void testClosedAfterHours() {
        settingsService.setHours("09:00 - 17:00");
        fixedClock = Clock.fixed(Instant.parse("2023-01-02T18:00:00Z"), ZoneId.of("UTC"));
        initService();

        restaurantStateService.checkAndScheduleState();

        assertFalse(restaurantStateService.isOpen());
    }

    @Test
    public void testTransitionsFromOpenToClosed() {
        settingsService.setHours("09:00 - 17:00");
        fixedClock = Clock.fixed(Instant.parse("2023-01-02T18:00:00Z"), ZoneId.of("UTC"));
        initService();

        restaurantStateService.checkAndScheduleState();
        assertFalse(restaurantStateService.isOpen());
    }

    @Test
    public void testParseError() {
        settingsService.setHours("invalid-time");
        initService();

        restaurantStateService.checkAndScheduleState();
        assertFalse(restaurantStateService.isOpen());
    }

    @Test
    public void testCleanup() {
        initService();
        restaurantStateService.cleanup();
    }

    @Test
    public void testForceClose() {
        settingsService.setHours("09:00 - 17:00");
        fixedClock = Clock.fixed(Instant.parse("2023-01-02T12:00:00Z"), ZoneId.of("UTC"));
        initService();

        restaurantStateService.checkAndScheduleState();
        assertTrue(restaurantStateService.isOpen());

        restaurantStateService.forceClose();
        assertFalse(restaurantStateService.isOpen());

        // Verify it stays closed
        restaurantStateService.checkAndScheduleState();
        assertFalse(restaurantStateService.isOpen());
    }

    @Test
    public void testForceOpen() {
        settingsService.setHours("09:00 - 17:00");
        fixedClock = Clock.fixed(Instant.parse("2023-01-02T19:00:00Z"), ZoneId.of("UTC")); // Closed time
        initService();

        restaurantStateService.checkAndScheduleState();
        assertFalse(restaurantStateService.isOpen());

        restaurantStateService.forceOpen();
        assertTrue(restaurantStateService.isOpen());

        restaurantStateService.checkAndScheduleState();
        assertTrue(restaurantStateService.isOpen());
    }

    @Test
    public void testClosingSequenceInterruptionCleanUp() throws InterruptedException {
        initService();
        ticketService.allClosed = false;

        Thread thread = new Thread(() -> {
            try {
                java.lang.reflect.Method method = RestaurantStateService.class.getDeclaredMethod("runClosingSequence");
                method.setAccessible(true);
                method.invoke(restaurantStateService);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread.start();
        Thread.sleep(200);

        thread.interrupt();
        thread.join(1000);

        assertTrue("moveAllToClosed should be called", ticketService.moveAllToClosedCalled);
        assertTrue("serializeClosedTickets should be called", ticketService.serializeClosedTicketsCalled);
        assertTrue("clearAllTickets should be called", ticketService.clearAllTicketsCalled);
    }

    @Test
    public void testClosingSequenceCompletesWhenAllClosed() throws Exception {
        initService();
        ticketService.allClosed = true;

        java.lang.reflect.Method method = RestaurantStateService.class.getDeclaredMethod("runClosingSequence");
        method.setAccessible(true);
        method.invoke(restaurantStateService);

        assertTrue("moveAllToClosed should be called", ticketService.moveAllToClosedCalled);
        assertTrue("serializeClosedTickets should be called", ticketService.serializeClosedTicketsCalled);
        assertTrue("clearAllTickets should be called", ticketService.clearAllTicketsCalled);
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
            if (hours == null || "closed".equals(hours))
                return null;
            try {
                return hours.split(" - ")[0];
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public String getCloseTime(String day) {
            if (hours == null || "closed".equals(hours))
                return null;
            try {
                return hours.split(" - ")[1];
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static class MockTicketService extends TicketService {
        boolean moveAllToClosedCalled = false;
        boolean serializeClosedTicketsCalled = false;
        boolean clearAllTicketsCalled = false;
        boolean allClosed = true;

        public MockTicketService() {
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

                public void clearRecoveryFile() {
                }
            });
        }

        @Override
        public boolean areAllTicketsClosed() {
            return allClosed;
        }

        @Override
        public boolean hasActiveTickets() {
            return !allClosed;
        }

        @Override
        public void moveAllToClosed() {
            moveAllToClosedCalled = true;
        }

        @Override
        public void serializeClosedTickets() {
            serializeClosedTicketsCalled = true;
        }

        @Override
        public void clearAllTickets() {
            clearAllTicketsCalled = true;
        }
    }
}
