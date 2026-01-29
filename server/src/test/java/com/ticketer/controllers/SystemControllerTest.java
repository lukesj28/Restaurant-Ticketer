package com.ticketer.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ticketer.services.RestaurantStateService;

public class SystemControllerTest {

    @Mock
    private RestaurantStateService restaurantStateService;

    @InjectMocks
    private SystemController systemController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(systemController)
                .setControllerAdvice(new com.ticketer.exceptions.GlobalExceptionHandler())
                .build();
    }

    @Test
    public void testInitialization() {
        assertNotNull(systemController);
    }

    @Test
    public void testIsOpen() throws Exception {
        when(restaurantStateService.isOpen()).thenReturn(true);

        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(restaurantStateService).isOpen();
    }

    @Test
    public void testForceClose() throws Exception {
        mockMvc.perform(post("/api/shutdown"))
                .andExpect(status().isOk());

        verify(restaurantStateService).forceClose();
    }

    @Test
    public void testForceOpen() throws Exception {
        mockMvc.perform(post("/api/open"))
                .andExpect(status().isOk());

        verify(restaurantStateService).forceOpen();
    }
}
