package com.ticketer.controllers;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ticketer.services.RestaurantStateService;

public class SystemControllerTest {

    private MockRestaurantStateService restaurantStateService;
    private SystemController systemController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        restaurantStateService = new MockRestaurantStateService();
        systemController = new SystemController(restaurantStateService);
        mockMvc = MockMvcBuilders.standaloneSetup(systemController)
                .setControllerAdvice(new com.ticketer.exceptions.GlobalExceptionHandler())
                .build();
    }

    @Test
    public void testInitialization() {
        assertNotNull(systemController);
    }

    @Test
    public void testIsOpen() {
        systemController.isOpen();
    }

    @Test
    public void testShutdown() {
        systemController.shutdown();
    }

    private static class MockRestaurantStateService extends RestaurantStateService {
        public MockRestaurantStateService() {
            super(null, null);
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void shutdown() {
        }
    }
}
