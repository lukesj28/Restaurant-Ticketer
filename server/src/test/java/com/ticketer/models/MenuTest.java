package com.ticketer.models;

import com.ticketer.utils.menu.dto.ComplexItem;
import com.ticketer.utils.menu.dto.MenuItemView;
import com.ticketer.utils.menu.dto.Side;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MenuTest {

    @Test
    public void testGetCategories() {
        Map<String, List<ComplexItem>> categories = new HashMap<>();
        List<ComplexItem> mains = new ArrayList<>();
        mains.add(new ComplexItem("Burger", 1000, true, null));
        categories.put("mains", mains);

        Menu menu = new Menu(categories);
        assertEquals(categories, menu.getCategories());
        assertNotNull(menu.getCategory("mains"));
        assertEquals(1, menu.getCategory("mains").size());
        assertNull(menu.getCategory("desserts"));
    }

    @Test
    public void testGetAllItems() {
        Map<String, List<ComplexItem>> categories = new HashMap<>();
        List<ComplexItem> mains = new ArrayList<>();
        mains.add(new ComplexItem("Burger", 1000, true, null));
        categories.put("mains", mains);

        Menu menu = new Menu(categories);
        List<MenuItemView> items = menu.getAllItems();
        assertEquals(1, items.size());
        assertEquals("Burger", items.get(0).name);
        assertEquals("mains", items.get(0).category);
    }

    @Test
    public void testGetItem() {
        Map<String, List<ComplexItem>> categories = new HashMap<>();
        List<ComplexItem> mains = new ArrayList<>();
        mains.add(new ComplexItem("Burger", 1000, true, null));
        categories.put("mains", mains);

        Menu menu = new Menu(categories);
        assertNotNull(menu.getItem("Burger"));
        assertNull(menu.getItem("Pizza"));
    }

    @Test
    public void testGetItemWithSides() {
        Map<String, Side> sides = new HashMap<>();
        Side chips = new Side();
        chips.price = 200;
        sides.put("chips", chips);
        ComplexItem fish = new ComplexItem("Fish", 1000, true, sides);

        Item result = Menu.getItem(fish, "chips");
        assertEquals("Fish", result.getName());
        assertEquals("chips", result.getSelectedSide());
        assertEquals(1200, result.getPrice());
    }

    @Test
    public void testGetItemWithoutSides() {
        ComplexItem burger = new ComplexItem("Burger", 1000, true, null);
        Item result = Menu.getItem(burger, null);
        assertEquals("Burger", result.getName());
        assertEquals(1000, result.getPrice());
        assertNull(result.getSelectedSide());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetItemInvalidSide() {
        Map<String, Side> sides = new HashMap<>();
        sides.put("fries", new Side());
        ComplexItem fish = new ComplexItem("Fish", 1000, true, sides);
        Menu.getItem(fish, "invalid_side");
    }
}
