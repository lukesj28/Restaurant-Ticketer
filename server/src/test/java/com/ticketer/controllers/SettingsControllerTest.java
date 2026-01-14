package com.ticketer.controllers;

import com.ticketer.models.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import static org.junit.Assert.*;

public class SettingsControllerTest {

    private static final String ORIGINAL_SETTINGS_PATH = "data/settings.json";
    private static final String TEST_SETTINGS_PATH = "target/test-settings-controller.json";

    @Before
    public void setUp() throws IOException {
        File dataDir = new File("target");
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }

        File original = new File(ORIGINAL_SETTINGS_PATH);
        if (original.exists()) {
            Files.copy(original.toPath(), new File(TEST_SETTINGS_PATH).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        System.setProperty("settings.file", TEST_SETTINGS_PATH);
    }

    @After
    public void tearDown() {
        try {
            Files.deleteIfExists(new File(TEST_SETTINGS_PATH).toPath());
        } catch (IOException e) {
        }
        System.clearProperty("settings.file");
    }

    @Test
    public void testSettingsControllerInitialization() throws IOException {
        SettingsController controller = new SettingsController();
        Settings settings = controller.getSettings();
        assertNotNull("Settings should be loaded on initialization", settings);
    }

    @Test
    public void testGetIndividualSettings() throws IOException {
        SettingsController controller = new SettingsController();

        double tax = controller.getTax();
        assertTrue("Tax should be non-negative", tax >= 0);

        java.util.Map<String, String> hours = controller.getOpeningHours();
        assertNotNull("Opening hours map should not be null", hours);

        String monHours = controller.getOpeningHours("mon");
        assertNotNull("Monday hours should not be null", monHours);
    }

    @Test
    public void testRefreshSettings() throws IOException {
        SettingsController controller = new SettingsController();
        controller.refreshSettings();
        assertNotNull(controller.getSettings());
    }

    @Test
    public void testSetTax() throws IOException {
        SettingsController controller = new SettingsController();
        double newTax = 0.99;

        controller.setTax(newTax);
        assertEquals(newTax, controller.getTax(), 0.001);
    }

    @Test
    public void testSetOpeningHours() throws IOException {
        SettingsController controller = new SettingsController();
        String day = "mon";
        String newHours = "00:00-23:59";

        controller.setOpeningHours(day, newHours);
        assertEquals(newHours, controller.getOpeningHours(day));
    }

    @Test
    public void testGetOpenAndCloseTime() throws IOException {
        SettingsController controller = new SettingsController();
        String day = "wed";
        String testHours = "09:00 - 17:00";

        controller.setOpeningHours(day, testHours);

        assertEquals("09:00", controller.getOpenTime(day));
        assertEquals("17:00", controller.getCloseTime(day));

        controller.setOpeningHours(day, "closed");
        assertNull(controller.getOpenTime(day));
        assertNull(controller.getCloseTime(day));
    }

    @Test
    public void testGetOpenTimeValid() throws IOException {
        SettingsController controller = new SettingsController();
        String mon = controller.getOpenTime("mon");
        if (mon != null) {
            assertTrue("Open time should match HH:MM format", mon.matches("^\\d{2}:\\d{2}$"));
        }

        String close = controller.getCloseTime("mon");
        if (close != null) {
            assertTrue("Close time should match HH:MM format", close.matches("^\\d{2}:\\d{2}$"));
        }
    }
}
