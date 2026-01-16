package com.ticketer.services;

import com.ticketer.models.Settings;
import com.ticketer.repositories.FileSettingsRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class SettingsServiceTest {

    private SettingsService service;
    private static final String TEST_SETTINGS_PATH = "target/test-settings-service.json";

    @Before
    public void setUp() {
        File dataDir = new File("target");
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }

        try {
            String json = "{ \"tax\": 0.1, \"hours\": { \"monday\": \"09:00 - 22:00\" } }";
            Files.write(new File(TEST_SETTINGS_PATH).toPath(), json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to set up test environment", e);
        }

        service = new SettingsService(new FileSettingsRepository(TEST_SETTINGS_PATH));
    }

    @After
    public void tearDown() {
        try {
            Files.deleteIfExists(new File(TEST_SETTINGS_PATH).toPath());
        } catch (IOException e) {
        }
    }

    @Test
    public void testInitialization() throws IOException {
        Settings settings = service.getSettings();
        assertNotNull("Settings should be loaded on initialization", settings);
    }

    @Test
    public void testGetIndividualSettings() throws IOException {
        double tax = service.getTax();
        assertTrue("Tax should be non-negative", tax >= 0);

        java.util.Map<String, String> hours = service.getAllOpeningHours();
        assertNotNull("Opening hours map should not be null", hours);

        String monHours = service.getOpeningHours("mon");
        assertNotNull("Monday hours should not be null", monHours);
    }

    @Test
    public void testSetTax() throws IOException {
        double newTax = 0.99;
        service.setTax(newTax);
        assertEquals(newTax, service.getTax(), 0.001);
    }

    @Test
    public void testSetOpeningHours() throws IOException {
        String day = "mon";
        String newHours = "00:00-23:59";

        service.setOpeningHours(day, newHours);
        assertEquals(newHours, service.getOpeningHours(day));
    }

    @Test
    public void testGetOpenAndCloseTime() throws IOException {
        String day = "wed";
        String testHours = "09:00 - 17:00";

        service.setOpeningHours(day, testHours);

        assertEquals("09:00", service.getOpenTime(day));
        assertEquals("17:00", service.getCloseTime(day));

        service.setOpeningHours(day, "closed");
        assertNull(service.getOpenTime(day));
        assertNull(service.getCloseTime(day));
    }

    @Test
    public void testGetOpenTimeValid() throws IOException {
        String mon = service.getOpenTime("mon");
        if (mon != null) {
            assertTrue("Open time should match HH:MM format", mon.matches("^\\d{2}:\\d{2}$"));
        }

        String close = service.getCloseTime("mon");
        if (close != null) {
            assertTrue("Close time should match HH:MM format", close.matches("^\\d{2}:\\d{2}$"));
        }
    }
}
