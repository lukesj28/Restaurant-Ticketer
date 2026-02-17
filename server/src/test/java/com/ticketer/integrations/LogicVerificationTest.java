package com.ticketer.integrations;

import com.ticketer.models.MenuItem;
import com.ticketer.models.OrderItem;
import com.ticketer.models.Ticket;
import com.ticketer.models.Order;
import com.ticketer.services.MenuService;
import com.ticketer.repositories.FileMenuRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LogicVerificationTest {

    private MenuService service;
    private ObjectMapper mapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        String json = "{ \"categories\": { " +
            "\"lunch\": { " +
                "\"Burger\": { \"price\": 1000, \"available\": true, \"kitchen\": true, \"sides\": {} } " +
            "}, " +
            "\"dinner\": { " +
                "\"Burger\": { \"price\": 1500, \"available\": true, \"kitchen\": true, \"sides\": {} } " +
            "} " +
        "} }";
        
        File menuFile = tempDir.resolve("logic-verif-menu.json").toFile();
        Files.write(menuFile.toPath(), json.getBytes());

        mapper = new ObjectMapper();
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
        service = new MenuService(new FileMenuRepository(menuFile.getAbsolutePath(), mapper));
    }

    @Test
    public void testAmbiguityResolution() {
        OrderItem lunchItem = service.createOrderItem("Lunch", "Burger", null, null);
        assertEquals("Lunch", lunchItem.getCategory());
        assertEquals(1000, lunchItem.getPrice());
        OrderItem dinnerItem = service.createOrderItem("Dinner", "Burger", null, null);
        assertEquals("Dinner", dinnerItem.getCategory());
        assertEquals(1500, dinnerItem.getPrice());
        
        assertEquals(1000, lunchItem.getPrice());
        assertEquals(1500, dinnerItem.getPrice());
    }

    @Test
    public void testCategoryPrivacy() throws Exception {
        OrderItem item = new OrderItem("SecretCategory", "Burger", null, null, 1000, 0, 0, null);
        String json = mapper.writeValueAsString(item);
        
        assertFalse(json.contains("SecretCategory"), "JSON should not contain category");
        assertFalse(json.contains("\"category\""), "JSON should not contain category field");
        
        assertTrue(json.contains("Burger"));
    }
}
