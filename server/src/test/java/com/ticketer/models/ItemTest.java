package com.ticketer.models;

import org.junit.Test;
import static org.junit.Assert.*;

public class ItemTest {

    @Test
    public void testToString() {
        Item item = new Item("Name", "Side", 10.0);
        assertNotNull(item.toString());

        Item item2 = new Item("Name", null, 10.0);
        assertNotNull(item2.toString());
    }

    @Test
    public void testGetName() {
        Item item = new Item("Burger", "Fries", 15.50);
        assertEquals("Burger", item.getName());
    }

    @Test
    public void testGetSelectedSide() {
        Item item = new Item("Burger", "Fries", 15.50);
        assertEquals("Fries", item.getSelectedSide());
    }

    @Test
    public void testGetSelectedSideNull() {
        Item item = new Item("Burger", null, 15.50);
        assertNull(item.getSelectedSide());
    }

    @Test
    public void testGetTotalPrice() {
        Item item = new Item("Burger", "Fries", 15.50);
        assertEquals(15.50, item.getTotalPrice(), 0.001);
    }
}
