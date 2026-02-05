package com.ticketer.services;

import com.ticketer.models.Settings;
import com.ticketer.repositories.SettingsRepository;
import com.ticketer.exceptions.InvalidInputException;
import com.ticketer.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SettingsServiceTest {

    @Mock
    private SettingsRepository settingsRepository;

    @InjectMocks
    private SettingsService settingsService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetSettings() {
        Settings settings = new Settings(1000, new HashMap<>());
        when(settingsRepository.getSettings()).thenReturn(settings);

        settingsService = new SettingsService(settingsRepository);
        Settings result = settingsService.getSettings();

        assertNotNull(result);
        assertEquals(1000, result.getTax());
    }

    @Test
    public void testGetSettingsNotFound() {
        when(settingsRepository.getSettings()).thenReturn(null);
        settingsService = new SettingsService(settingsRepository);

        assertThrows(EntityNotFoundException.class, () -> settingsService.getSettings());
    }

    @Test
    public void testSetTax() {
        Settings settings = new Settings(1000, new HashMap<>());
        when(settingsRepository.getSettings()).thenReturn(settings);
        settingsService = new SettingsService(settingsRepository);

        settingsService.setTax(2000);

        ArgumentCaptor<Settings> captor = ArgumentCaptor.forClass(Settings.class);
        verify(settingsRepository).saveSettings(captor.capture());

        Settings saved = captor.getValue();
        assertEquals(2000, saved.getTax());
    }

    @Test
    public void testSetTaxInvalid() {
        assertThrows(InvalidInputException.class, () -> {
            settingsService.setTax(-1);
        });
    }

    @Test
    public void testSetOpeningHours() {
        Settings settings = new Settings(1000, new HashMap<>());
        when(settingsRepository.getSettings()).thenReturn(settings);
        settingsService = new SettingsService(settingsRepository);

        settingsService.setOpeningHours("monday", "09:00 - 17:00");

        ArgumentCaptor<Settings> captor = ArgumentCaptor.forClass(Settings.class);
        verify(settingsRepository).saveSettings(captor.capture());

        Settings saved = captor.getValue();
        assertEquals("09:00 - 17:00", saved.getHours().get("monday"));
    }

    @Test
    public void testGetOpenTime() {
        Map<String, String> hours = new HashMap<>();
        hours.put("monday", "09:00 - 17:00");
        Settings settings = new Settings(1000, hours);

        when(settingsRepository.getSettings()).thenReturn(settings);
        settingsService = new SettingsService(settingsRepository);

        String openTime = settingsService.getOpenTime("monday");
        assertEquals("09:00", openTime);
    }

    @Test
    public void testGetOpenTimeClosed() {
        Map<String, String> hours = new HashMap<>();
        hours.put("monday", "closed");
        Settings settings = new Settings(1000, hours);

        when(settingsRepository.getSettings()).thenReturn(settings);
        settingsService = new SettingsService(settingsRepository);

        String openTime = settingsService.getOpenTime("monday");
        assertNull(openTime);
    }
}
