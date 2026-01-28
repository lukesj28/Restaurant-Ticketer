package com.ticketer.services;

import com.ticketer.models.Ticket;
import com.ticketer.models.Order;
import com.ticketer.models.OrderItem;
import com.ticketer.repositories.TicketRepository;
import com.ticketer.exceptions.EntityNotFoundException;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private int ticketIdCounter = 0;

    @Autowired
    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
        initializeTicketCounter();
    }

    private void initializeTicketCounter() {
        int maxId = 0;
        for (Ticket t : ticketRepository.findAllActive()) {
            if (t.getId() > maxId)
                maxId = t.getId();
        }
        for (Ticket t : ticketRepository.findAllCompleted()) {
            if (t.getId() > maxId)
                maxId = t.getId();
        }
        this.ticketIdCounter = maxId;
    }

    public Ticket createTicket(String tableNumber) {
        int id = generateTicketId();
        Ticket ticket = new Ticket(id);
        ticket.setTableNumber(tableNumber);
        return ticketRepository.save(ticket);
    }

    private synchronized int generateTicketId() {
        return ++ticketIdCounter;
    }

    public void resetTicketCounter() {
        this.ticketIdCounter = 0;
    }

    public Ticket getTicket(int ticketId) {
        return ticketRepository.findById(ticketId).orElse(null);
    }

    public void addOrderToTicket(int ticketId, Order order) {
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            throw new EntityNotFoundException("Ticket with ID " + ticketId + " not found.");
        }

        if (ticketRepository.findAllClosed().contains(ticket)) {
            throw new IllegalArgumentException("Cannot modify a closed ticket.");
        }

        if (ticketRepository.findAllCompleted().contains(ticket)) {
            ticketRepository.moveToActive(ticket.getId());
        }

        ticket.addOrder(order);
        ticketRepository.save(ticket);
    }

    public void addItemToOrder(int ticketId, int orderIndex, OrderItem item) {
        Ticket ticket = getTicket(ticketId);
        if (ticket == null) {
            throw new EntityNotFoundException("Ticket " + ticketId + " not found");
        }
        if (orderIndex < 0 || orderIndex >= ticket.getOrders().size()) {
            throw new EntityNotFoundException("Order index " + orderIndex + " invalid");
        }
        ticket.getOrders().get(orderIndex).addItem(item);
        ticketRepository.save(ticket);
    }

    public void removeItemFromOrder(int ticketId, int orderIndex, OrderItem item) {
        Ticket ticket = getTicket(ticketId);
        if (ticket == null) {
            throw new EntityNotFoundException("Ticket " + ticketId + " not found");
        }
        if (orderIndex < 0 || orderIndex >= ticket.getOrders().size()) {
            throw new EntityNotFoundException("Order index " + orderIndex + " invalid");
        }
        if (!ticket.getOrders().get(orderIndex).removeItem(item)) {
            throw new EntityNotFoundException("Item not found in order " + orderIndex);
        }
        ticketRepository.save(ticket);
    }

    public void removeOrder(int ticketId, int orderIndex) {
        Ticket ticket = getTicket(ticketId);
        if (ticket == null) {
            throw new EntityNotFoundException("Ticket " + ticketId + " not found");
        }
        List<Order> orders = ticket.getOrders();
        if (orderIndex < 0 || orderIndex >= orders.size()) {
            throw new EntityNotFoundException("Order index " + orderIndex + " invalid");
        }
        ticket.removeOrder(orders.get(orderIndex));
        ticketRepository.save(ticket);
    }

    public void moveToCompleted(int ticketId) {
        if (ticketRepository.findById(ticketId).isEmpty()) {
            throw new EntityNotFoundException("Ticket with ID " + ticketId + " not found.");
        }
        ticketRepository.moveToCompleted(ticketId);
    }

    public void moveToClosed(int ticketId) {
        if (ticketRepository.findById(ticketId).isEmpty()) {
            throw new EntityNotFoundException("Ticket with ID " + ticketId + " not found.");
        }
        ticketRepository.moveToClosed(ticketId);
    }

    public void moveToActive(int ticketId) {
        if (ticketRepository.findById(ticketId).isEmpty()) {
            throw new EntityNotFoundException("Ticket with ID " + ticketId + " not found.");
        }
        ticketRepository.moveToActive(ticketId);
    }

    public void removeTicket(int ticketId) {
        if (!ticketRepository.deleteById(ticketId)) {
            throw new EntityNotFoundException("Ticket with ID " + ticketId + " not found.");
        }
    }

    public void moveAllToClosed() {
        List<Ticket> active = new java.util.ArrayList<>(ticketRepository.findAllActive());
        for (Ticket t : active) {
            ticketRepository.moveToCompleted(t.getId());
            ticketRepository.moveToClosed(t.getId());
        }

        List<Ticket> completed = new java.util.ArrayList<>(ticketRepository.findAllCompleted());
        for (Ticket t : completed) {
            ticketRepository.moveToClosed(t.getId());
        }
    }

    public void serializeClosedTickets() {
        ticketRepository.persistClosedTickets();
    }

    public void clearAllTickets() {
        ticketRepository.deleteAll();
        ticketRepository.clearRecoveryFile();
        resetTicketCounter();
    }

    public boolean areAllTicketsClosed() {
        return ticketRepository.findAllActive().isEmpty() && ticketRepository.findAllCompleted().isEmpty();
    }

    public boolean hasActiveTickets() {
        return !ticketRepository.findAllActive().isEmpty();
    }

    public List<Ticket> getActiveTickets() {
        return ticketRepository.findAllActive();
    }

    public List<Ticket> getCompletedTickets() {
        return ticketRepository.findAllCompleted();
    }

    public List<Ticket> getClosedTickets() {
        return ticketRepository.findAllClosed();
    }
}
