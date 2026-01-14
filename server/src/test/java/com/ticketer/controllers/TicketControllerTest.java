package com.ticketer.controllers;

import com.ticketer.models.Item;
import com.ticketer.models.Order;
import com.ticketer.models.Ticket;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class TicketControllerTest {

    private TicketController ticketController;

    @Before
    public void setUp() throws IOException {
        ticketController = new TicketController();
        ticketController.resetTicketCounter();
    }

    @Test
    public void testCreateTicketAndIdIncrement() {
        Ticket t1 = ticketController.createTicket("Table 1");
        assertEquals(1, t1.getId());
        assertEquals("Table 1", t1.getTableNumber());

        Ticket t2 = ticketController.createTicket("Table 2");
        assertEquals(2, t2.getId());

        ticketController.resetTicketCounter();
        Ticket t3 = ticketController.createTicket("Table 3");
        assertEquals(1, t3.getId());
    }

    @Test
    public void testGetActiveTickets() {
        assertTrue(ticketController.getActiveTickets().isEmpty());
        Ticket t1 = ticketController.createTicket("T1");
        assertEquals(1, ticketController.getActiveTickets().size());
        assertTrue(ticketController.getActiveTickets().contains(t1));
    }

    @Test
    public void testLifecycle() {
        Ticket t = ticketController.createTicket("T1");
        int id = t.getId();

        ticketController.moveToCompleted(id);
        assertFalse(ticketController.getActiveTickets().contains(t));
        assertTrue(ticketController.getCompletedTickets().contains(t));

        ticketController.moveToActive(id);
        assertTrue(ticketController.getActiveTickets().contains(t));
        assertFalse(ticketController.getCompletedTickets().contains(t));

        ticketController.moveToCompleted(id);
        ticketController.moveToClosed(id);
        assertFalse(ticketController.getCompletedTickets().contains(t));
        assertTrue(ticketController.getClosedTickets().contains(t));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMoveToCompletedInvalid() {
        ticketController.moveToCompleted(999);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMoveToClosedInvalid() {
        ticketController.moveToClosed(999);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMoveToActiveInvalid() {
        ticketController.moveToActive(999);
    }

    @Test
    public void testRemoveTicket() {
        Ticket t1 = ticketController.createTicket("T1");
        ticketController.removeTicket(t1.getId());
        assertFalse(ticketController.getActiveTickets().contains(t1));

        Ticket t2 = ticketController.createTicket("T2");
        ticketController.moveToCompleted(t2.getId());
        ticketController.removeTicket(t2.getId());
        assertFalse(ticketController.getCompletedTickets().contains(t2));

        Ticket t3 = ticketController.createTicket("T3");
        ticketController.moveToCompleted(t3.getId());
        ticketController.moveToClosed(t3.getId());
        ticketController.removeTicket(t3.getId());
        assertFalse(ticketController.getClosedTickets().contains(t3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveTicketNotFound() {
        ticketController.removeTicket(999);
    }

    @Test
    public void testCreateAndManageOrders() {
        Ticket ticket = ticketController.createTicket("T1");

        Order order = ticketController.createOrder(0.1);
        ticketController.addOrderToTicket(ticket.getId(), order);

        assertEquals(1, ticket.getOrders().size());

        Item item = new Item("Burger", "Fries", 10.0);
        ticketController.addItemToOrder(order, item);

        assertEquals(10.0, order.getSubtotal(), 0.001);
        assertEquals(11.0, order.getTotal(), 0.001);
        assertEquals(10.0, ticket.getSubtotal(), 0.001);
        assertEquals(11.0, ticket.getTotal(), 0.001);

        ticketController.removeItemFromOrder(order, item);
        assertEquals(0.0, order.getTotal(), 0.001);
        assertEquals(0.0, ticket.getTotal(), 0.001);

        ticketController.addItemToOrder(order, item);
        ticketController.removeOrderFromTicket(ticket.getId(), order);
        assertTrue(ticket.getOrders().isEmpty());
        assertEquals(0.0, ticket.getTotal(), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddOrderToTicketNotFound() {
        ticketController.addOrderToTicket(999, ticketController.createOrder(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveOrderFromTicketNotFoundTicket() {
        ticketController.removeOrderFromTicket(999, ticketController.createOrder(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveOrderFromTicketNotFoundOrder() {
        Ticket t = ticketController.createTicket("T1");
        ticketController.removeOrderFromTicket(t.getId(), ticketController.createOrder(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveItemFromOrderNotFound() {
        Order order = ticketController.createOrder(0.0);
        Item item = new Item("None", null, 0);
        ticketController.removeItemFromOrder(order, item);
    }
}
