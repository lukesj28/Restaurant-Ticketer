package com.ticketer.repositories;

import com.ticketer.models.Ticket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class FileTicketRepositoryTest {

    private FileTicketRepository repository;
    private static final String TEST_TICKETS_DIR = "target/test-tickets-repo";
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        File ticketsDir = new File(TEST_TICKETS_DIR);
        if (!ticketsDir.exists()) {
            ticketsDir.mkdirs();
        }
        System.setProperty("tickets.dir", TEST_TICKETS_DIR);
        System.setProperty("recovery.file", TEST_TICKETS_DIR + "/recovery.json");

        mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

        repository = new FileTicketRepository(mapper);
    }

    @AfterEach
    public void tearDown() {
        repository.deleteAll();
        File testDir = new File(TEST_TICKETS_DIR);
        if (testDir.exists()) {
            File[] files = testDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            testDir.delete();
        }
        System.clearProperty("tickets.dir");
        System.clearProperty("recovery.file");
    }

    @Test
    public void testSaveAndFind() {
        Ticket t = new Ticket(1);
        t.setTableNumber("T1");
        repository.save(t);

        assertTrue(repository.findById(1).isPresent());
        assertEquals("T1", repository.findById(1).get().getTableNumber());
    }

    @Test
    public void testConcurrency() throws InterruptedException {
        int threadCount = 20;
        int ticketsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger exceptions = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ticketsPerThread; j++) {
                        int id = (threadNum * ticketsPerThread) + j;
                        Ticket t = new Ticket(id);
                        repository.save(t);
                    }
                    for (Ticket t : repository.findAllActive()) {
                        t.getId();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    exceptions.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        assertEquals(0, exceptions.get(), "Should complete without exceptions");
        assertEquals(threadCount * ticketsPerThread, repository.findAllActive().size());
        executor.shutdown();
    }

    @Test
    public void testRecovery() throws IOException {
        Ticket t = new Ticket(99);
        t.setTableNumber("T99");
        repository.save(t);

        FileTicketRepository newRepo = new FileTicketRepository(mapper);
        java.util.List<Ticket> active = newRepo.findAllActive();
        assertEquals(1, active.size());
        Ticket recovered = active.get(0);
        assertEquals(99, recovered.getId());
        assertEquals("T99", recovered.getTableNumber());
    }

    @Test
    public void testTicketSerializationUsesUTC() throws IOException {
        Ticket t = new Ticket(55);
        t.setCreatedAt(java.time.Instant.parse("2023-01-01T12:00:00Z"));
        repository.save(t);

        File recoveryFile = new File(TEST_TICKETS_DIR + "/recovery.json");
        assertTrue(recoveryFile.exists());

        String content = new String(Files.readAllBytes(recoveryFile.toPath()));
        assertTrue(content.contains("2023-01-01T12:00:00Z"),
                "JSON should contain UTC timestamp (ending in Z). Actual: " + content);
    }

    @Test
    public void testPersistClosedTicketsAppendsToFile() throws IOException {
        String today = java.time.LocalDate.now(java.time.ZoneId.systemDefault()).toString();
        File dailyFile = new File(TEST_TICKETS_DIR + "/" + today + ".json");
        if (dailyFile.exists()) {
            dailyFile.delete();
        }

        java.util.List<Ticket> initialTickets = new java.util.ArrayList<>();
        initialTickets.add(new Ticket(1));
        com.ticketer.models.DailyTicketLog log = new com.ticketer.models.DailyTicketLog(new java.util.HashMap<>(),
                initialTickets, 0.0, 0.0);

        try (java.io.FileWriter writer = new java.io.FileWriter(dailyFile)) {
            mapper.writeValue(writer, log);
        }

        Ticket t = new Ticket(2);
        repository.save(t);
        repository.moveToClosed(2);
        repository.persistClosedTickets();

        com.ticketer.models.DailyTicketLog resultLog = mapper.readValue(dailyFile,
                com.ticketer.models.DailyTicketLog.class);
        assertEquals(2, resultLog.getTickets().size(), "Should contain 2 tickets");
        assertEquals(1, resultLog.getTickets().get(0).getId());
        assertEquals(2, resultLog.getTickets().get(1).getId());
    }

    @Test
    public void testDailyTallyCalculation() throws IOException {
        String today = java.time.LocalDate.now(java.time.ZoneId.systemDefault()).toString();
        File dailyFile = new File(TEST_TICKETS_DIR + "/" + today + ".json");
        if (dailyFile.exists()) {
            dailyFile.delete();
        }

        Ticket t1 = new Ticket(1);
        com.ticketer.models.Order o1 = new com.ticketer.models.Order();
        o1.addItem(new com.ticketer.models.OrderItem("Burger", "Fries", 1000));
        o1.addItem(new com.ticketer.models.OrderItem("Soda", null, 200));
        t1.addOrder(o1);

        Ticket t2 = new Ticket(2);
        com.ticketer.models.Order o2 = new com.ticketer.models.Order();
        o2.addItem(new com.ticketer.models.OrderItem("Burger", null, 1000));
        o2.addItem(new com.ticketer.models.OrderItem("Fries", "Ranch", 500));
        t2.addOrder(o2);

        repository.save(t1);
        repository.save(t2);
        repository.moveToClosed(1);
        repository.moveToClosed(2);

        repository.persistClosedTickets();

        com.ticketer.models.DailyTicketLog resultLog = mapper.readValue(dailyFile,
                com.ticketer.models.DailyTicketLog.class);
        java.util.Map<String, Integer> tally = resultLog.getTally();

        assertEquals(2, tally.get("Burger"));
        assertEquals(2, tally.get("Fries"));
        assertEquals(1, tally.get("Soda"));
        assertEquals(1, tally.get("Ranch"));

        assertEquals(27.00, resultLog.getSubtotal(), 0.001);
        assertTrue(resultLog.getTotal() >= 27.00);
    }

    @Test
    public void testDeleteAll() throws IOException {
        Ticket t = new Ticket(1);
        repository.save(t);
        File recoveryFile = new File(System.getProperty("recovery.file"));
        assertTrue(recoveryFile.exists(), "Recovery file should exist");

        repository.deleteAll();

        assertFalse(recoveryFile.exists(), "Recovery file should be deleted");
        assertTrue(repository.findAllActive().isEmpty());
    }
}
