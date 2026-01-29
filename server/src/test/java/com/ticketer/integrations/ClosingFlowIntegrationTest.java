package com.ticketer.integrations;

import com.ticketer.models.Ticket;
import com.ticketer.services.RestaurantStateService;
import com.ticketer.services.SettingsService;
import com.ticketer.services.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

import java.time.*;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "tickets.dir=target/test-tickets/closing",
        "recovery.file=target/test-tickets/closing/recovery.json"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ClosingFlowIntegrationTest {

    @Autowired
    private RestaurantStateService restaurantStateService;

    @Autowired
    private TicketService ticketService;

    private static MutableClock clock = new MutableClock(Instant.parse("2023-01-01T20:00:00Z"), ZoneId.of("UTC"));

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public RestaurantStateService restaurantStateService(SettingsService settings, TicketService ticketService) {
            return new RestaurantStateService(settings, ticketService, clock);
        }
    }

    @BeforeEach
    public void setup() {
        ticketService.clearAllTickets();
        clock.setInstant(Instant.parse("2023-01-01T20:00:00Z"));
    }

    @Test
    public void testClosingFlow() {
        restaurantStateService.forceOpen();
        assertTrue(restaurantStateService.isOpen(), "Restaurant should be forced open");

        Ticket t1 = ticketService.createTicket("Table1");
        Ticket t2 = ticketService.createTicket("Table2");
        ticketService.moveToCompleted(t2.getId());

        assertEquals(1, ticketService.getActiveTickets().size());
        assertEquals(1, ticketService.getCompletedTickets().size());

        restaurantStateService.forceClose();

        ticketService.moveToClosed(t1.getId());
        ticketService.moveToClosed(t2.getId());

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            return ticketService.getClosedTickets().isEmpty();
        });

        assertEquals(0, ticketService.getActiveTickets().size());
    }

    private static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        public MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }

    }
}
