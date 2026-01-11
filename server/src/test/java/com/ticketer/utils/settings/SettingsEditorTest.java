package com.ticketer.utils.settings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;

public class SettingsEditorTest {

    private static final String TEST_SETTINGS_PATH = "target/test_settings_editor.json";

    @Before
    public void setUp() {
        File dataDir = new File("target");
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }
        System.setProperty("settings.file", TEST_SETTINGS_PATH);

        try (java.io.FileWriter writer = new java.io.FileWriter(TEST_SETTINGS_PATH)) {
            writer.write("{}");
        } catch (IOException e) {
            fail("Could not create test settings file");
        }
    }

    @After
    public void tearDown() {
        new File(TEST_SETTINGS_PATH).delete();
        System.clearProperty("settings.file");
    }

    @Test
    public void testSetTax() throws IOException {
        // Initial set
        SettingsEditor.setTax(0.20);
        assertEquals(0.20, SettingsReader.getTax(), 0.001);

        // Update
        SettingsEditor.setTax(0.25);
        assertEquals(0.25, SettingsReader.getTax(), 0.001);
    }

    @Test
    public void testSetOpeningHours() throws IOException {
        SettingsEditor.setOpeningHours("mon", "09:00 - 17:00");

        assertEquals("09:00 - 17:00", SettingsReader.getOpeningHours("mon"));

        // Ensure strictly lower case handling in storage if desired,
        // or at least that reading it back works (Reader lowercases input)
        SettingsEditor.setOpeningHours("Fri", "10:00 - 20:00");
        assertEquals("10:00 - 20:00", SettingsReader.getOpeningHours("fri"));
    }

    @Test
    public void testConstructor() {
        new SettingsEditor();
    }
}
