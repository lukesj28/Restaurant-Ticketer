package com.ticketer.repositories;

import com.ticketer.models.Ticket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class FileTicketRepositoryTest {

    private FileTicketRepository repository;
    private static final String TEST_TICKETS_DIR = "target/test-tickets-repo";
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @Before
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

    @After
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
        assertEquals("Should complete without exceptions", 0, exceptions.get());
        assertEquals(threadCount * ticketsPerThread, repository.findAllActive().size());
        executor.shutdown();
    }

    @Test
    public void testLegacyRecovery() throws java.io.IOException {
        File recoveryFile = new File(TEST_TICKETS_DIR + "/recovery.json");
        recoveryFile.getParentFile().mkdirs();
        try (java.io.FileWriter writer = new java.io.FileWriter(recoveryFile)) {
            writer.write("{\n" +
                    "  \"active\": [\n" +
                    "    {\n" +
                    "      \"id\": 99,\n" +
                    "      \"tableNumber\": \"T99\",\n" +
                    "      \"orders\": [\n" +
                    "        {\n" +
                    "          \"items\": [\n" +
                    "            {\n" +
                    "              \"name\": \"Burger\",\n" +
                    "              \"price\": 10.99\n" +
                    "            }\n" +
                    "          ],\n" +
                    "          \"subtotal\": 10.99,\n" +
                    "          \"total\": 12.42\n" +
                    "        }\n" +
                    "      ],\n" +
                    "      \"subtotal\": 10.99,\n" +
                    "      \"total\": 12.42,\n" +
                    "      \"createdAt\": \"2023-01-01T12:00:00Z\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"completed\": []\n" +
                    "}");
        }

        repository = new FileTicketRepository(mapper);

        java.util.List<Ticket> active = repository.findAllActive();
        assertEquals(1, active.size());
        Ticket t = active.get(0);
        assertEquals(99, t.getId());
        assertEquals(1, t.getOrders().size());
        assertEquals(1099, t.getOrders().get(0).getItems().get(0).getPrice());
        assertEquals(1099, t.getOrders().get(0).getTotal());
    }
}
