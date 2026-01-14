package com.ticketer.controllers;

import com.ticketer.models.Settings;
import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;

public class SettingsControllerTest {

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
}
