package com.ticketer.integrations;

import com.ticketer.models.BaseItem;
import com.ticketer.models.OrderItem;
import com.ticketer.services.MenuService;
import com.ticketer.repositories.FileMenuRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LogicVerificationTest {

    private MenuService service;
    private ObjectMapper mapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        File menuFile = tempDir.resolve("logic-verif-menu.json").toFile();
        mapper = new com.ticketer.config.JacksonConfig().objectMapper();
        service = new MenuService(new FileMenuRepository(menuFile.getAbsolutePath(), mapper));
    }

    @Test
    public void testUuidBasedItemResolution() {
        BaseItem lunch = service.createBaseItem("Burger", 1000, true);
        service.addMenuItemToCategory("Lunch", lunch.getId(), Collections.emptyList());

        BaseItem dinner = service.createBaseItem("Burger", 1500, true);
        service.addMenuItemToCategory("Dinner", dinner.getId(), Collections.emptyList());

        OrderItem lunchItem = service.createItemOrderItem(lunch.getId(), null);
        assertEquals(1000, lunchItem.getPrice());
        assertEquals("Burger", lunchItem.getName());

        OrderItem dinnerItem = service.createItemOrderItem(dinner.getId(), null);
        assertEquals(1500, dinnerItem.getPrice());
        assertEquals("Burger", dinnerItem.getName());

        assertEquals(1000, lunchItem.getPrice());
        assertEquals(1500, dinnerItem.getPrice());
    }

    @Test
    public void testOrderItemJsonNoCategoryField() throws Exception {
        OrderItem item = OrderItem.forItem(UUID.randomUUID(), "Burger", null, null, 1000, 0);
        String json = mapper.writeValueAsString(item);

        assertFalse(json.contains("\"category\""), "JSON should not contain a category field");
        assertTrue(json.contains("Burger"), "JSON should contain item name");
        assertTrue(json.contains("mainPrice"), "JSON should contain mainPrice");
    }

    @Test
    public void testOrderItemWithSideJson() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID sideId = UUID.randomUUID();
        OrderItem item = OrderItem.forItem(itemId, "Fish", sideId, "Chips", 1200, 200);
        String json = mapper.writeValueAsString(item);

        assertTrue(json.contains("Fish"));
        assertTrue(json.contains("Chips"));
        assertFalse(json.contains("\"category\""));
    }
}
