package com.ticketer.services;

import com.ticketer.models.Order;
import com.ticketer.models.Ticket;
import com.ticketer.repositories.TicketRepository;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.ActionNotAllowedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private Clock clock;

    private TicketService ticketService;

    @BeforeEach
    public void setUp() {
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2023-01-01T12:00:00Z"));

        ticketService = new TicketService(ticketRepository, clock);
    }

    @Test
    public void testCreateTicket() {
        Ticket ticket = new Ticket(1);
        ticket.setTableNumber("Table 1");
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        Ticket created = ticketService.createTicket("Table 1");

        assertNotNull(created);
        assertEquals("Table 1", created.getTableNumber());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    public void testGetActiveTickets() {
        Ticket t1 = new Ticket(1);
        when(ticketRepository.findAllActive()).thenReturn(Arrays.asList(t1));

        List<Ticket> active = ticketService.getActiveTickets();
        assertEquals(1, active.size());
        assertEquals(t1, active.get(0));
    }

    @Test
    public void testMoveToCompleted() {
        Ticket t1 = new Ticket(1);
        when(ticketRepository.findById(1)).thenReturn(Optional.of(t1));

        ticketService.moveToCompleted(1);

        verify(ticketRepository).moveToCompleted(1);
    }

    @Test
    public void testMoveToCompletedNotFound() {
        when(ticketRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> ticketService.moveToCompleted(1));
    }

    @Test
    public void testMoveToClosed() {
        Ticket t1 = new Ticket(1);
        when(ticketRepository.findById(1)).thenReturn(Optional.of(t1));

        ticketService.moveToClosed(1);

        verify(ticketRepository).moveToClosed(1);
    }

    @Test
    public void testMoveToActive() {
        Ticket t1 = new Ticket(1);
        when(ticketRepository.findById(1)).thenReturn(Optional.of(t1));

        ticketService.moveToActive(1);

        verify(ticketRepository).moveToActive(1);
    }

    @Test
    public void testRemoveTicket() {

        when(ticketRepository.deleteById(1)).thenReturn(true);

        ticketService.removeTicket(1);

        verify(ticketRepository).deleteById(1);
    }

    @Test
    public void testRemoveTicketNotFound() {
        when(ticketRepository.deleteById(1)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> ticketService.removeTicket(1));
    }

    @Test
    public void testAddOrderToTicket() {
        Ticket t1 = new Ticket(1);
        when(ticketRepository.findById(1)).thenReturn(Optional.of(t1));

        Order order = new Order(1000);
        ticketService.addOrderToTicket(1, order);

        assertEquals(1, t1.getOrders().size());

        verify(ticketRepository).save(t1);
    }

    @Test
    public void testAddOrderToTicketNotFound() {
        when(ticketRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> ticketService.addOrderToTicket(1, new Order(1000)));
    }

    @Test
    void testAddOrderToClosedTicket() {
        Ticket t1 = new Ticket(1);

        when(ticketRepository.findAllClosed()).thenReturn(Arrays.asList(t1));
        when(ticketRepository.findById(1)).thenReturn(Optional.of(t1));

        assertThrows(ActionNotAllowedException.class, () -> ticketService.addOrderToTicket(1, new Order(1000)));
    }

    @Test
    void testMoveClosedToCompletedShouldFail() {
        Ticket t1 = new Ticket(1);
        t1.setClosedAt(Instant.now());
        when(ticketRepository.findById(1)).thenReturn(Optional.of(t1));

        assertThrows(ActionNotAllowedException.class, () -> ticketService.moveToCompleted(1));
    }

    @Test
    void testMoveClosedToActiveShouldFail() {
        Ticket t1 = new Ticket(1);
        t1.setClosedAt(Instant.now());
        when(ticketRepository.findById(1)).thenReturn(Optional.of(t1));

        assertThrows(ActionNotAllowedException.class, () -> ticketService.moveToActive(1));
    }

    @Test
    public void testGetOrder() {
        Ticket t1 = new Ticket(1);
        Order o1 = new Order(0);
        t1.addOrder(o1);
        when(ticketRepository.findById(1)).thenReturn(Optional.of(t1));

        Order retrieved = ticketService.getOrder(1, 0);
        assertEquals(o1, retrieved);
    }

    @Test
    public void testGetOrderTicketNotFound() {
        when(ticketRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> ticketService.getOrder(1, 0));
    }

    @Test
    public void testGetOrderIndexInvalid() {
        Ticket t1 = new Ticket(1);
        when(ticketRepository.findById(1)).thenReturn(Optional.of(t1));
        assertThrows(EntityNotFoundException.class, () -> ticketService.getOrder(1, 0));
    }

    @Test
    public void testGetClosedTicketsSubtotal() {
        Ticket t1 = new Ticket(1);
        Order o1 = new Order(0);
        o1.addItem(new com.ticketer.models.OrderItem("Burger", "Fries", 1000, 0));
        t1.addOrder(o1);

        Ticket t2 = new Ticket(2);
        Order o2 = new Order(0);
        o2.addItem(new com.ticketer.models.OrderItem("Pizza", "None", 2000, 0));
        t2.addOrder(o2);

        when(ticketRepository.findAllClosed()).thenReturn(Arrays.asList(t1, t2));

        int subtotal = ticketService.getClosedTicketsSubtotal();
        assertEquals(3000, subtotal);
    }

    @Test
    public void testGetClosedTicketsTotal() {
        Ticket t1 = new Ticket(1);
        Order o1 = new Order(1000);
        o1.addItem(new com.ticketer.models.OrderItem("Burger", "Fries", 1000, 0));
        t1.addOrder(o1);

        Ticket t2 = new Ticket(2);
        Order o2 = new Order(2000);
        o2.addItem(new com.ticketer.models.OrderItem("Pizza", "None", 2000, 0));
        t2.addOrder(o2);

        when(ticketRepository.findAllClosed()).thenReturn(Arrays.asList(t1, t2));

        int total = ticketService.getClosedTicketsTotal();
        assertEquals(3500, total);
    }

    @Test
    public void testGetActiveAndCompletedTicketsSubtotal() {
        Ticket t1 = new Ticket(1);
        Order o1 = new Order(0);
        o1.addItem(new com.ticketer.models.OrderItem("Burger", "Fries", 1000, 0));
        t1.addOrder(o1);

        Ticket t2 = new Ticket(2);
        Order o2 = new Order(0);
        o2.addItem(new com.ticketer.models.OrderItem("Pizza", "None", 2000, 0));
        t2.addOrder(o2);

        when(ticketRepository.findAllActive()).thenReturn(Arrays.asList(t1));
        when(ticketRepository.findAllCompleted()).thenReturn(Arrays.asList(t2));

        int subtotal = ticketService.getActiveAndCompletedTicketsSubtotal();
        assertEquals(3000, subtotal);
    }

    @Test
    public void testGetActiveAndCompletedTicketsTotal() {
        Ticket t1 = new Ticket(1);
        Order o1 = new Order(1000);
        o1.addItem(new com.ticketer.models.OrderItem("Burger", "Fries", 1000, 0));
        t1.addOrder(o1);

        Ticket t2 = new Ticket(2);
        Order o2 = new Order(2000);
        o2.addItem(new com.ticketer.models.OrderItem("Pizza", "None", 2000, 0));
        t2.addOrder(o2);

        when(ticketRepository.findAllActive()).thenReturn(Arrays.asList(t1));
        when(ticketRepository.findAllCompleted()).thenReturn(Arrays.asList(t2));

        int total = ticketService.getActiveAndCompletedTicketsTotal();
        assertEquals(3500, total);
    }
}
