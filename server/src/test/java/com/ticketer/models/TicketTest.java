package com.ticketer.models;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class TicketTest {

    @Test
    public void testTicketInitialization() {
        long before = System.currentTimeMillis();
        Ticket ticket = new Ticket(101, 0.1);
        long after = System.currentTimeMillis();

        assertEquals(101, ticket.getId());
        assertEquals("", ticket.getTableNumber());
        assertEquals(0.0, ticket.getSubtotal(), 0.001);
        assertEquals(0.0, ticket.getTotal(), 0.001);
        assertEquals(0.1, ticket.getTaxRate(), 0.001);
        assertTrue(ticket.getOrders().isEmpty());
        assertTrue("Created at should be current", ticket.getCreatedAt() >= before && ticket.getCreatedAt() <= after);
    }

    @Test
    public void testSetTableNumber() {
        Ticket ticket = new Ticket(1, 0.1);
        ticket.setTableNumber("Table 5");
        assertEquals("Table 5", ticket.getTableNumber());
    }

    @Test
    public void testAddOrdersAndCalculatePrice() {
        Ticket ticket = new Ticket(1, 0.1); // 10% tax

        Order order1 = new Order();
        order1.addItem(new Item("A", null, 10.0));

        Order order2 = new Order();
        order2.addItem(new Item("B", null, 5.0));

        ticket.addOrder(order1);
        // Subtotal: 10.0, Total: 11.0
        assertEquals(10.0, ticket.getSubtotal(), 0.001);
        assertEquals(11.0, ticket.getTotal(), 0.001);

        ticket.addOrder(order2);
        // Subtotal: 15.0, Total: 16.5
        assertEquals(15.0, ticket.getSubtotal(), 0.001);
        assertEquals(16.5, ticket.getTotal(), 0.001);

        List<Order> orders = ticket.getOrders();
        assertEquals(2, orders.size());
        assertTrue(orders.contains(order1));
        assertTrue(orders.contains(order2));
    }

    @Test
    public void testOrdersListImmutability() {
        Ticket ticket = new Ticket(1, 0.1);
        ticket.addOrder(new Order());

        List<Order> retrievedOrders = ticket.getOrders();
        retrievedOrders.clear();

        assertEquals(1, ticket.getOrders().size());
    }

    @Test
    public void testDifferentTaxRates() {
        Ticket ticketZeroTax = new Ticket(1, 0.0);
        Order order = new Order();
        order.addItem(new Item("Item", null, 100.0));
        ticketZeroTax.addOrder(order);
        assertEquals(100.0, ticketZeroTax.getTotal(), 0.001);

        Ticket ticketHighTax = new Ticket(2, 0.25);
        ticketHighTax.addOrder(order);
        assertEquals(125.0, ticketHighTax.getTotal(), 0.001);
    }
}
