package com.ticketer.models;

import org.junit.Test;
import java.util.UUID;
import static org.junit.Assert.*;

public class OrderItemTest {

    @Test
    public void testForItemWithSide() {
        UUID itemId = UUID.randomUUID();
        UUID sideId = UUID.randomUUID();
        OrderItem item = OrderItem.forItem(itemId, "Burger", sideId, "Fries", 1200, 300);
        assertEquals("Burger", item.getName());
        assertEquals("Fries", item.getSelectedSide());
        assertEquals(itemId, item.getMenuItemId());
        assertEquals(sideId, item.getSelectedSideId());
        assertEquals(1200, item.getMainPrice());
        assertEquals(300, item.getSidePrice());
        assertEquals(1500, item.getPrice());
        assertEquals(OrderItem.TYPE_ITEM, item.getType());
        assertFalse(item.isCombo());
    }

    @Test
    public void testForItemWithNoSide() {
        UUID itemId = UUID.randomUUID();
        OrderItem item = OrderItem.forItem(itemId, "Soda", null, null, 200, 0);
        assertEquals("Soda", item.getName());
        assertNull(item.getSelectedSide());
        assertNull(item.getSelectedSideId());
        assertEquals(200, item.getMainPrice());
        assertEquals(0, item.getSidePrice());
        assertEquals(200, item.getPrice());
    }

    @Test
    public void testForCombo() {
        UUID comboId = UUID.randomUUID();
        OrderItem item = OrderItem.forCombo(comboId, "Meal Deal", null, null, 1500);
        assertEquals("Meal Deal", item.getName());
        assertEquals(comboId, item.getComboId());
        assertEquals(1500, item.getMainPrice());
        assertEquals(0, item.getSidePrice());
        assertEquals(1500, item.getPrice());
        assertEquals(OrderItem.TYPE_COMBO, item.getType());
        assertTrue(item.isCombo());
    }

    @Test
    public void testToStringWithSide() {
        OrderItem item = OrderItem.forItem(UUID.randomUUID(), "Burger", UUID.randomUUID(), "Fries", 1200, 300);
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
        OrderItem item = OrderItem.forItem(UUID.randomUUID(), "Soda", null, null, 200, 0);
        String str = item.toString();
        assertNotNull(str);
        assertTrue(str.contains("Soda"));
        assertTrue(str.contains("2.00"));
        assertFalse(str.contains("Side:"));
    }
}
