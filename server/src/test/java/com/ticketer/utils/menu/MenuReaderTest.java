package com.ticketer.utils.menu;

import org.junit.Test;
import static org.junit.Assert.*;
import com.ticketer.utils.menu.dto.ComplexItem;
import com.ticketer.utils.menu.dto.Side;
import com.ticketer.models.Item;
import com.ticketer.models.Menu;

import java.util.Map;
import java.util.HashMap;

public class MenuReaderTest {

    @Test
    public void testGetItemCalculation() {
        Map<String, Side> sides = new HashMap<>();
        Side chips = new Side();
        chips.price = 2.00;
        sides.put("chips", chips);

        ComplexItem complexItem = new ComplexItem("Fish", 10.00, true, sides);

        Item result = Menu.getItem(complexItem, "chips");

        assertNotNull(result);
        assertEquals("Fish", result.getName());
        assertEquals("chips", result.getSelectedSide());
        assertEquals(12.00, result.getPrice(), 0.001);
    }

    @Test
    public void testGetItemNoSides() {
        ComplexItem complexItem = new ComplexItem("Burger", 5.00, true, null);

        Item result = Menu.getItem(complexItem, null);

        assertEquals(5.00, result.getPrice(), 0.001);
        assertNull(result.getSelectedSide());
    }

    @Test
    public void testGetItemWithSidesButNullSelection() {
        Map<String, Side> sides = new HashMap<>();
        sides.put("chips", new Side());
        ComplexItem complexItem = new ComplexItem("Fish", 10.00, true, sides);

        Item result = Menu.getItem(complexItem, null);

        assertEquals(10.00, result.getPrice(), 0.001);
        assertNull(result.getSelectedSide());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSideSelection() {
        Map<String, Side> sides = new HashMap<>();
        sides.put("fries", new Side());
        ComplexItem complexItem = new ComplexItem("Fish", 10.00, true, sides);

        Menu.getItem(complexItem, "lobster");
    }

    @Test
    public void testConstructor() {
        new MenuReader();
    }

    @Test(expected = com.ticketer.exceptions.StorageException.class)
    public void testGetAllItemsError() {
        String originalPath = System.getProperty("menu.file");
        System.setProperty("menu.file", "invalid_path/missing.json");
        try {
            MenuReader.readMenu();
        } finally {
            if (originalPath != null)
                System.setProperty("menu.file", originalPath);
            else
                System.clearProperty("menu.file");
        }
    }

    @Test(expected = com.ticketer.exceptions.StorageException.class)
    public void testGetItemDetailsError() {
        String originalPath = System.getProperty("menu.file");
        System.setProperty("menu.file", "invalid_path/missing.json");
        try {
            MenuReader.readMenu();
        } finally {
            if (originalPath != null)
                System.setProperty("menu.file", originalPath);
            else
                System.clearProperty("menu.file");
        }
    }

    @Test
    public void testLoadMenuIntegration() {
        try {
            var items = MenuReader.readMenu().getAllItems();
            assertNotNull(items);
            assertFalse(items.isEmpty());

            for (var item : items) {
                assertNotNull(item.name);
                assertTrue(item.price >= 0);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetItemDetailsNotFound() {
        assertNull(MenuReader.readMenu().getItem("non_existent"));
    }

    @Test
    public void testReadMenu() {
        Menu menu = MenuReader.readMenu();
        assertNotNull(menu);
        assertNotNull(menu.getCategories());
        assertFalse(menu.getCategories().isEmpty());

        assertTrue(menu.getCategories().containsKey("mains"));
        java.util.List<ComplexItem> mains = menu.getCategories().get("mains");
        assertFalse(mains.isEmpty());

        ComplexItem halibut = mains.stream().filter(i -> i.name.equals("halibut")).findFirst().orElse(null);
        assertNotNull(halibut);
        assertTrue(halibut.hasSides());
    }
}
