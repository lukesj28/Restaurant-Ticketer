package com.ticketer.utils.settings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import static org.junit.Assert.*;

public class SettingsReaderTest {

    private static final String TEST_SETTINGS_PATH = "target/test_settings.json";

    @Before
    public void setUp() throws IOException {
        File dataDir = new File("target");
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }

        String jsonContent = "{\n" +
                "    \"tax\": 0.15,\n" +
                "    \"hours\": {\n" +
                "        \"mon\": \"closed\",\n" +
                "        \"tue\": \"09:00 - 17:00\",\n" +
                "        \"wed\": \"10:30 - 22:00\"\n" +
                "    }\n" +
                "}";

        try (FileWriter writer = new FileWriter(TEST_SETTINGS_PATH)) {
            writer.write(jsonContent);
        }

        System.setProperty("settings.file", TEST_SETTINGS_PATH);
    }

    @After
    public void tearDown() {
        new File(TEST_SETTINGS_PATH).delete();
        System.clearProperty("settings.file");
    }

    @Test
    public void testGetTax() throws IOException {
        assertEquals(0.15, SettingsReader.getTax(), 0.0001);
    }

    @Test
    public void testGetOpeningHours_SpecificDay() throws IOException {
        assertEquals("closed", SettingsReader.getOpeningHours("mon"));
        assertEquals("09:00 - 17:00", SettingsReader.getOpeningHours("tue"));
    }

    @Test
    public void testGetOpeningHours_MissingDay() throws IOException {
        assertEquals("closed", SettingsReader.getOpeningHours("sun"));
    }

    @Test
    public void testGetAllOpeningHours() throws IOException {
        Map<String, String> hours = SettingsReader.getOpeningHours();
        assertNotNull(hours);
        assertEquals("closed", hours.get("mon"));
        assertEquals("09:00 - 17:00", hours.get("tue"));
    }

    @Test
    public void testGetOpenTime() throws IOException {
        assertEquals("09:00", SettingsReader.getOpenTime("tue"));
        assertEquals("10:30", SettingsReader.getOpenTime("wed"));
        assertNull(SettingsReader.getOpenTime("mon"));
    }

    @Test
    public void testGetCloseTime() throws IOException {
        assertEquals("17:00", SettingsReader.getCloseTime("tue"));
        assertEquals("22:00", SettingsReader.getCloseTime("wed"));
        assertNull(SettingsReader.getCloseTime("mon"));
    }

    @Test
    public void testSettingsModelConstructor() {
        com.ticketer.models.Settings settings = new com.ticketer.models.Settings(0.1, null);
        assertEquals(0.1, settings.getTax(), 0.001);
        assertNull(settings.getHours());
    }

    @Test
    public void testSettingsReaderConstructor() {
        new SettingsReader();
    }

    @Test(expected = IOException.class)
    public void testMissingFileGetTax() throws IOException {
        new File(TEST_SETTINGS_PATH).delete();
        SettingsReader.getTax();
    }

    @Test(expected = IOException.class)
    public void testMissingFileGetHours() throws IOException {
        new File(TEST_SETTINGS_PATH).delete();
        SettingsReader.getOpeningHours("mon");
    }

    @Test(expected = IOException.class)
    public void testEmptyFileGetTax() throws IOException {
        try (FileWriter writer = new FileWriter(TEST_SETTINGS_PATH)) {
            writer.write("");
        }
        SettingsReader.getTax();
    }

    @Test
    public void testEmptyFileGetOpeningHours() throws IOException {
        try (FileWriter writer = new FileWriter(TEST_SETTINGS_PATH)) {
            writer.write("");
        }
        assertEquals("closed", SettingsReader.getOpeningHours("mon"));
        assertTrue(SettingsReader.getOpeningHours().isEmpty());
    }

    @Test
    public void testBrokenTimeFormat() throws IOException {
        String jsonContent = "{\n" +
                "    \"tax\": 0.15,\n" +
                "    \"hours\": {\n" +
                "        \"bad_format\": \"12:00\"\n" +
                "    }\n" +
                "}";
        try (FileWriter writer = new FileWriter(TEST_SETTINGS_PATH)) {
            writer.write(jsonContent);
        }

        assertEquals("12:00", SettingsReader.getOpenTime("bad_format"));
        assertNull(SettingsReader.getCloseTime("bad_format"));
    }
}
