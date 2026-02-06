package com.ticketer.models;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MenuTest {

    @Test
    public void testGetCategories() {
        Map<String, List<MenuItem>> categories = new HashMap<>();
        List<MenuItem> mains = new ArrayList<>();
        mains.add(new MenuItem("Burger", 1000, true, null, null));
        categories.put("mains", mains);

        Menu menu = new Menu(categories, null, null);
        assertEquals(categories, menu.getCategories());
        assertNotNull(menu.getCategory("mains"));
        assertEquals(1, menu.getCategory("mains").size());
        assertNull(menu.getCategory("desserts"));
    }

    @Test
    public void testGetAllItems() {
        Map<String, List<MenuItem>> categories = new HashMap<>();
        List<MenuItem> mains = new ArrayList<>();
        mains.add(new MenuItem("Burger", 1000, true, null, null));
        categories.put("mains", mains);

        Menu menu = new Menu(categories, null, null);
        List<MenuItemView> items = menu.getAllItems();
        assertEquals(1, items.size());
        assertEquals("Burger", items.get(0).name);

    }

    @Test
    public void testGetItem() {
        Map<String, List<MenuItem>> categories = new HashMap<>();
        List<MenuItem> mains = new ArrayList<>();
        mains.add(new MenuItem("Burger", 1000, true, null, null));
        categories.put("mains", mains);

        Menu menu = new Menu(categories, null, null);
        assertNotNull(menu.getItem("Burger"));
        assertNull(menu.getItem("Pizza"));
    }

    @Test
    public void testGetItemWithSides() {
        Map<String, Side> sides = new HashMap<>();
        Side chips = new Side();
        chips.price = 200;
        sides.put("chips", chips);
        MenuItem fish = new MenuItem("Fish", 1000, true, sides, null);

        OrderItem result = Menu.getItem(fish, "chips");
        assertEquals("Fish", result.getName());
        assertEquals("chips", result.getSelectedSide());
        assertEquals(1000, result.getMainPrice());
        assertEquals(200, result.getSidePrice());
        assertEquals(1200, result.getPrice());
    }

    @Test
    public void testGetItemWithoutSides() {
        MenuItem burger = new MenuItem("Burger", 1000, true, null, null);
        OrderItem result = Menu.getItem(burger, null);
        assertEquals("Burger", result.getName());
        assertEquals(1000, result.getMainPrice());
        assertEquals(0, result.getSidePrice());
        assertEquals(1000, result.getPrice());
        assertNull(result.getSelectedSide());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetItemInvalidSide() {
        Map<String, Side> sides = new HashMap<>();
        sides.put("fries", new Side());
        MenuItem fish = new MenuItem("Fish", 1000, true, sides, null);
        Menu.getItem(fish, "invalid_side");
    }

    @Test
    public void testGetItemWithSideIgnored() {
        MenuItem burger = new MenuItem("Burger", 1000, true, null, null);
        OrderItem result = Menu.getItem(burger, "Fries");
        assertEquals("Burger", result.getName());
        assertEquals(1000, result.getMainPrice());
        assertEquals(0, result.getSidePrice());
        assertEquals(1000, result.getPrice());
        assertNull(result.getSelectedSide());
    }
}
