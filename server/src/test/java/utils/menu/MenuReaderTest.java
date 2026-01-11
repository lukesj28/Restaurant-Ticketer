package utils.menu;

import org.junit.Test;
import static org.junit.Assert.*;
import utils.menu.dto.ComplexItem;
import utils.menu.dto.Side;
import models.Item;
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

        Item result = MenuReader.getItem(complexItem, "chips");

        assertNotNull(result);
        assertEquals("Fish", result.name);
        assertEquals("chips", result.selectedSide);
        assertEquals(12.00, result.totalPrice, 0.001);
    }

    @Test
    public void testGetItemNoSides() {
        ComplexItem complexItem = new ComplexItem("Burger", 5.00, true, null);

        Item result = MenuReader.getItem(complexItem, null);

        assertEquals(5.00, result.totalPrice, 0.001);
        assertNull(result.selectedSide);
    }

    @Test
    public void testGetItemWithSidesButNullSelection() {
        Map<String, Side> sides = new HashMap<>();
        sides.put("chips", new Side());
        ComplexItem complexItem = new ComplexItem("Fish", 10.00, true, sides);

        Item result = MenuReader.getItem(complexItem, null);

        assertEquals(10.00, result.totalPrice, 0.001);
        assertNull(result.selectedSide);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSideSelection() {
        Map<String, Side> sides = new HashMap<>();
        sides.put("fries", new Side());
        ComplexItem complexItem = new ComplexItem("Fish", 10.00, true, sides);

        MenuReader.getItem(complexItem, "lobster");
    }

    @Test
    public void testConstructor() {
        new MenuReader();
    }

    @Test
    public void testGetItemDetailsNotFound() {
        assertNull(MenuReader.getItemDetails("non_existent"));
    }

    @Test
    public void testGetAllItemsError() {
        String originalPath = System.getProperty("menu.file");
        System.setProperty("menu.file", "invalid_path/missing.json");
        try {
            var items = MenuReader.getAllItems();
            assertTrue(items.isEmpty());
        } finally {
            if (originalPath != null)
                System.setProperty("menu.file", originalPath);
            else
                System.clearProperty("menu.file");
        }
    }

    @Test
    public void testGetItemDetailsError() {
        String originalPath = System.getProperty("menu.file");
        System.setProperty("menu.file", "invalid_path/missing.json");
        try {
            assertNull(MenuReader.getItemDetails("some_item"));
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
            var items = MenuReader.getAllItems();
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
}
