package com.ticketer.utils.ticket;

import com.ticketer.models.Ticket;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import java.time.Instant;

public class ReproduceTimezoneIssueTest {

    @Test
    public void testTicketSerializationUsesUTC() throws Exception {
        Ticket ticket = new Ticket(1);
        Instant now = Instant.parse("2023-01-01T12:00:00Z");
        ticket.setCreatedAt(now);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

        String json = mapper.writeValueAsString(ticket);

        assertTrue("JSON should contain UTC timestamp (ending in Z)", json.contains("2023-01-01T12:00:00Z"));
    }
}
