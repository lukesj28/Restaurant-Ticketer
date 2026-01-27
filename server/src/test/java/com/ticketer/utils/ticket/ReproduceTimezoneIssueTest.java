package com.ticketer.utils.ticket;

import com.ticketer.models.Ticket;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertTrue;

public class ReproduceTimezoneIssueTest {

    @Test
    public void testTicketSerializationUsesSystemTimezone() {
        Ticket ticket = new Ticket(1);
        Instant now = Instant.parse("2023-01-01T12:00:00Z");

        String json = TicketUtils.serializeTicket(ticket);

        boolean containsZ = json.contains("Z\"");

        if (!ZoneId.systemDefault().getId().equals("UTC")) {
            assertTrue("Should NOT contain Z (UTC marker) if we want local system time", !containsZ);
        }
    }
}
