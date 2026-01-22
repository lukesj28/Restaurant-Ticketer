package com.ticketer.services;

import com.ticketer.repositories.SettingsRepository;
import com.ticketer.repositories.TicketRepository;
import com.ticketer.models.Settings;
import com.ticketer.models.Ticket;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class RestaurantStateServiceTest {

    private MockSettingsService settingsService;
    private MockTicketService ticketService;
    private RestaurantStateService restaurantStateService;

    @Before
    public void setUp() {
        settingsService = new MockSettingsService();
        ticketService = new MockTicketService();
        restaurantStateService = new RestaurantStateService(settingsService, ticketService);
    }

    @Test
    public void testClosingSequenceInterruptionCleanUp() throws InterruptedException {
        // Arrange
        // Simulate active tickets existing
        ticketService.allClosed = false;

        Thread thread = new Thread(() -> {
            try {
                java.lang.reflect.Method method = RestaurantStateService.class.getDeclaredMethod("runClosingSequence");
                method.setAccessible(true);
                method.invoke(restaurantStateService);
            } catch (Exception e) {
                // Unexpected, but let's print
                e.printStackTrace();
            }
        });

        thread.start();

        // Give it a moment to enter the loop
        Thread.sleep(200);

        // Act: Interrupt
        thread.interrupt();
        thread.join(1000);

        // Assert
        assertTrue("moveAllToClosed should be called", ticketService.moveAllToClosedCalled);
        assertTrue("serializeClosedTickets should be called", ticketService.serializeClosedTicketsCalled);
        assertTrue("clearAllTickets should be called", ticketService.clearAllTicketsCalled);
    }

    // Manual Mocks

    private static class MockSettingsService extends SettingsService {
        public MockSettingsService() {
            super(new SettingsRepository() {
                public Settings getSettings() {
                    return null;
                }

                public void saveSettings(Settings s) {
                }
            });
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
            });
        }

        @Override
        public boolean areAllTicketsClosed() {
            return allClosed;
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
