package com.ticketer.integrations;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
public class SystemIntegrationTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setUp() {
        System.setProperty("tickets.dir", tempDir.resolve("tickets").toAbsolutePath().toString());
        System.setProperty("recovery.file", tempDir.resolve("recovery.json").toAbsolutePath().toString());
        System.setProperty("menu.file", tempDir.resolve("menu.json").toAbsolutePath().toString());
        System.setProperty("settings.file", tempDir.resolve("settings.json").toAbsolutePath().toString());
    }

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
