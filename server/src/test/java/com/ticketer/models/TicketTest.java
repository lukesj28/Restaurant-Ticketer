package com.ticketer.models;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class TicketTest {

    @Test
    public void testTicketInitialization() {
        long before = System.currentTimeMillis();
        Ticket ticket = new Ticket(101);
        long after = System.currentTimeMillis();

        assertEquals(101, ticket.getId());
        assertEquals("", ticket.getTableNumber());
        assertEquals(0.0, ticket.getSubtotal(), 0.001);
        assertEquals(0.0, ticket.getTotal(), 0.001);
        assertTrue(ticket.getOrders().isEmpty());
        assertTrue("Created at should be current", ticket.getCreatedAt() >= before && ticket.getCreatedAt() <= after);
    }

    @Test
    public void testSetTableNumber() {
        Ticket ticket = new Ticket(1);
        ticket.setTableNumber("Table 5");
        assertEquals("Table 5", ticket.getTableNumber());
    }

    @Test
    public void testAddOrdersAndCalculatePrice() {
        Ticket ticket = new Ticket(1);

        Order order1 = new Order(0.1);
        order1.addItem(new Item("A", null, 10.0));

        Order order2 = new Order(0.1);
        order2.addItem(new Item("B", null, 5.0));

        ticket.addOrder(order1);
        assertEquals(10.0, ticket.getSubtotal(), 0.001);
        assertEquals(11.0, ticket.getTotal(), 0.001);

        ticket.addOrder(order2);
        assertEquals(15.0, ticket.getSubtotal(), 0.001);
        assertEquals(16.5, ticket.getTotal(), 0.001);

        List<Order> orders = ticket.getOrders();
        assertEquals(2, orders.size());
        assertTrue(orders.contains(order1));
        assertTrue(orders.contains(order2));
    }

    @Test
    public void testOrdersListImmutability() {
        Ticket ticket = new Ticket(1);
        ticket.addOrder(new Order());

        List<Order> retrievedOrders = ticket.getOrders();
        retrievedOrders.clear();

        assertEquals(1, ticket.getOrders().size());
    }

    @Test
    public void testDifferentTaxRates() {
        Ticket ticket = new Ticket(1);

        Order orderZeroTax = new Order(0.0);
        orderZeroTax.addItem(new Item("Item", null, 100.0));
        ticket.addOrder(orderZeroTax);
        assertEquals(100.0, ticket.getTotal(), 0.001);

        Order orderHighTax = new Order(0.25);
        orderHighTax.addItem(new Item("Item", null, 100.0));
        ticket.addOrder(orderHighTax);

        assertEquals(225.0, ticket.getTotal(), 0.001);
    }

    @Test
    public void testRemoveOrder() {
        Ticket ticket = new Ticket(1);
        Order order = new Order(0.1);
        order.addItem(new Item("Item", null, 100.0));

        ticket.addOrder(order);
        assertEquals(110.0, ticket.getTotal(), 0.001);

        assertTrue(ticket.removeOrder(order));
        assertEquals(0.0, ticket.getTotal(), 0.001);
        assertEquals(0.0, ticket.getSubtotal(), 0.001);
        assertTrue(ticket.getOrders().isEmpty());

        assertFalse(ticket.removeOrder(order));
    }
}
