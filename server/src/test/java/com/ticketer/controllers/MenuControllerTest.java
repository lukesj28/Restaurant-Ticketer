package com.ticketer.controllers;

import com.ticketer.utils.menu.dto.ComplexItem;
import com.ticketer.utils.menu.dto.MenuItemView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
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
        Files.copy(new File(ORIGINAL_MENU_PATH).toPath(), new File(TEST_MENU_PATH).toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        System.setProperty("menu.file", TEST_MENU_PATH);

        controller = new MenuController();
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(new File(TEST_MENU_PATH).toPath());
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
        List<MenuItemView> items = controller.getAllItems();
        if (items.isEmpty())
            return;

        String nameToRemove = items.get(0).name;

        controller.removeItem(nameToRemove);

        assertNull("Item should be removed", controller.getItem(nameToRemove));
    }

    @Test
    public void testRenameCategory() throws IOException {
        if (controller.getCategories().isEmpty())
            return;

        String oldCat = controller.getCategories().keySet().iterator().next();
        String newCat = "new_category_name";

        controller.renameCategory(oldCat, newCat);

        assertNull("Old category should be gone", controller.getCategory(oldCat));
        assertNotNull("New category should exist", controller.getCategory(newCat));
    }

    @Test
    public void testGetItemObject() {
        if (controller.getAllItems().isEmpty())
            return;

        MenuItemView view = controller.getAllItems().get(0);
        ComplexItem complexItem = controller.getItem(view.name);

        com.ticketer.models.Item item = controller.getItem(complexItem, null);

        assertNotNull("Should create Item object", item);
        assertEquals(complexItem.name, item.getName());
    }
}
