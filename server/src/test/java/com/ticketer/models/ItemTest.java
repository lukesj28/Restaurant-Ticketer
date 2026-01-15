package com.ticketer.models;

import org.junit.Test;
import static org.junit.Assert.*;

public class ItemTest {

    @Test
    public void testToString() {
        Item item = new Item("Name", "Side", 1000);
        assertNotNull(item.toString());

        Item item2 = new Item("Name", null, 1000);
        assertNotNull(item2.toString());
    }

    @Test
    public void testGetName() {
        Item item = new Item("Burger", "Fries", 1550);
        assertEquals("Burger", item.getName());
    }

    @Test
    public void testGetSelectedSide() {
        Item item = new Item("Burger", "Fries", 1550);
        assertEquals("Fries", item.getSelectedSide());
    }

    @Test
    public void testGetSelectedSideNull() {
        Item item = new Item("Burger", null, 1550);
        assertNull(item.getSelectedSide());
    }

    @Test
    public void testGetPrice() {
        Item item = new Item("Burger", "Fries", 1550);
        assertEquals(1550, item.getPrice());
    }
}
