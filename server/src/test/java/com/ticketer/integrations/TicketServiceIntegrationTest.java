package com.ticketer.integrations;

import com.ticketer.models.OrderItem;
import com.ticketer.models.Order;
import com.ticketer.models.Ticket;
import com.ticketer.repositories.FileTicketRepository;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.ActionNotAllowedException;
import com.ticketer.services.TicketService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class TicketServiceIntegrationTest {

    private TicketService service;

    @TempDir
    Path tempDir;

    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @BeforeEach
    public void setUp() throws IOException {
        System.setProperty("tickets.dir", tempDir.toAbsolutePath().toString());
        System.setProperty("recovery.file", tempDir.resolve("recovery.json").toAbsolutePath().toString());

        mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

        service = new TicketService(new FileTicketRepository(mapper));
        service.clearAllTickets();
    }

    @org.junit.jupiter.api.AfterEach
    public void tearDown() {
        System.clearProperty("tickets.dir");
        System.clearProperty("recovery.file");
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

    @Test
    public void testMoveToCompletedInvalid() {
        assertThrows(EntityNotFoundException.class, () -> {
            service.moveToCompleted(999);
        });
    }

    @Test
    public void testMoveToClosedInvalid() {
        assertThrows(EntityNotFoundException.class, () -> {
            service.moveToClosed(999);
        });
    }

    @Test
    public void testMoveToActiveInvalid() {
        assertThrows(EntityNotFoundException.class, () -> {
            service.moveToActive(999);
        });
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
        assertThrows(ActionNotAllowedException.class, () -> {
            service.removeTicket(t3.getId());
        });
    }

    @Test
    public void testRemoveTicketNotFound() {
        assertThrows(EntityNotFoundException.class, () -> {
            service.removeTicket(999);
        });
    }

    @Test
    public void testCreateAndManageOrders() {
        Ticket ticket = service.createTicket("T1");

        Order order = new Order(1000);
        service.addOrderToTicket(ticket.getId(), order);

        assertEquals(1, ticket.getOrders().size());

        OrderItem item = new OrderItem("test-category", "Burger", "Fries", null, 800, 200, 0, null);
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

    @Test
    public void testAddOrderToTicketNotFound() {
        assertThrows(EntityNotFoundException.class, () -> {
            service.addOrderToTicket(999, new Order(0));
        });
    }

    @Test
    public void testRemoveOrderNotFoundTicket() {
        assertThrows(EntityNotFoundException.class, () -> {
            service.removeOrder(999, 0);
        });
    }

    @Test
    public void testRemoveOrderInvalidIndex() {
        Ticket t = service.createTicket("T1");
        assertThrows(EntityNotFoundException.class, () -> {
            service.removeOrder(t.getId(), 999);
        });
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

        Order order = new Order(1000);
        OrderItem item = new OrderItem("test-category", "Burger", "Fries", null, 800, 200, 0, null);
        order.addItem(item);
        service.addOrderToTicket(t1.getId(), order);

        service.moveToCompleted(t1.getId());
        service.moveToClosed(t1.getId());

        service.serializeClosedTickets();

        String date = java.time.LocalDate.now(java.time.ZoneId.systemDefault()).toString();
        File file = tempDir.resolve(date + ".json").toFile();

        assertTrue(file.exists(), "Ticket file should exist at " + file.getAbsolutePath());

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

        Order order = new Order(1000);
        service.addOrderToTicket(id, order);

        assertTrue(service.getActiveTickets().contains(ticket));
        assertFalse(service.getCompletedTickets().contains(ticket));
        assertEquals(1, ticket.getOrders().size());
    }

    @Test
    public void testAddOrderToClosedTicket() {
        Ticket ticket = service.createTicket("T2");
        int id = ticket.getId();
        service.moveToCompleted(id);
        service.moveToClosed(id);

        Order order = new Order(1000);
        assertThrows(ActionNotAllowedException.class, () -> {
            service.addOrderToTicket(id, order);
        });
    }

    @Test
    public void testSerializeClosedTicketsError() {
        File badDir = tempDir.resolve("bad").toFile();
        try {
            badDir.createNewFile();
        } catch (IOException e) {

        }
        System.setProperty("tickets.dir", badDir.getAbsolutePath());

        TicketService badService = new TicketService(new FileTicketRepository(mapper));
        assertThrows(RuntimeException.class, () -> {
            badService.serializeClosedTickets();
        });

        System.setProperty("tickets.dir", tempDir.toAbsolutePath().toString());
    }

    @Test
    public void testMoveAllToClosedEmpty() {
        service.clearAllTickets();
        service.moveAllToClosed();
        assertTrue(service.getClosedTickets().isEmpty());
    }

    @Test
    public void testAreAllTicketsClosed() {
        assertTrue(service.areAllTicketsClosed());

        Ticket t1 = service.createTicket("T1");
        assertFalse(service.areAllTicketsClosed());

        service.moveToCompleted(t1.getId());
        assertFalse(service.areAllTicketsClosed());

        service.moveToClosed(t1.getId());
        assertTrue(service.areAllTicketsClosed());
    }

    @Test
    public void testAddItemToOrder() {
        Ticket t = service.createTicket("T1");
        service.addOrderToTicket(t.getId(), new Order(0));

        com.ticketer.models.OrderItem item = new com.ticketer.models.OrderItem("test-category", "I", "S", null, 100, 0, 0, null);
        service.addItemToOrder(t.getId(), 0, item, null);

        assertEquals(1, t.getOrders().get(0).getItems().size());
        assertEquals(item, t.getOrders().get(0).getItems().get(0));
    }

    @Test
    public void testAddItemToOrderTicketNotFound() {
        assertThrows(EntityNotFoundException.class, () -> {
            service.addItemToOrder(999, 0, new com.ticketer.models.OrderItem("test-category", "I", "S", null, 100, 0, 0, null), null);
        });
    }

    @Test
    public void testAddItemToOrderInvalidIndex() {
        Ticket t = service.createTicket("T1");
        assertThrows(EntityNotFoundException.class, () -> {
            service.addItemToOrder(t.getId(), 0, new com.ticketer.models.OrderItem("test-category", "I", "S", null, 100, 0, 0, null), null);
        });
    }

    @Test
    public void testRemoveItemFromOrder() {
        Ticket t = service.createTicket("T1");
        Order o = new Order(0);
        com.ticketer.models.OrderItem item = new com.ticketer.models.OrderItem("test-category", "I", "S", null, 100, 0, 0, null);
        o.addItem(item);
        service.addOrderToTicket(t.getId(), o);

        service.removeItemFromOrder(t.getId(), 0, item);
        assertTrue(t.getOrders().get(0).getItems().isEmpty());
    }

    @Test
    public void testRemoveItemFromOrderTicketNotFound() {
        assertThrows(EntityNotFoundException.class, () -> {
            service.removeItemFromOrder(999, 0, new com.ticketer.models.OrderItem("test-category", "I", "S", null, 100, 0, 0, null));
        });
    }

    @Test
    public void testRemoveItemFromOrderInvalidIndex() {
        Ticket t = service.createTicket("T1");
        assertThrows(EntityNotFoundException.class, () -> {
            service.removeItemFromOrder(t.getId(), 0, new com.ticketer.models.OrderItem("test-category", "I", "S", null, 100, 0, 0, null));
        });
    }

    @Test
    public void testRemoveItemFromOrderNotFound() {
        Ticket t = service.createTicket("T1");
        service.addOrderToTicket(t.getId(), new Order(0));
        assertThrows(EntityNotFoundException.class, () -> {
            service.removeItemFromOrder(t.getId(), 0, new com.ticketer.models.OrderItem("test-category", "I", "S", null, 100, 0, 0, null));
        });
    }
}
