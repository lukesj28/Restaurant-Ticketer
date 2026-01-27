package com.ticketer.controllers;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import com.ticketer.api.ApiResponse;
import com.ticketer.dtos.*;
import com.ticketer.exceptions.TicketerException;
import com.ticketer.models.Settings;
import com.ticketer.repositories.SettingsRepository;
import com.ticketer.services.RestaurantStateService;
import com.ticketer.services.SettingsService;

public class SettingsControllerTest {

    private MockSettingsService settingsService;
    private MockRestaurantStateService restaurantStateService;
    private SettingsController settingsController;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        settingsService = new MockSettingsService();
        restaurantStateService = new MockRestaurantStateService();
        settingsController = new SettingsController(settingsService, restaurantStateService);
        mockMvc = MockMvcBuilders.standaloneSetup(settingsController)
                .setControllerAdvice(new com.ticketer.exceptions.GlobalExceptionHandler())
                .build();
    }

    @Test
    public void testInitialization() {
        assertNotNull(settingsController);
    }

    @Test
    public void testRefreshSettings() {
        ApiResponse<Settings> response = settingsController.refreshSettings();
        assertNotNull(response.payload());
    }

    @Test
    public void testGetTax() {
        ApiResponse<Double> response = settingsController.getTax();
        assertEquals(0.1, response.payload(), 0.001);
    }

    @Test
    public void testGetOpeningHours() {
        ApiResponse<Map<String, String>> response = settingsController.getOpeningHours();
        assertTrue(response.payload().containsKey("monday"));
    }

    @Test
    public void testGetOpeningHoursForDay() {
        ApiResponse<String> response = settingsController.getOpeningHours("monday");
        assertEquals("09:00-22:00", response.payload());
    }

    @Test
    public void testGetOpenCloseTime() {
        assertEquals("09:00", settingsController.getOpenTime("monday").payload());
        assertEquals("22:00", settingsController.getCloseTime("monday").payload());
    }

    @Test
    public void testSetTax() {
        settingsController.setTax(new Requests.TaxUpdateRequest(0.15));
        assertTrue(settingsService.setTaxCalled);
    }

    @Test
    public void testSetOpeningHours() {
        settingsController.setOpeningHours("Monday", new Requests.OpeningHoursUpdateRequest("10:00 - 20:00"));
        assertTrue(restaurantStateService.checkAndScheduleStateCalled);
    }

    private static class FakeSettingsRepository implements SettingsRepository {
        @Override
        public Settings getSettings() {
            return new Settings(0.1, new HashMap<>());
        }

        @Override
        public void saveSettings(Settings settings) {
        }
    }

    private static class MockSettingsService extends SettingsService {
        boolean setTaxCalled = false;
        boolean throwGenericException = false;
        boolean throwTicketerException = false;

        public MockSettingsService() {
            super(new FakeSettingsRepository());
        }

        private void checkExceptions() {
            if (throwTicketerException)
                throw new TicketerException("Ticketer Error", 400);
            if (throwGenericException)
                throw new RuntimeException("Generic Error");
        }

        @Override
        public String getOpenTime(String day) {
            checkExceptions();
            return "09:00";
        }

        @Override
        public String getCloseTime(String day) {
            checkExceptions();
            return "22:00";
        }

        @Override
        public Settings getSettings() {
            checkExceptions();
            Map<String, String> hours = new HashMap<>();
            hours.put("monday", "09:00 - 22:00");
            return new Settings(0.1, hours);
        }

        @Override
        public double getTax() {
            checkExceptions();
            return 0.1;
        }

        @Override
        public void setTax(double tax) {
            checkExceptions();
            setTaxCalled = true;
        }

        @Override
        public Map<String, String> getAllOpeningHours() {
            checkExceptions();
            Map<String, String> hours = new HashMap<>();
            hours.put("monday", "09:00 - 22:00");
            return hours;
        }

        @Override
        public String getOpeningHours(String day) {
            checkExceptions();
            return "09:00-22:00";
        }

        @Override
        public void setOpeningHours(String day, String range) {
            checkExceptions();
        }
    }

    private static class MockRestaurantStateService extends RestaurantStateService {
        boolean checkAndScheduleStateCalled = false;

        public MockRestaurantStateService() {
            super(null, null);
        }

        @Override
        public void checkAndScheduleState() {
            checkAndScheduleStateCalled = true;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

    }
}
