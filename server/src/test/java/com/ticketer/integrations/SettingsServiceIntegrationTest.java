package com.ticketer.integrations;

import com.ticketer.exceptions.InvalidInputException;
import com.ticketer.exceptions.EntityNotFoundException;

import com.ticketer.models.Settings;
import com.ticketer.repositories.FileSettingsRepository;
import com.ticketer.services.SettingsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsServiceIntegrationTest {

    private SettingsService service;

    @TempDir
    Path tempDir;

    private File testSettingsFile;
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        testSettingsFile = tempDir.resolve("test-settings-service.json").toFile();

        try {
            String json = "{ \"tax\": 1000, \"hours\": { \"monday\": \"09:00 - 22:00\" } }";
            Files.write(testSettingsFile.toPath(), json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to set up test environment", e);
        }

        mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

        service = new SettingsService(new FileSettingsRepository(testSettingsFile.getAbsolutePath(), mapper));
    }

    @Test
    public void testInitialization() throws IOException {
        Settings settings = service.getSettings();
        assertNotNull(settings, "Settings should be loaded on initialization");
    }

    @Test
    public void testGetIndividualSettings() throws IOException {
        int tax = service.getTax();
        assertTrue(tax >= 0, "Tax should be non-negative");

        java.util.Map<String, String> hours = service.getAllOpeningHours();
        assertNotNull(hours, "Opening hours map should not be null");

        String monHours = service.getOpeningHours("mon");
        assertNotNull(monHours, "Monday hours should not be null");
    }

    @Test
    public void testSetTax() throws IOException {
        int newTax = 9900;
        service.setTax(newTax);
        assertEquals(newTax, service.getTax());
    }

    @Test
    public void testSetOpeningHours() throws IOException {
        String day = "mon";
        String newHours = "00:00 - 23:59";

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
            assertTrue(mon.matches("^\\d{2}:\\d{2}$"), "Open time should match HH:MM format");
        }

        String close = service.getCloseTime("mon");
        if (close != null) {
            assertTrue(close.matches("^\\d{2}:\\d{2}$"), "Close time should match HH:MM format");
        }
    }

    public void testSetTaxInvalid() {
        assertThrows(InvalidInputException.class, () -> {
            service.setTax(-1);
        });
    }

    @Test
    public void testGetSettingsNotFound() {
        SettingsService errorService = new SettingsService(new com.ticketer.repositories.SettingsRepository() {
            public Settings getSettings() {
                return null;
            }

            public void saveSettings(Settings s) {
            }
        });
        assertThrows(EntityNotFoundException.class, () -> {
            errorService.getSettings();
        });
    }
}
