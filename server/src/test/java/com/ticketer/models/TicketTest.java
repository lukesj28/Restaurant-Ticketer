package com.ticketer.models;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class TicketTest {

    @Test
    public void testTicketInitialization() {
        Ticket ticket = new Ticket(101);

        assertEquals(101, ticket.getId());
        assertEquals("", ticket.getTableNumber());
        assertEquals(0, ticket.getSubtotal());
        assertEquals(0, ticket.getTotal());
        assertTrue(ticket.getOrders().isEmpty());
        assertNotNull(ticket.getCreatedAt());
        try {
            java.time.Instant.parse(ticket.getCreatedAt());
        } catch (java.time.format.DateTimeParseException e) {
            fail("createdAt should be valid ISO-8601 timestamp");
        }
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
        order1.addItem(new Item("A", null, 1000));

        Order order2 = new Order(0.1);
        order2.addItem(new Item("B", null, 500));

        ticket.addOrder(order1);
        assertEquals(1000, ticket.getSubtotal());
        assertEquals(1100, ticket.getTotal());

        ticket.addOrder(order2);
        assertEquals(1500, ticket.getSubtotal());
        assertEquals(1650, ticket.getTotal());

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
        orderZeroTax.addItem(new Item("Item", null, 10000));
        ticket.addOrder(orderZeroTax);
        assertEquals(10000, ticket.getTotal());

        Order orderHighTax = new Order(0.25);
        orderHighTax.addItem(new Item("Item", null, 10000));
        ticket.addOrder(orderHighTax);

        assertEquals(22500, ticket.getTotal());
    }

    @Test
    public void testRemoveOrder() {
        Ticket ticket = new Ticket(1);
        Order order = new Order(0.1);
        order.addItem(new Item("Item", null, 10000));

        ticket.addOrder(order);
        assertEquals(11000, ticket.getTotal());

        assertTrue(ticket.removeOrder(order));
        assertEquals(0, ticket.getTotal());
        assertEquals(0, ticket.getSubtotal());
        assertTrue(ticket.getOrders().isEmpty());

        assertFalse(ticket.removeOrder(order));
    }
}
