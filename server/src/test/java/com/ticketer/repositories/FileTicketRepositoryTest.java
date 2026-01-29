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
}
