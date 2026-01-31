package com.ticketer.models;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "subtotal", "total", "tally", "tickets" })
public class DailyTicketLog {
    private Map<String, Integer> tally;
    private List<Ticket> tickets;
    private double subtotal;
    private double total;

    public DailyTicketLog() {
    }

    public DailyTicketLog(Map<String, Integer> tally, List<Ticket> tickets, double subtotal, double total) {
        this.tally = tally;
        this.tickets = tickets;
        this.subtotal = subtotal;
        this.total = total;
    }

    public Map<String, Integer> getTally() {
        return tally;
    }

    public void setTally(Map<String, Integer> tally) {
        this.tally = tally;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
