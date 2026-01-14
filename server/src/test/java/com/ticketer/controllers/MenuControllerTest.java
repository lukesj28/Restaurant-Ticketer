package com.ticketer.controllers;

import com.ticketer.utils.menu.dto.ComplexItem;
import com.ticketer.utils.menu.dto.MenuItemView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class MenuControllerTest {

    private MenuController controller;
    private static final String ORIGINAL_MENU_PATH = "data/menu.json";
    private static final String TEST_MENU_PATH = "target/test-menu-controller.json";

    @Before
    public void setUp() throws IOException {
        File dataDir = new File("target");
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }
        File original = new File(ORIGINAL_MENU_PATH);
        if (original.exists()) {
            Files.copy(original.toPath(), new File(TEST_MENU_PATH).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        System.setProperty("menu.file", TEST_MENU_PATH);
        controller = new MenuController();
    }

    @After
    public void tearDown() throws IOException {
        try {
            Files.deleteIfExists(new File(TEST_MENU_PATH).toPath());
        } catch (Exception e) {
        }
        System.clearProperty("menu.file");
    }

    @Test
    public void testInitialization() {
        assertNotNull("Menu should be loaded", controller.getMenu());
        assertNotNull("Categories should function", controller.getCategories());
    }

    @Test
    public void testGetItem() {
        List<MenuItemView> items = controller.getAllItems();
        if (!items.isEmpty()) {
            MenuItemView item = items.get(0);
            ComplexItem found = controller.getItem(item.name);
            assertNotNull("Should find existing item", found);
            assertEquals(item.name, found.name);
        }
    }

    @Test
    public void testRemoveItem() throws IOException {
        if (controller.getAllItems().isEmpty()) {
            controller.addItem("TempCat", "ToRemove", 10.0, null);
        }

        String nameToRemove = controller.getAllItems().get(0).name;
        controller.removeItem(nameToRemove);
        assertNull("Item should be removed", controller.getItem(nameToRemove));
    }

    @Test
    public void testRenameCategory() throws IOException {
        String oldCat = "OldCat";
        controller.addItem(oldCat, "ItemInCat", 10.0, null);

        String newCat = "NewCat_Renamed";
        controller.renameCategory(oldCat, newCat);

        assertNull("Old category should be gone", controller.getCategory(oldCat.toLowerCase()));
        assertNotNull("New category should exist", controller.getCategory(newCat.toLowerCase()));
    }

    @Test
    public void testGetItemObject() {
        if (controller.getAllItems().isEmpty()) {
            try {
                controller.addItem("Cat", "Item", 10.0, null);
            } catch (IOException e) {
            }
        }

        MenuItemView view = controller.getAllItems().get(0);
        ComplexItem complexItem = controller.getItem(view.name);

        com.ticketer.models.Item item = controller.getItem(complexItem, null);

        assertNotNull("Should create Item object", item);
        assertEquals(complexItem.name, item.getName());
    }

    @Test
    public void testGetItemWithSide() throws IOException {
        String name = "ItemSideTest";
        Map<String, Double> sides = new HashMap<>();
        sides.put("Fries", 2.0);
        controller.addItem("SideCat", name, 10.0, sides);

        ComplexItem complexItem = controller.getItem(name);
        com.ticketer.models.Item item = controller.getItem(complexItem, "Fries");

        assertNotNull(item);
        assertEquals(name, item.getName());
    }

    @Test
    public void testAddItem() throws IOException {
        String cat = "TestCategory";
        String name = "TestItemAdd";
        double price = 12.34;

        controller.addItem(cat, name, price, null);

        ComplexItem item = controller.getItem(name);
        assertNotNull(item);
        assertEquals(name, item.name);
        assertEquals(price, item.basePrice, 0.001);

        List<ComplexItem> catItems = controller.getCategory(cat.toLowerCase());
        assertNotNull(catItems);
        assertTrue(catItems.stream().anyMatch(i -> i.name.equals(name)));
    }

    @Test
    public void testEditItemPrice() throws IOException {
        String name = "PriceItem";
        controller.addItem("Cat", name, 10.0, null);

        double newPrice = 99.99;
        controller.editItemPrice(name, newPrice);

        assertEquals(newPrice, controller.getItem(name).basePrice, 0.001);
    }

    @Test
    public void testEditItemAvailability() throws IOException {
        String name = "AvailItem";
        controller.addItem("Cat", name, 10.0, null);

        controller.editItemAvailability(name, false);
        assertFalse(controller.getItem(name).available);

        controller.editItemAvailability(name, true);
        assertTrue(controller.getItem(name).available);
    }

    @Test
    public void testRenameItem() throws IOException {
        String oldName = "OldNameItem";
        controller.addItem("Cat", oldName, 10.0, null);

        String newName = "RenamedItem_" + System.currentTimeMillis();
        controller.renameItem(oldName, newName);

        assertNull(controller.getItem(oldName));
        assertNotNull(controller.getItem(newName));
    }

    @Test
    public void testChangeCategory() throws IOException {
        String name = "CatChangeItem";
        String oldCat = "Cat1";
        controller.addItem(oldCat, name, 10.0, null);

        String newCat = "Cat2_New";
        controller.changeCategory(name, newCat);

        List<ComplexItem> catItems = controller.getCategory(newCat.toLowerCase());
        assertNotNull(catItems);
        assertTrue(catItems.stream().anyMatch(i -> i.name.equals(name)));

        List<ComplexItem> oldCatItems = controller.getCategory(oldCat.toLowerCase());
        if (oldCatItems != null) {
            assertFalse(oldCatItems.stream().anyMatch(i -> i.name.equals(name)));
        }
    }

    @Test
    public void testUpdateSide() throws IOException {
        String name = "ItemWithSide";
        Map<String, Double> sides = new HashMap<>();
        sides.put("Fries", 2.0);
        controller.addItem("SideCat", name, 10.0, sides);

        controller.updateSide(name, "Fries", 5.0);

        ComplexItem item = controller.getItem(name);
        assertNotNull(item.sideOptions);
        assertEquals(5.0, item.sideOptions.get("Fries").price, 0.001);
    }

    @Test
    public void testGetMethodsWithNullMenu() throws Exception {
        Field menuField = MenuController.class.getDeclaredField("menu");
        menuField.setAccessible(true);
        menuField.set(controller, null);

        assertNull(controller.getItem("any"));
        assertNull(controller.getCategory("any"));
        assertTrue(controller.getAllItems().isEmpty());
        assertTrue(controller.getCategories().isEmpty());
    }
}
