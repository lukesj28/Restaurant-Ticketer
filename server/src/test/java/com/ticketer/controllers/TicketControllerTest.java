package com.ticketer.controllers;

import com.ticketer.models.Item;
import com.ticketer.models.Order;
import com.ticketer.models.Ticket;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class TicketControllerTest {

    private TicketController ticketController;

    private static final String TEST_TICKETS_DIR = "target/test-tickets";

    @Before
    public void setUp() throws IOException {
        ticketController = new TicketController();
        ticketController.resetTicketCounter();

        File testDir = new File(TEST_TICKETS_DIR);
        if (!testDir.exists()) {
            testDir.mkdirs();
        }
        System.setProperty("tickets.dir", TEST_TICKETS_DIR);
    }

    @org.junit.After
    public void tearDown() {
        File testDir = new File(TEST_TICKETS_DIR);
        if (testDir.exists()) {
            File[] files = testDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            testDir.delete();
        }
        System.clearProperty("tickets.dir");
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

    @Test(expected = com.ticketer.exceptions.EntityNotFoundException.class)
    public void testMoveToCompletedInvalid() {
        ticketController.moveToCompleted(999);
    }

    @Test(expected = com.ticketer.exceptions.EntityNotFoundException.class)
    public void testMoveToClosedInvalid() {
        ticketController.moveToClosed(999);
    }

    @Test(expected = com.ticketer.exceptions.EntityNotFoundException.class)
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

    @Test(expected = com.ticketer.exceptions.EntityNotFoundException.class)
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

    @Test(expected = com.ticketer.exceptions.EntityNotFoundException.class)
    public void testAddOrderToTicketNotFound() {
        ticketController.addOrderToTicket(999, ticketController.createOrder(0));
    }

    @Test(expected = com.ticketer.exceptions.EntityNotFoundException.class)
    public void testRemoveOrderFromTicketNotFoundTicket() {
        ticketController.removeOrderFromTicket(999, ticketController.createOrder(0));
    }

    @Test(expected = com.ticketer.exceptions.EntityNotFoundException.class)
    public void testRemoveOrderFromTicketNotFoundOrder() {
        Ticket t = ticketController.createTicket("T1");
        ticketController.removeOrderFromTicket(t.getId(), ticketController.createOrder(0));
    }

    @Test(expected = com.ticketer.exceptions.EntityNotFoundException.class)
    public void testRemoveItemFromOrderNotFound() {
        Order order = ticketController.createOrder(0.0);
        Item item = new Item("None", null, 0);
        ticketController.removeItemFromOrder(order, item);
    }

    @Test
    public void testMoveAllToClosed() {
        ticketController.createTicket("T1");
        Ticket t2 = ticketController.createTicket("T2");
        ticketController.moveToCompleted(t2.getId());

        ticketController.moveAllToClosed();

        assertTrue(ticketController.getActiveTickets().isEmpty());
        assertTrue(ticketController.getCompletedTickets().isEmpty());
        assertEquals(2, ticketController.getClosedTickets().size());
    }

    @Test
    public void testClearAllTickets() {
        Ticket t1 = ticketController.createTicket("T1");
        ticketController.moveToCompleted(t1.getId());
        ticketController.moveToClosed(t1.getId());

        ticketController.createTicket("T2");

        ticketController.clearAllTickets();

        assertTrue(ticketController.getActiveTickets().isEmpty());
        assertTrue(ticketController.getCompletedTickets().isEmpty());
        assertTrue(ticketController.getClosedTickets().isEmpty());

        assertEquals(1, ticketController.createTicket("T3").getId());
    }

    @Test
    public void testSerializeClosedTickets() throws IOException {
        Ticket t1 = ticketController.createTicket("T1");
        ticketController.moveToCompleted(t1.getId());
        ticketController.moveToClosed(t1.getId());

        ticketController.serializeClosedTickets();

        String date = new java.text.SimpleDateFormat("ddMMyyyy").format(new java.util.Date());
        String filename = TEST_TICKETS_DIR + "/" + date + ".json";
        File file = new File(filename);

        assertTrue("Ticket file should exist at " + filename, file.exists());

        String content = new String(Files.readAllBytes(file.toPath()));
        assertTrue(content.contains("T1"));
    }

    @Test
    public void testAddOrderToCompletedTicket() {
        Ticket ticket = ticketController.createTicket("T1");
        int id = ticket.getId();
        ticketController.moveToCompleted(id);

        Order order = ticketController.createOrder(0.1);
        ticketController.addOrderToTicket(id, order);

        assertTrue(ticketController.getActiveTickets().contains(ticket));
        assertFalse(ticketController.getCompletedTickets().contains(ticket));
        assertEquals(1, ticket.getOrders().size());
    }

    @Test(expected = com.ticketer.exceptions.InvalidStateException.class)
    public void testAddOrderToClosedTicket() {
        Ticket ticket = ticketController.createTicket("T2");
        int id = ticket.getId();
        ticketController.moveToCompleted(id);
        ticketController.moveToClosed(id);

        Order order = ticketController.createOrder(0.1);
        ticketController.addOrderToTicket(id, order);
    }
}
