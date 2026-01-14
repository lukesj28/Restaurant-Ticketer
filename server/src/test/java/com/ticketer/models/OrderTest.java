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
        assertEquals(0.0, order.getSubtotal(), 0.001);
        assertEquals(0.0, order.getTotal(), 0.001);
    }

    @Test
    public void testAddItemsAndCalculateTotal() {
        Order order = new Order();
        Item item1 = new Item("Burger", null, 10.0);
        Item item2 = new Item("Fries", "Large", 5.5);

        order.addItem(item1);
        order.addItem(item2);

        List<Item> items = order.getItems();
        assertEquals(2, items.size());

        assertTrue(items.contains(item1));
        assertTrue(items.contains(item2));

        assertEquals(15.5, order.getSubtotal(), 0.001);
        assertEquals(15.5, order.getTotal(), 0.001);
    }

    @Test
    public void testImmutabilityOfGetItems() {
        Order order = new Order();
        Item item1 = new Item("Burger", null, 10.0);
        order.addItem(item1);

        List<Item> retrievedItems = order.getItems();
        retrievedItems.clear();

        assertEquals(1, order.getItems().size());
    }

    @Test
    public void testTaxCalculation() {
        Order order = new Order(0.10);
        Item item = new Item("Burger", null, 10.0);

        order.addItem(item);

        assertEquals(10.0, order.getSubtotal(), 0.001);
        assertEquals(11.0, order.getTotal(), 0.001);
        assertEquals(0.10, order.getTaxRate(), 0.001);

        order.setTaxRate(0.20);
        assertEquals(12.0, order.getTotal(), 0.001);
    }
}
