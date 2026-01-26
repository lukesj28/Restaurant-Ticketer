package com.ticketer.repositories;

import com.ticketer.models.Ticket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceTest {

    private FileTicketRepository repository;
    private final String TEST_TICKETS_DIR = "target/test-data/tickets";
    private final String TEST_RECOVERY_FILE = "target/test-data/recovery.json";

    @BeforeEach
    void setUp() throws IOException {
        deleteDir(new File("target/test-data"));
        new File(TEST_TICKETS_DIR).mkdirs();

        System.setProperty("tickets.dir", TEST_TICKETS_DIR);
        System.setProperty("recovery.file", TEST_RECOVERY_FILE);

        repository = new FileTicketRepository();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("tickets.dir");
        System.clearProperty("recovery.file");
    }

    @Test
    void testCrashRecovery() {
        Ticket t1 = new Ticket(1);
        t1.setTableNumber("Table 1");
        repository.save(t1);

        File recoveryFile = new File(TEST_RECOVERY_FILE);
        assertTrue(recoveryFile.exists(), "Recovery file should exist after saving a ticket");

        FileTicketRepository newRepository = new FileTicketRepository();

        Optional<Ticket> recovered = newRepository.findById(1);
        assertTrue(recovered.isPresent(), "Ticket should be recovered from file");
        assertEquals("Table 1", recovered.get().getTableNumber());
    }

    @Test
    void testClearOnShutdown() {
        Ticket t1 = new Ticket(1);
        repository.save(t1);
        assertTrue(new File(TEST_RECOVERY_FILE).exists());

        repository.clearRecoveryFile();

        assertFalse(new File(TEST_RECOVERY_FILE).exists(), "Recovery file should be deleted after explicit clear");
    }

    private void deleteDir(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files)
                    deleteDir(f);
            }
        }
        file.delete();
    }
}
