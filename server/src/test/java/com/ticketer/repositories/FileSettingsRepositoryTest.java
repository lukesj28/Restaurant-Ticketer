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

    @Before
    public void setUp() {
        new File(TEST_FILE).delete();
    }

    @After
    public void tearDown() {
        new File(TEST_FILE).delete();
    }

    @Test
    public void testGetSettingsFileNotFound() {
        new FileMenuRepository(TEST_FILE);

        FileSettingsRepository settingsRepo = new FileSettingsRepository(TEST_FILE);
        try {
            settingsRepo.getSettings();
            fail("Should throw exception if file missing");
        } catch (RuntimeException e) {

        }
    }

    @Test
    public void testGetSettingsCorruptedJson() throws IOException {
        Files.write(new File(TEST_FILE).toPath(), "{ invalid json ".getBytes());
        FileSettingsRepository repo = new FileSettingsRepository(TEST_FILE);
        try {
            repo.getSettings();
            fail("Should throw exception for corrupted JSON");
        } catch (com.google.gson.JsonSyntaxException e) {

        } catch (RuntimeException e) {

        }
    }

    @Test
    public void testSaveAndLoad() {
        FileSettingsRepository repo = new FileSettingsRepository(TEST_FILE);
        Settings settings = new Settings(0.15, new java.util.HashMap<>());
        repo.saveSettings(settings);

        File f = new File(TEST_FILE);
        assertTrue(f.exists());

        Settings loaded = repo.getSettings();
        assertNotNull(loaded);
        assertEquals(0.15, loaded.getTax(), 0.001);
    }
}
