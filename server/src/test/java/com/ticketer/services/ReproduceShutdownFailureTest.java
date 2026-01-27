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

public class ReproduceShutdownFailureTest {

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
        restaurantStateService.init();
    }

    @Test
    public void testShutdownClosesRestaurantDuringOpenHours() {
        settingsService.setHours("09:00 - 17:00");
        initService();

        assertTrue("Restaurant should be open initially", restaurantStateService.isOpen());

        restaurantStateService.forceClose();

        assertFalse("Restaurant should be closed after shutdown() call", restaurantStateService.isOpen());
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
            return true;
        }

        @Override
        public boolean hasActiveTickets() {
            return false;
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
