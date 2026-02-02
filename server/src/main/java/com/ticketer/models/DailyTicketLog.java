package com.ticketer.models;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "subtotal", "total", "tally", "tickets" })
public class DailyTicketLog {
    private Map<String, Integer> tally;
    private List<Ticket> tickets;
    private int subtotal;
    private int total;

    public DailyTicketLog() {
    }

    public DailyTicketLog(Map<String, Integer> tally, List<Ticket> tickets, int subtotal, int total) {
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

    public int getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(int subtotal) {
        this.subtotal = subtotal;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
