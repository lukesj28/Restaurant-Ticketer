package com.ticketer.integrations;

import com.ticketer.models.Ticket;
import com.ticketer.repositories.FileTicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceIntegrationTest {

    private FileTicketRepository repository;

    @TempDir
    Path tempDir;

    private String TEST_TICKETS_DIR;
    private String TEST_RECOVERY_FILE;
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @BeforeEach
    void setUp() throws IOException {
        TEST_TICKETS_DIR = tempDir.resolve("tickets").toAbsolutePath().toString();
        TEST_RECOVERY_FILE = tempDir.resolve("recovery.json").toAbsolutePath().toString();
        new File(TEST_TICKETS_DIR).mkdirs();

        System.setProperty("tickets.dir", TEST_TICKETS_DIR);
        System.setProperty("recovery.file", TEST_RECOVERY_FILE);

        mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

        repository = new FileTicketRepository(mapper);
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

        FileTicketRepository newRepository = new FileTicketRepository(mapper);

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
}
