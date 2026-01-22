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

    @Before
    public void setUp() {
        File ticketsDir = new File(TEST_TICKETS_DIR);
        if (!ticketsDir.exists()) {
            ticketsDir.mkdirs();
        }
        System.setProperty("tickets.dir", TEST_TICKETS_DIR);
        repository = new FileTicketRepository();
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

        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Should complete without exceptions", 0, exceptions.get());
        assertEquals(threadCount * ticketsPerThread, repository.findAllActive().size());
        executor.shutdown();
    }
}
