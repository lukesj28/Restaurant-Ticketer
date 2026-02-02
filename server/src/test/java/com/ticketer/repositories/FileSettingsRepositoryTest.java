package com.ticketer.repositories;

import com.ticketer.models.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class FileSettingsRepositoryTest {

    private static final String TEST_FILE = "target/repo-test-settings.json";
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        new File(TEST_FILE).delete();
        mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
    }

    @AfterEach
    public void tearDown() {
        new File(TEST_FILE).delete();
    }

    @Test
    public void testGetSettingsFileNotFound() {
        FileSettingsRepository settingsRepo = new FileSettingsRepository(TEST_FILE, mapper);
        Settings settings = settingsRepo.getSettings();
        assertNotNull(settings);
        assertEquals(0, settings.getTax());
        assertNotNull(settings.getHours());
        assertTrue(settings.getHours().isEmpty());
    }

    @Test
    public void testGetSettingsCorruptedJson() throws IOException {
        Files.write(new File(TEST_FILE).toPath(), "{ invalid json ".getBytes());
        FileSettingsRepository repo = new FileSettingsRepository(TEST_FILE, mapper);
        assertThrows(RuntimeException.class, () -> repo.getSettings());
    }

    @Test
    public void testSaveAndLoad() {
        FileSettingsRepository repo = new FileSettingsRepository(TEST_FILE, mapper);
        Settings settings = new Settings(1500, new java.util.HashMap<>());
        repo.saveSettings(settings);

        File f = new File(TEST_FILE);
        assertTrue(f.exists());

        Settings loaded = repo.getSettings();
        assertNotNull(loaded);
        assertEquals(1500, loaded.getTax());
    }
}
