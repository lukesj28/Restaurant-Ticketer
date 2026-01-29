package com.ticketer.services;

import com.ticketer.models.Order;
import com.ticketer.models.Ticket;
import com.ticketer.repositories.TicketRepository;
import com.ticketer.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketService ticketService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
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
        Ticket t1 = new Ticket(1);
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

        Order order = new Order(10.0);
        ticketService.addOrderToTicket(1, order);

        assertEquals(1, t1.getOrders().size());

        verify(ticketRepository).save(t1);
    }

    @Test
    public void testAddOrderToTicketNotFound() {
        when(ticketRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> ticketService.addOrderToTicket(1, new Order(10.0)));
    }

    @Test
    public void testAddOrderToClosedTicket() {
        Ticket t1 = new Ticket(1);
        

        when(ticketRepository.findById(1)).thenReturn(Optional.of(t1));

        assertThrows(IllegalArgumentException.class, () -> ticketService.addOrderToTicket(1, new Order(10.0)));
    }
}

