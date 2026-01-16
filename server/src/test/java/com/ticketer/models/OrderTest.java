package com.ticketer.models;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

public class OrderTest {

    @Test
    public void testCreateEmptyOrder() {
        Order order = new Order();
        assertNotNull(order.getItems());
        assertTrue(order.getItems().isEmpty());
        assertEquals(0, order.getSubtotal());
        assertEquals(0, order.getTotal());
    }

    @Test
    public void testAddItemsAndCalculateTotal() {
        Order order = new Order();
        OrderItem item1 = new OrderItem("Burger", null, 1000);
        OrderItem item2 = new OrderItem("Fries", "Large", 550);

        order.addItem(item1);
        order.addItem(item2);

        List<OrderItem> items = order.getItems();
        assertEquals(2, items.size());

        assertTrue(items.contains(item1));
        assertTrue(items.contains(item2));

        assertEquals(1550, order.getSubtotal());
        assertEquals(1550, order.getTotal());
    }

    @Test
    public void testImmutabilityOfGetItems() {
        Order order = new Order();
        OrderItem item1 = new OrderItem("Burger", null, 1000);
        order.addItem(item1);

        List<OrderItem> retrievedItems = order.getItems();
        retrievedItems.clear();

        assertEquals(1, order.getItems().size());
    }

    @Test
    public void testTaxCalculation() {
        Order order = new Order(0.10);
        OrderItem item = new OrderItem("Burger", null, 1000);

        order.addItem(item);

        assertEquals(1000, order.getSubtotal());
        assertEquals(1100, order.getTotal());
        assertEquals(0.10, order.getTaxRate(), 0.001);

        order.setTaxRate(0.20);
        assertEquals(1200, order.getTotal());
    }
}
