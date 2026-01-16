package com.ticketer.models;

import org.junit.Test;
import static org.junit.Assert.*;

public class OrderItemTest {

    @Test
    public void testToString() {
        OrderItem item = new OrderItem("Name", "Side", 1000);
        assertNotNull(item.toString());

        OrderItem item2 = new OrderItem("Name", null, 1000);
        assertNotNull(item2.toString());
    }

    @Test
    public void testGetName() {
        OrderItem item = new OrderItem("Burger", "Fries", 1550);
        assertEquals("Burger", item.getName());
    }

    @Test
    public void testGetSelectedSide() {
        OrderItem item = new OrderItem("Burger", "Fries", 1550);
        assertEquals("Fries", item.getSelectedSide());
    }

    @Test
    public void testGetSelectedSideNull() {
        OrderItem item = new OrderItem("Burger", null, 1550);
        assertNull(item.getSelectedSide());
    }

    @Test
    public void testGetPrice() {
        OrderItem item = new OrderItem("Burger", "Fries", 1550);
        assertEquals(1550, item.getPrice());
    }
}
