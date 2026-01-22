package com.ticketer.services;

import com.ticketer.models.OrderItem;
import com.ticketer.models.Order;
import com.ticketer.models.Ticket;
import com.ticketer.repositories.FileTicketRepository;
import com.ticketer.exceptions.EntityNotFoundException;

import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class TicketServiceTest {

    private TicketService service;
    private static final String TEST_TICKETS_DIR = "target/test-tickets-service";

    @Before
    public void setUp() throws IOException {
        File ticketsDir = new File(TEST_TICKETS_DIR);
        if (!ticketsDir.exists()) {
            ticketsDir.mkdirs();
        }
        System.setProperty("tickets.dir", TEST_TICKETS_DIR);

        service = new TicketService(new FileTicketRepository());
        service.clearAllTickets();
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
        Ticket t1 = service.createTicket("Table 1");
        assertEquals(1, t1.getId());
        assertEquals("Table 1", t1.getTableNumber());

        Ticket t2 = service.createTicket("Table 2");
        assertEquals(2, t2.getId());

        service.clearAllTickets();
        Ticket t3 = service.createTicket("Table 3");
        assertEquals(1, t3.getId());
    }

    @Test
    public void testGetActiveTickets() {
        assertTrue(service.getActiveTickets().isEmpty());
        Ticket t1 = service.createTicket("T1");
        assertEquals(1, service.getActiveTickets().size());
        assertTrue(service.getActiveTickets().contains(t1));
    }

    @Test
    public void testLifecycle() {
        Ticket t = service.createTicket("T1");
        int id = t.getId();

        service.moveToCompleted(id);
        assertFalse(service.getActiveTickets().contains(t));
        assertTrue(service.getCompletedTickets().contains(t));

        service.moveToActive(id);
        assertTrue(service.getActiveTickets().contains(t));
        assertFalse(service.getCompletedTickets().contains(t));

        service.moveToCompleted(id);
        service.moveToClosed(id);
        assertFalse(service.getCompletedTickets().contains(t));
        assertTrue(service.getClosedTickets().contains(t));
    }

    @Test(expected = EntityNotFoundException.class)
    public void testMoveToCompletedInvalid() {
        service.moveToCompleted(999);
    }

    @Test(expected = EntityNotFoundException.class)
    public void testMoveToClosedInvalid() {
        service.moveToClosed(999);
    }

    @Test(expected = EntityNotFoundException.class)
    public void testMoveToActiveInvalid() {
        service.moveToActive(999);
    }

    @Test
    public void testRemoveTicket() {
        Ticket t1 = service.createTicket("T1");
        service.removeTicket(t1.getId());
        assertFalse(service.getActiveTickets().contains(t1));

        Ticket t2 = service.createTicket("T2");
        service.moveToCompleted(t2.getId());
        service.removeTicket(t2.getId());
        assertFalse(service.getCompletedTickets().contains(t2));

        Ticket t3 = service.createTicket("T3");
        service.moveToCompleted(t3.getId());
        service.moveToClosed(t3.getId());
        service.removeTicket(t3.getId());
        assertFalse(service.getClosedTickets().contains(t3));
    }

    @Test(expected = EntityNotFoundException.class)
    public void testRemoveTicketNotFound() {
        service.removeTicket(999);
    }

    @Test
    public void testCreateAndManageOrders() {
        Ticket ticket = service.createTicket("T1");

        Order order = new Order(0.1);
        service.addOrderToTicket(ticket.getId(), order);

        assertEquals(1, ticket.getOrders().size());

        OrderItem item = new OrderItem("Burger", "Fries", 1000);
        order.addItem(item);

        assertEquals(1000, order.getSubtotal());
        assertEquals(1100, order.getTotal());
        assertEquals(1000, ticket.getSubtotal());
        assertEquals(1100, ticket.getTotal());

        assertEquals(1100, ticket.getTotal());

        service.removeOrder(ticket.getId(), 0);
        assertTrue(ticket.getOrders().isEmpty());
        assertEquals(0, ticket.getTotal());
    }

    @Test(expected = EntityNotFoundException.class)
    public void testAddOrderToTicketNotFound() {
        service.addOrderToTicket(999, new Order(0));
    }

    @Test(expected = EntityNotFoundException.class)
    public void testRemoveOrderNotFoundTicket() {
        service.removeOrder(999, 0);
    }

    @Test(expected = EntityNotFoundException.class)
    public void testRemoveOrderInvalidIndex() {
        Ticket t = service.createTicket("T1");
        service.removeOrder(t.getId(), 999);
    }

    @Test
    public void testMoveAllToClosed() {
        service.createTicket("T1");
        Ticket t2 = service.createTicket("T2");
        service.moveToCompleted(t2.getId());

        service.moveAllToClosed();

        assertTrue(service.getActiveTickets().isEmpty());
        assertTrue(service.getCompletedTickets().isEmpty());
        assertEquals(2, service.getClosedTickets().size());
    }

    @Test
    public void testSerializeClosedTickets() throws IOException {
        Ticket t1 = service.createTicket("T1");

        Order order = new Order(0.1);
        OrderItem item = new OrderItem("Burger", "Fries", 1000);
        order.addItem(item);
        service.addOrderToTicket(t1.getId(), order);

        service.moveToCompleted(t1.getId());
        service.moveToClosed(t1.getId());

        service.serializeClosedTickets();

        String date = java.time.LocalDate.now().toString();
        String filename = TEST_TICKETS_DIR + "/" + date + ".json";
        File file = new File(filename);

        assertTrue("Ticket file should exist at " + filename, file.exists());

        String content = new String(Files.readAllBytes(file.toPath()));
        assertTrue(content.contains("T1"));
        assertTrue(content.contains("Burger"));
        assertTrue(content.contains("Fries"));
    }

    @Test
    public void testAddOrderToCompletedTicket() {
        Ticket ticket = service.createTicket("T1");
        int id = ticket.getId();
        service.moveToCompleted(id);

        Order order = new Order(0.1);
        service.addOrderToTicket(id, order);

        assertTrue(service.getActiveTickets().contains(ticket));
        assertFalse(service.getCompletedTickets().contains(ticket));
        assertEquals(1, ticket.getOrders().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddOrderToClosedTicket() {
        Ticket ticket = service.createTicket("T2");
        int id = ticket.getId();
        service.moveToCompleted(id);
        service.moveToClosed(id);

        Order order = new Order(0.1);
        service.addOrderToTicket(id, order);
    }

    @Test(expected = RuntimeException.class)
    public void testSerializeClosedTicketsError() {
        File badDir = new File(TEST_TICKETS_DIR, "bad");
        try {
            badDir.createNewFile();
        } catch (IOException e) {

        }
        System.setProperty("tickets.dir", badDir.getAbsolutePath());

        TicketService badService = new TicketService(new FileTicketRepository());
        badService.serializeClosedTickets();
    }
}
