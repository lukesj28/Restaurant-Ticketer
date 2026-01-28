package com.ticketer.repositories;

import com.ticketer.models.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class FileSettingsRepositoryTest {

    private static final String TEST_FILE = "target/repo-test-settings.json";
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @Before
    public void setUp() {
        new File(TEST_FILE).delete();
        mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
    }

    @After
    public void tearDown() {
        new File(TEST_FILE).delete();
    }

    @Test
    public void testGetSettingsFileNotFound() {
        FileSettingsRepository settingsRepo = new FileSettingsRepository(TEST_FILE, mapper);
        Settings settings = settingsRepo.getSettings();
        assertNotNull(settings);
        assertEquals(0.0, settings.getTax(), 0.001);
        assertNotNull(settings.getHours());
        assertTrue(settings.getHours().isEmpty());
    }

    @Test
    public void testGetSettingsCorruptedJson() throws IOException {
        Files.write(new File(TEST_FILE).toPath(), "{ invalid json ".getBytes());
        FileSettingsRepository repo = new FileSettingsRepository(TEST_FILE, mapper);
        try {
            repo.getSettings();
            fail("Should throw exception for corrupted JSON");
        } catch (RuntimeException e) {

        }
    }

    @Test
    public void testSaveAndLoad() {
        FileSettingsRepository repo = new FileSettingsRepository(TEST_FILE, mapper);
        Settings settings = new Settings(0.15, new java.util.HashMap<>());
        repo.saveSettings(settings);

        File f = new File(TEST_FILE);
        assertTrue(f.exists());

        Settings loaded = repo.getSettings();
        assertNotNull(loaded);
        assertEquals(0.15, loaded.getTax(), 0.001);
    }
}
