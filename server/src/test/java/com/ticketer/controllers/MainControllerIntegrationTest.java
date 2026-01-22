package com.ticketer.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
public class MainControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetStatus() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetMenu() throws Exception {
        mockMvc.perform(get("/api/menu/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }

    @Test
    public void testCreateTicket() throws Exception {
        mockMvc.perform(post("/api/tickets")
                .param("tableNumber", "Table1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.payload.tableNumber", is("Table1")));
    }

    @Test
    public void testSettings() throws Exception {
        mockMvc.perform(get("/api/settings/tax"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }
}
