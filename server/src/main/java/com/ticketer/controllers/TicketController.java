package com.ticketer.controllers;

import com.ticketer.models.Ticket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TicketController {

    private List<Ticket> activeTickets;
    private List<Ticket> completedTickets;
    private List<Ticket> closedTickets;

    public TicketController() throws IOException {
        this.activeTickets = new ArrayList<>();
        this.completedTickets = new ArrayList<>();
        this.closedTickets = new ArrayList<>();
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
