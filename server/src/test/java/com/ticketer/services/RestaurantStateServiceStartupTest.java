package com.ticketer.services;

import com.ticketer.models.Settings;
import com.ticketer.repositories.SettingsRepository;
import org.junit.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class RestaurantStateServiceStartupTest {

    private final ZoneId zone = ZoneId.of("UTC");

    @Test
    public void testStartupDuringOpenHours() {
        SettingsService settingsService = createMockSettingsService("09:00 - 22:00");

        Clock clock = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), zone);

        MockTicketService ticketService = new MockTicketService();
        RestaurantStateService stateService = new RestaurantStateService(settingsService, ticketService, clock);
        stateService.init();

        assertTrue("Should be open at 12:00 on Monday", stateService.isOpen());
    }

    @Test
    public void testStartupBeforeOpenHours() {
        SettingsService settingsService = createMockSettingsService("09:00 - 22:00");
        Clock clock = Clock.fixed(Instant.parse("2024-01-01T08:00:00Z"), zone);

        MockTicketService ticketService = new MockTicketService();
        RestaurantStateService stateService = new RestaurantStateService(settingsService, ticketService, clock);
        stateService.init();

        assertFalse("Should be closed at 08:00 on Monday", stateService.isOpen());
    }

    @Test
    public void testStartupAfterCloseHours() {
        SettingsService settingsService = createMockSettingsService("09:00 - 22:00");
        Clock clock = Clock.fixed(Instant.parse("2024-01-01T23:00:00Z"), zone);

        MockTicketService ticketService = new MockTicketService();
        RestaurantStateService stateService = new RestaurantStateService(settingsService, ticketService, clock);
        stateService.init();

        assertFalse("Should be closed at 23:00 on Monday", stateService.isOpen());
    }

    @Test
    public void testStartupMissingSettings() {
        SettingsService settingsService = new SettingsService(new SettingsRepository() {
            public Settings getSettings() {
                return new Settings(0.0, new HashMap<>());
            }

            public void saveSettings(Settings s) {
            }
        });

        Clock clock = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), zone);

        MockTicketService ticketService = new MockTicketService();
        RestaurantStateService stateService = new RestaurantStateService(settingsService, ticketService, clock);
        stateService.init();

        assertFalse("Should be closed if settings are missing", stateService.isOpen());
    }

    private SettingsService createMockSettingsService(String mondayHours) {
        return new SettingsService(new SettingsRepository() {
            public Settings getSettings() {
                Map<String, String> hours = new HashMap<>();
                hours.put("mon", mondayHours);
                return new Settings(0.0, hours);
            }

            public void saveSettings(Settings s) {
            }
        });
    }

    private static class MockTicketService extends TicketService {
        public MockTicketService() {
            super(new com.ticketer.repositories.TicketRepository() {
                public com.ticketer.models.Ticket save(com.ticketer.models.Ticket t) {
                    return t;
                }

                public boolean deleteById(int id) {
                    return true;
                }

                public java.util.Optional<com.ticketer.models.Ticket> findById(int id) {
                    return java.util.Optional.empty();
                }

                public java.util.List<com.ticketer.models.Ticket> findAllActive() {
                    return new java.util.ArrayList<>();
                }

                public java.util.List<com.ticketer.models.Ticket> findAllCompleted() {
                    return new java.util.ArrayList<>();
                }

                public java.util.List<com.ticketer.models.Ticket> findAllClosed() {
                    return new java.util.ArrayList<>();
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
