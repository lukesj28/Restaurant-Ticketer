package com.ticketer.integrations;

import com.ticketer.models.Ticket;
import com.ticketer.repositories.FileTicketRepository;
import com.ticketer.repositories.FileSettingsRepository;
import com.ticketer.services.TicketService;
import com.ticketer.services.RestaurantStateService;
import com.ticketer.services.SettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TurnoverIntegrationTest {

    private FileTicketRepository ticketRepository;
    private TicketService ticketService;
    private RestaurantStateService restaurantStateService;
    private static final String TEST_DIR = "target/test-turnover-integration";
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        File testDir = new File(TEST_DIR);
        if (!testDir.exists()) {
            testDir.mkdirs();
        }
        System.setProperty("tickets.dir", TEST_DIR);
        System.setProperty("recovery.file", TEST_DIR + "/recovery.json");
        System.setProperty("settings.file", TEST_DIR + "/settings.json");

        mapper = new com.ticketer.config.JacksonConfig().objectMapper();
        ticketRepository = new FileTicketRepository(mapper);
        Clock clock = Clock.fixed(Instant.parse("2023-01-01T12:00:00Z"), ZoneId.of("UTC"));

        SettingsService settingsService = new SettingsService(new FileSettingsRepository(mapper));

        ticketService = new TicketService(ticketRepository, clock);
        restaurantStateService = new RestaurantStateService(settingsService, ticketService);
    }

    @AfterEach
    public void tearDown() {
        ticketRepository.deleteAll();
        deleteDirectory(new File(TEST_DIR));
        System.clearProperty("tickets.dir");
        System.clearProperty("recovery.file");
        System.clearProperty("settings.file");
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDirectory(f);
                    } else {
                        f.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    @Test
    public void testManualCloseSetsTimestamp() {
        Ticket t = ticketService.createTicket("T1");
        ticketService.moveToClosed(t.getId());

        Ticket closed = ticketRepository.findAllClosed().get(0);
        assertNotNull(closed.getClosedAt(), "Manual close SHOULD set timestamp");
    }

    @Test
    public void testShutdownLeavesTimestampNull() throws IOException {
        Ticket t = ticketService.createTicket("T2");

        ticketService.moveToCompleted(t.getId());

        assertEquals(0, ticketRepository.findAllActive().size());
        assertEquals(1, ticketRepository.findAllCompleted().size());

        restaurantStateService.forceClose();

        long start = System.currentTimeMillis();
        while (!ticketRepository.findAllCompleted().isEmpty() && System.currentTimeMillis() - start < 5000) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        assertEquals(0, ticketRepository.findAllCompleted().size());

        String today = java.time.LocalDate.now(ZoneId.systemDefault()).toString();
        File dailyFile = new File(TEST_DIR + "/" + today + ".json");
        assertTrue(dailyFile.exists(), "Daily log file should exist: " + dailyFile.getAbsolutePath());

        com.ticketer.models.DailyTicketLog log = mapper.readValue(dailyFile, com.ticketer.models.DailyTicketLog.class);
        assertFalse(log.getTickets().isEmpty(), "Log should contain tickets");
        Ticket closed = log.getTickets().get(log.getTickets().size() - 1);

        assertNull(closed.getClosedAt(), "Shutdown close SHOULD NOT set timestamp (it should be null)");
    }
}
