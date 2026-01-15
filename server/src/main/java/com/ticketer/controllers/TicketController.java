package com.ticketer.controllers;

import com.ticketer.models.Ticket;
import com.ticketer.models.Order;
import com.ticketer.models.Item;
import com.ticketer.exceptions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TicketController {

    private List<Ticket> activeTickets;
    private List<Ticket> completedTickets;
    private List<Ticket> closedTickets;
    private int ticketIdCounter;

    public TicketController() {
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
            return;
        }

        ticket = completedTickets.stream()
                .filter(t -> t.getId() == ticketId)
                .findFirst()
                .orElse(null);

        if (ticket != null) {
            completedTickets.remove(ticket);
            activeTickets.add(ticket);
            ticket.addOrder(order);
            return;
        }

        if (closedTickets.stream().anyMatch(t -> t.getId() == ticketId)) {
            throw new InvalidStateException("Cannot add order to closed ticket " + ticketId);
        }

        throw new EntityNotFoundException("Ticket with ID " + ticketId + " not found.");
    }

    public void removeOrderFromTicket(int ticketId, Order order) {
        Ticket ticket = getTicket(ticketId);
        if (ticket != null) {
            if (!ticket.removeOrder(order)) {
                throw new EntityNotFoundException("Order not found in ticket " + ticketId);
            }
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
    }

    public Order createOrder(double taxRate) {
        return new Order(taxRate);
    }

    public void addItemToOrder(Order order, Item item) {
        order.addItem(item);
    }

    public void removeItemFromOrder(Order order, Item item) {
        if (!order.removeItem(item)) {
            throw new EntityNotFoundException("Item not found in order");
        }
    }

    public void removeMatchingItem(Order order, Item templateItem) {
        Item toRemove = null;
        for (Item i : order.getItems()) {
            if (i.getName().equals(templateItem.getName()) &&
                    ((i.getSelectedSide() == null && templateItem.getSelectedSide() == null)
                            || (i.getSelectedSide() != null
                                    && i.getSelectedSide().equals(templateItem.getSelectedSide())))) {
                toRemove = i;
                break;
            }
        }

        if (toRemove != null) {
            order.removeItem(toRemove);
        } else {
            throw new EntityNotFoundException("Item not found in order");
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
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ticket with ID " + ticketId + " not found in active tickets."));

        activeTickets.remove(ticket);
        completedTickets.add(ticket);
    }

    public void moveToClosed(int ticketId) {
        Ticket ticket = completedTickets.stream()
                .filter(t -> t.getId() == ticketId)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ticket with ID " + ticketId + " not found in completed tickets."));

        completedTickets.remove(ticket);
        closedTickets.add(ticket);
    }

    public void moveToActive(int ticketId) {
        Ticket ticket = completedTickets.stream()
                .filter(t -> t.getId() == ticketId)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
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

        throw new EntityNotFoundException(
                "Ticket with ID " + ticketId + " not found in any list.");
    }

    public void moveAllToClosed() {
        closedTickets.addAll(activeTickets);
        activeTickets.clear();

        closedTickets.addAll(completedTickets);
        completedTickets.clear();
    }

    public void serializeClosedTickets() {
        String date = java.time.LocalDate.now().toString();
        String dirPath = System.getProperty("tickets.dir", "data/tickets");
        String filename = dirPath + "/" + date + ".json";

        java.io.File directory = new java.io.File(dirPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (java.io.FileWriter writer = new java.io.FileWriter(filename)) {
            writer.write(com.ticketer.utils.ticket.TicketUtils.serializeTickets(closedTickets));
        } catch (IOException e) {
            throw new com.ticketer.exceptions.StorageException("Failed to serialize closed tickets", e);
        }
    }

    public void clearAllTickets() {
        activeTickets.clear();
        completedTickets.clear();
        closedTickets.clear();
        resetTicketCounter();
    }

    public boolean areAllTicketsClosed() {
        return activeTickets.isEmpty() && completedTickets.isEmpty();
    }
}
