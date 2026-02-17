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
        assertEquals(0, ticket.getTax());
        assertTrue(ticket.getOrders().isEmpty());
        assertNotNull(ticket.getCreatedAt());
        assertTrue(ticket.getCreatedAt().isAfter(java.time.Instant.now().minusSeconds(10)));
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

        Order order1 = new Order(1000);
        order1.addItem(new OrderItem("test-category", "A", null, null, 1000, 0, 0, null));

        Order order2 = new Order(1000);
        order2.addItem(new OrderItem("test-category", "B", null, null, 500, 0, 0, null));

        ticket.addOrder(order1);
        assertEquals(1000, ticket.getSubtotal());
        assertEquals(1100, ticket.getTotal());
        assertEquals(100, ticket.getTax());

        ticket.addOrder(order2);
        assertEquals(1500, ticket.getSubtotal());
        assertEquals(1650, ticket.getTotal());
        assertEquals(150, ticket.getTax());

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

        Order orderZeroTax = new Order(0);
        orderZeroTax.addItem(new OrderItem("test-category", "Item", null, null, 10000, 0, 0, null));
        ticket.addOrder(orderZeroTax);
        assertEquals(10000, ticket.getTotal());
        assertEquals(0, ticket.getTax());

        Order orderHighTax = new Order(2500);
        orderHighTax.addItem(new OrderItem("test-category", "Item", null, null, 10000, 0, 0, null));
        ticket.addOrder(orderHighTax);

        assertEquals(22500, ticket.getTotal());
        assertEquals(2500, ticket.getTax());
    }

    @Test
    public void testRemoveOrder() {
        Ticket ticket = new Ticket(1);
        Order order = new Order(1000);
        order.addItem(new OrderItem("test-category", "Item", null, null, 10000, 0, 0, null));

        ticket.addOrder(order);
        assertEquals(11000, ticket.getTotal());
        assertEquals(1000, ticket.getTax());

        assertTrue(ticket.removeOrder(order));
        assertEquals(0, ticket.getTotal());
        assertEquals(0, ticket.getSubtotal());
        assertEquals(0, ticket.getTax());
        assertTrue(ticket.getOrders().isEmpty());

        assertFalse(ticket.removeOrder(order));
    }
}
