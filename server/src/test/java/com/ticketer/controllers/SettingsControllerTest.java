package com.ticketer.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ticketer.exceptions.InvalidInputException;
import com.ticketer.exceptions.GlobalExceptionHandler;
import com.ticketer.models.Settings;
import com.ticketer.services.RestaurantStateService;
import com.ticketer.services.SettingsService;
import com.ticketer.components.SerialPortManager;

public class SettingsControllerTest {

        @Mock
        private SettingsService settingsService;

        @Mock
        private RestaurantStateService restaurantStateService;

        @Mock
        private SerialPortManager serialPortManager;

        @InjectMocks
        private SettingsController settingsController;

        private MockMvc mockMvc;

        @BeforeEach
        public void setUp() {
                MockitoAnnotations.openMocks(this);
                settingsController = new SettingsController(settingsService, restaurantStateService, serialPortManager);
                mockMvc = MockMvcBuilders.standaloneSetup(settingsController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        public void testInitialization() {
                assertNotNull(settingsController);
        }

        @Test
        public void testRefreshSettings() throws Exception {
                Map<String, String> hours = new HashMap<>();
                hours.put("monday", "09:00 - 22:00");
                Settings settings = new Settings(1000, hours, null, null, null);
                when(settingsService.getSettings()).thenReturn(settings);

                mockMvc.perform(get("/api/settings/refresh"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload").exists());

                verify(settingsService).getSettings();
        }

        @Test
        public void testGetTax() throws Exception {
                when(settingsService.getTax()).thenReturn(1000);

                mockMvc.perform(get("/api/settings/tax"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload").value(1000));

                verify(settingsService).getTax();
        }

        @Test
        public void testGetOpeningHours() throws Exception {
                Map<String, String> hours = new HashMap<>();
                hours.put("monday", "09:00 - 22:00");
                when(settingsService.getAllOpeningHours()).thenReturn(hours);

                mockMvc.perform(get("/api/settings/hours"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.monday").exists());

                verify(settingsService).getAllOpeningHours();
        }

        @Test
        public void testGetOpeningHoursForDay() throws Exception {
                when(settingsService.getOpeningHours("monday")).thenReturn("09:00-22:00");

                mockMvc.perform(get("/api/settings/hours/monday"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload").value("09:00-22:00"));

                verify(settingsService).getOpeningHours("monday");
        }

        @Test
        public void testGetOpenCloseTime() throws Exception {
                when(settingsService.getOpenTime("monday")).thenReturn("09:00");
                when(settingsService.getCloseTime("monday")).thenReturn("22:00");

                mockMvc.perform(get("/api/settings/hours/monday/open"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload").value("09:00"));

                verify(settingsService).getOpenTime("monday");

                mockMvc.perform(get("/api/settings/hours/monday/close"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload").value("22:00"));

                verify(settingsService).getCloseTime("monday");
        }

        @Test
        public void testSetTax() throws Exception {
                String json = "{\"tax\":1500}";
                mockMvc.perform(put("/api/settings/tax")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk());

                verify(settingsService).setTax(1500);
        }

        @Test
        public void testSetOpeningHours() throws Exception {
                String json = "{\"hours\":\"10:00 - 20:00\"}";
                mockMvc.perform(put("/api/settings/hours/Monday")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk());

                verify(settingsService).setOpeningHours("Monday", "10:00 - 20:00");
                verify(restaurantStateService).checkAndScheduleState();
        }

        @Test
        public void testSetTaxInvalid() throws Exception {
                doThrow(new InvalidInputException("Tax cannot be negative"))
                                .when(settingsService).setTax(anyInt());

                String json = "{\"tax\":-100}";
                mockMvc.perform(put("/api/settings/tax")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest());

                verify(settingsService).setTax(-100);
        }

        @Test
        public void testSetOpeningHoursInvalid() throws Exception {
                doThrow(new InvalidInputException("Invalid format"))
                                .when(settingsService).setOpeningHours(anyString(), anyString());

                String json = "{\"hours\":\"invalid-format\"}";
                mockMvc.perform(put("/api/settings/hours/Monday")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest());

                verify(settingsService).setOpeningHours("Monday", "invalid-format");
        }
}
