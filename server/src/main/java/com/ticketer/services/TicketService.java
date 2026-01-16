package com.ticketer.services;

import com.ticketer.models.Ticket;
import com.ticketer.models.Order;
import com.ticketer.repositories.TicketRepository;
import com.ticketer.exceptions.EntityNotFoundException;

import java.util.List;

public class TicketService {

    private final TicketRepository ticketRepository;
    private int ticketIdCounter = 0;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
        ticketIdCounter = 0;
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

    public void removeOrderFromTicket(int ticketId, Order order) {
        Ticket ticket = getTicket(ticketId);
        if (ticket != null) {
            if (!ticket.removeOrder(order)) {
                throw new EntityNotFoundException("Order not found in ticket " + ticketId);
            }
            ticketRepository.save(ticket);
        } else {
            throw new EntityNotFoundException("Active ticket with ID " + ticketId + " not found.");
        }
    }

    public void removeMatchingOrder(int ticketId, Order templateOrder) {
        Ticket ticket = getTicket(ticketId);
        if (ticket == null) {
            throw new EntityNotFoundException("Active ticket with ID " + ticketId + " not found.");
        }

        Order orderToRemove = null;
        for (Order o : ticket.getOrders()) {
            if (o.getTotal() == templateOrder.getTotal() && o.getItems().size() == templateOrder.getItems().size()) {
                orderToRemove = o;
                break;
            }
        }

        if (orderToRemove == null) {
            throw new EntityNotFoundException("Order not found in ticket " + ticketId);
        }

        ticket.removeOrder(orderToRemove);
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
        resetTicketCounter();
    }

    public boolean areAllTicketsClosed() {
        return ticketRepository.findAllActive().isEmpty() && ticketRepository.findAllCompleted().isEmpty();
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
