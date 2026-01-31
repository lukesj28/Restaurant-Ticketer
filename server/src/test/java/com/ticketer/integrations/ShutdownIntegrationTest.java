package com.ticketer.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketer.models.Ticket;
import com.ticketer.services.RestaurantStateService;
import com.ticketer.services.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.nio.file.Path;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "tickets.dir=target/test-tickets/shutdown",
        "recovery.file=target/test-tickets/shutdown/recovery.json",
        "spring.main.allow-bean-definition-overriding=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ShutdownIntegrationTest {

    @Autowired
    private RestaurantStateService restaurantStateService;

    @Autowired
    private TicketService ticketService;

    private static MutableClock clock = new MutableClock(Instant.parse("2023-01-01T20:00:00Z"), ZoneId.of("UTC"));

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public Clock clock() {
            return clock;
        }
    }

    @BeforeEach
    public void setup() throws IOException {
        File shutdownDir = new File("target/test-tickets/shutdown");
        if (shutdownDir.exists()) {
            java.nio.file.Files.walk(shutdownDir.toPath())
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        ticketService.clearAllTickets();
        clock.setInstant(Instant.parse("2023-01-01T20:00:00Z"));
        restaurantStateService.forceOpen();
    }

    @Test
    public void testCompleteShutdownFlow() throws IOException {
        Ticket t1 = ticketService.createTicket("Table1");
        Ticket t2 = ticketService.createTicket("Table2");
        ticketService.moveToCompleted(t2.getId());

        assertTrue(new File("target/test-tickets/shutdown/recovery.json").exists(), "Recovery file should exist");

        restaurantStateService.forceClose();

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            return ticketService.getActiveTickets().isEmpty() &&
                    ticketService.getCompletedTickets().isEmpty() &&
                    ticketService.getClosedTickets().isEmpty();
        });

        assertFalse(new File("target/test-tickets/shutdown/recovery.json").exists(), "Recovery file should be deleted");

        File dailyLog = new File("target/test-tickets/shutdown/2023-01-01.json");
        assertTrue(dailyLog.exists(), "Daily log file should exist");

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        com.ticketer.models.DailyTicketLog log = mapper.readValue(dailyLog, com.ticketer.models.DailyTicketLog.class);
        assertEquals(1, log.getTickets().size());
        assertEquals(t2.getId(), log.getTickets().get(0).getId());
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
