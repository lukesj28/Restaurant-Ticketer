package com.ticketer.models;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "ticketCount", "orderCount", "subtotal", "total", "tally", "tickets" })
public class DailyTicketLog {
    private Map<String, Integer> tally;
    private List<Ticket> tickets;
    private long subtotal;
    private long total;
    private int ticketCount;
    private int orderCount;

    public DailyTicketLog() {
    }

    public DailyTicketLog(Map<String, Integer> tally, List<Ticket> tickets, long subtotal, long total, int ticketCount,
            int orderCount) {
        this.tally = tally;
        this.tickets = tickets;
        this.subtotal = subtotal;
        this.total = total;
        this.ticketCount = ticketCount;
        this.orderCount = orderCount;
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

    public long getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(long subtotal) {
        this.subtotal = subtotal;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(int ticketCount) {
        this.ticketCount = ticketCount;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }
}
