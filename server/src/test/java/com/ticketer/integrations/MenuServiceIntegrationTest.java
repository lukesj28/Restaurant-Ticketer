package com.ticketer.integrations;

import com.ticketer.models.MenuItem;
import com.ticketer.models.MenuItemView;
import com.ticketer.repositories.FileMenuRepository;
import com.ticketer.services.MenuService;
import com.ticketer.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MenuServiceIntegrationTest {

    private MenuService service;

    @TempDir
    Path tempDir;

    private File testMenuFile;
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        testMenuFile = tempDir.resolve("test-menu-service.json").toFile();

        try {
            String json = "{ \"TestCategory\": { \"TestItemAdd\": { \"price\": 1234, \"available\": true, \"sides\": {} } } }";
            Files.write(testMenuFile.toPath(), json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to set up test environment", e);
        }

        mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

        service = new MenuService(new FileMenuRepository(testMenuFile.getAbsolutePath(), mapper));
    }

    @Test
    public void testInitialization() {
        assertNotNull(service.getCategories(), "Categories should function");
    }

    @Test
    public void testGetItem() {
        List<MenuItemView> items = service.getAllItems();
        if (!items.isEmpty()) {
            MenuItemView item = items.get(0);
            MenuItem found = service.getItem(item.name);
            assertNotNull(found, "Should find existing item");
            assertEquals(item.name, found.name);
        }
    }

    @Test
    public void testRemoveItem() {
        if (service.getAllItems().isEmpty()) {
            service.addItem("TempCat", "ToRemove", 1000, null);
        }

        String nameToRemove = service.getAllItems().get(0).name;
        service.removeItem(nameToRemove);

        assertThrows(EntityNotFoundException.class, () -> {
            service.getItem(nameToRemove);
        });
    }

    @Test
    public void testRenameCategory() {
        String oldCat = "OldCat";
        service.addItem(oldCat, "ItemInCat", 1000, null);

        String newCat = "NewCat_Renamed";
        service.renameCategory(oldCat, newCat);

        assertThrows(EntityNotFoundException.class, () -> {
            service.getCategory(oldCat.toLowerCase());
        });
        assertNotNull(service.getCategory(newCat.toLowerCase()), "New category should exist");
    }

    @Test
    public void testGetItemObject() {
        if (service.getAllItems().isEmpty()) {
            service.addItem("Cat", "Item", 1000, null);
        }

        MenuItemView view = service.getAllItems().get(0);
        MenuItem menuItem = service.getItem(view.name);

        com.ticketer.models.OrderItem item = com.ticketer.models.Menu.getItem(menuItem, null);

        assertNotNull(item, "Should create OrderItem object");
        assertEquals(menuItem.name, item.getName());
    }

    @Test
    public void testGetItemWithSide() {
        String name = "ItemSideTest";
        Map<String, Integer> sides = new HashMap<>();
        sides.put("Fries", 200);
        service.addItem("SideCat", name, 1000, sides);

        MenuItem menuItem = service.getItem(name);
        com.ticketer.models.OrderItem item = com.ticketer.models.Menu.getItem(menuItem, "Fries");

        assertNotNull(item);
        assertEquals(name, item.getName());
        assertEquals(1000, item.getMainPrice());
        assertEquals(200, item.getSidePrice());
        assertEquals(1200, item.getPrice());
    }

    @Test
    public void testAddItem() {
        String cat = "TestCategory";
        String name = "TestItemAdd";
        int price = 1234;

        service.addItem(cat, name, price, null);

        MenuItem item = service.getItem(name);
        assertNotNull(item);
        assertEquals(name, item.name);
        assertEquals(price, item.price);

        List<MenuItem> catItems = service.getCategory(cat.toLowerCase());
        assertNotNull(catItems);
        assertTrue(catItems.stream().anyMatch(i -> i.name.equals(name)));
    }

    @Test
    public void testEditItemPrice() {
        String name = "PriceItem";
        service.addItem("Cat", name, 1000, null);

        int newPrice = 9999;
        service.editItemPrice(name, newPrice);

        assertEquals(newPrice, service.getItem(name).price);
    }

    @Test
    public void testEditItemAvailability() {
        String name = "AvailItem";
        service.addItem("Cat", name, 1000, null);

        service.editItemAvailability(name, false);
        assertFalse(service.getItem(name).available);

        service.editItemAvailability(name, true);
        assertTrue(service.getItem(name).available);
    }

    @Test
    public void testRenameItem() {
        String oldName = "OldNameItem";
        service.addItem("Cat", oldName, 1000, null);

        String newName = "RenamedItem_" + System.currentTimeMillis();
        service.renameItem(oldName, newName);

        assertThrows(EntityNotFoundException.class, () -> {
            service.getItem(oldName);
        });
        assertNotNull(service.getItem(newName));
    }

    @Test
    public void testChangeCategory() {
        String name = "CatChangeItem";
        String oldCat = "Cat1";
        service.addItem(oldCat, name, 1000, null);

        String newCat = "Cat2_New";
        service.changeCategory(name, newCat);

        List<MenuItem> catItems = service.getCategory(newCat.toLowerCase());
        assertNotNull(catItems);
        assertTrue(catItems.stream().anyMatch(i -> i.name.equals(name)));

        List<MenuItem> oldCatItems = service.getCategory(oldCat.toLowerCase());
        assertFalse(oldCatItems.stream().anyMatch(i -> i.name.equals(name)));
    }

    @Test
    public void testUpdateSide() {
        String name = "ItemWithSide";
        Map<String, Integer> sides = new HashMap<>();
        sides.put("Fries", 200);
        service.addItem("SideCat", name, 1000, sides);

        service.updateSide(name, "Fries", 500);

        MenuItem item = service.getItem(name);
        assertNotNull(item.sideOptions);
        assertEquals(500, item.sideOptions.get("Fries").price);
    }

    @Test
    public void testChangeCategorySameCategory() {
        String name = "SameCatItem";
        String cat = "SameCat";
        service.addItem(cat, name, 1000, null);

        service.changeCategory(name, cat);

        assertEquals(cat.toLowerCase(), service.getCategoryOfItem(name));
    }

    @Test
    public void testRenameCategoryNotFound() {
        assertThrows(EntityNotFoundException.class, () -> {
            service.renameCategory("NonExistentCat", "NewCat");
        });
    }

    @Test
    public void testGetCategoryOfItemFound() {
        String name = "FindMe";
        String cat = "Hideout";
        service.addItem(cat, name, 1000, null);

        assertEquals(cat.toLowerCase(), service.getCategoryOfItem(name));
    }

    @Test
    public void testGetCategoryOfItemNotFound() {
        assertThrows(EntityNotFoundException.class, () -> {
            service.getCategoryOfItem("InvisibleItem");
        });
    }

    @Test
    public void testGetCategoryNotFound() {
        assertThrows(EntityNotFoundException.class, () -> {
            service.getCategory("GhostCategory");
        });
    }
}
