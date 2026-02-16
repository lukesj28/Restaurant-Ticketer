package com.ticketer.models;

import org.junit.Test;
import static org.junit.Assert.*;

public class OrderItemTest {

    @Test
    public void testConstructorAndGetters() {
        OrderItem item = new OrderItem("Burger", "Fries", null, 1200, 300, 0, null);
        assertEquals("Burger", item.getName());
        assertEquals("Fries", item.getSelectedSide());
        assertEquals(1200, item.getMainPrice());
        assertEquals(300, item.getSidePrice());
        assertEquals(1500, item.getPrice());
    }

    @Test
    public void testConstructorWithNoSide() {
        OrderItem item = new OrderItem("Soda", null, null, 200, 0, 0, null);
        assertEquals("Soda", item.getName());
        assertNull(item.getSelectedSide());
        assertEquals(200, item.getMainPrice());
        assertEquals(0, item.getSidePrice());
        assertEquals(200, item.getPrice());
    }

    @Test
    public void testToString() {
        OrderItem item = new OrderItem("Burger", "Fries", null, 1200, 300, 0, null);
        String str = item.toString();
        assertNotNull(str);
        assertTrue(str.contains("Burger"));
        assertTrue(str.contains("Fries"));
        assertTrue(str.contains("12.00"));
        assertTrue(str.contains("3.00"));
        assertTrue(str.contains("15.00"));
    }

    @Test
    public void testToStringNoSide() {
        OrderItem item = new OrderItem("Soda", null, null, 200, 0, 0, null);
        String str = item.toString();
        assertNotNull(str);
        assertTrue(str.contains("Soda"));
        assertTrue(str.contains("2.00"));
        assertFalse(str.contains("Side:"));
    }
}
