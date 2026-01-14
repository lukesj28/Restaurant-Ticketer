package com.ticketer.controllers;

import com.ticketer.models.Ticket;
import com.ticketer.models.Order;
import com.ticketer.models.Item;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TicketController {

    private List<Ticket> activeTickets;
    private List<Ticket> completedTickets;
    private List<Ticket> closedTickets;
    private int ticketIdCounter;

    public TicketController() throws IOException {
        this.activeTickets = new ArrayList<>();
        this.completedTickets = new ArrayList<>();
        this.closedTickets = new ArrayList<>();
        this.ticketIdCounter = 1;
    }

    public void resetTicketCounter() {
        this.ticketIdCounter = 1;
    }

    public Ticket createTicket(String tableNumber) {
        Ticket ticket = new Ticket(ticketIdCounter++);
        ticket.setTableNumber(tableNumber);
        activeTickets.add(ticket);
        return ticket;
    }

    public Ticket getTicket(int ticketId) {
        return activeTickets.stream()
                .filter(t -> t.getId() == ticketId)
                .findFirst()
                .orElse(null);
    }

    public void addOrderToTicket(int ticketId, Order order) {
        Ticket ticket = getTicket(ticketId);
        if (ticket != null) {
            ticket.addOrder(order);
        } else {
            throw new IllegalArgumentException("Active ticket with ID " + ticketId + " not found.");
        }
    }

    public void removeOrderFromTicket(int ticketId, Order order) {
        Ticket ticket = getTicket(ticketId);
        if (ticket != null) {
            if (!ticket.removeOrder(order)) {
                throw new IllegalArgumentException("Order not found in ticket " + ticketId);
            }
        } else {
            throw new IllegalArgumentException("Active ticket with ID " + ticketId + " not found.");
        }
    }

    public Order createOrder(double taxRate) {
        return new Order(taxRate);
    }

    public void addItemToOrder(Order order, Item item) {
        order.addItem(item);
    }

    public void removeItemFromOrder(Order order, Item item) {
        if (!order.removeItem(item)) {
            throw new IllegalArgumentException("Item not found in order");
        }
    }

    public List<Ticket> getActiveTickets() {
        return activeTickets;
    }

    public List<Ticket> getCompletedTickets() {
        return completedTickets;
    }

    public List<Ticket> getClosedTickets() {
        return closedTickets;
    }

    public void moveToCompleted(int ticketId) {
        Ticket ticket = activeTickets.stream()
                .filter(t -> t.getId() == ticketId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ticket with ID " + ticketId + " not found in active tickets."));

        activeTickets.remove(ticket);
        completedTickets.add(ticket);
    }

    public void moveToClosed(int ticketId) {
        Ticket ticket = completedTickets.stream()
                .filter(t -> t.getId() == ticketId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ticket with ID " + ticketId + " not found in completed tickets."));

        completedTickets.remove(ticket);
        closedTickets.add(ticket);
    }

    public void moveToActive(int ticketId) {
        Ticket ticket = completedTickets.stream()
                .filter(t -> t.getId() == ticketId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ticket with ID " + ticketId + " not found in completed tickets."));

        completedTickets.remove(ticket);
        activeTickets.add(ticket);
    }

    public void removeTicket(int ticketId) {
        if (activeTickets.removeIf(t -> t.getId() == ticketId))
            return;
        if (completedTickets.removeIf(t -> t.getId() == ticketId))
            return;
        if (closedTickets.removeIf(t -> t.getId() == ticketId))
            return;

        throw new IllegalArgumentException("Ticket with ID " + ticketId + " not found in any list.");
    }
}
