package com.ticketer.services;

import com.ticketer.models.MenuItem;
import com.ticketer.models.MenuItemView;
import com.ticketer.repositories.FileMenuRepository;
import com.ticketer.exceptions.EntityNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class MenuServiceTest {

    private MenuService service;
    private static final String TEST_MENU_PATH = "target/test-menu-service.json";

    @Before
    public void setUp() {
        File dataDir = new File("target");
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }

        try {
            String json = "{ \"TestCategory\": { \"TestItemAdd\": { \"price\": 12.34, \"available\": true, \"sides\": {} } } }";
            Files.write(new File(TEST_MENU_PATH).toPath(), json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to set up test environment", e);
        }

        service = new MenuService(new FileMenuRepository(TEST_MENU_PATH));
    }

    @After
    public void tearDown() {
        try {
            Files.deleteIfExists(new File(TEST_MENU_PATH).toPath());
        } catch (Exception e) {
        }
    }

    @Test
    public void testInitialization() {
        assertNotNull("Categories should function", service.getCategories());
    }

    @Test
    public void testGetItem() {
        List<MenuItemView> items = service.getAllItems();
        if (!items.isEmpty()) {
            MenuItemView item = items.get(0);
            MenuItem found = service.getItem(item.name);
            assertNotNull("Should find existing item", found);
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

        try {
            service.getItem(nameToRemove);
            fail("Expected EntityNotFoundException");
        } catch (EntityNotFoundException e) {
        }
    }

    @Test
    public void testRenameCategory() {
        String oldCat = "OldCat";
        service.addItem(oldCat, "ItemInCat", 1000, null);

        String newCat = "NewCat_Renamed";
        service.renameCategory(oldCat, newCat);

        try {
            service.getCategory(oldCat.toLowerCase());
            fail("Old category should be gone");
        } catch (EntityNotFoundException e) {
        }
        assertNotNull("New category should exist", service.getCategory(newCat.toLowerCase()));
    }

    @Test
    public void testGetItemObject() {
        if (service.getAllItems().isEmpty()) {
            service.addItem("Cat", "Item", 1000, null);
        }

        MenuItemView view = service.getAllItems().get(0);
        MenuItem menuItem = service.getItem(view.name);

        com.ticketer.models.OrderItem item = com.ticketer.models.Menu.getItem(menuItem, null);

        assertNotNull("Should create OrderItem object", item);
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
        assertEquals(price, item.basePrice);

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

        assertEquals(newPrice, service.getItem(name).basePrice);
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

        try {
            service.getItem(oldName);
            fail("Old item name should not exist");
        } catch (EntityNotFoundException e) {
        }
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

    @Test(expected = EntityNotFoundException.class)
    public void testRenameCategoryNotFound() {
        service.renameCategory("NonExistentCat", "NewCat");
    }

    @Test
    public void testGetCategoryOfItemFound() {
        String name = "FindMe";
        String cat = "Hideout";
        service.addItem(cat, name, 1000, null);

        assertEquals(cat.toLowerCase(), service.getCategoryOfItem(name));
    }

    @Test(expected = EntityNotFoundException.class)
    public void testGetCategoryOfItemNotFound() {
        service.getCategoryOfItem("InvisibleItem");
    }

    @Test(expected = EntityNotFoundException.class)
    public void testGetCategoryNotFound() {
        service.getCategory("GhostCategory");
    }
}
