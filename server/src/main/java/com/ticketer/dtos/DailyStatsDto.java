package com.ticketer.dtos;

public class DailyStatsDto {
    private long totalRevenue;
    private long subtotal;
    private long totalTax;
    private int ticketCount;
    private int orderCount;
    private int averageTurnoverTimeMinutes;
    private long averageCostPerTicket;

    public DailyStatsDto(long totalRevenue, long subtotal, long totalTax, int ticketCount, int orderCount,
            int averageTurnoverTimeMinutes, long averageCostPerTicket) {
        this.totalRevenue = totalRevenue;
        this.subtotal = subtotal;
        this.totalTax = totalTax;
        this.ticketCount = ticketCount;
        this.orderCount = orderCount;
        this.averageTurnoverTimeMinutes = averageTurnoverTimeMinutes;
        this.averageCostPerTicket = averageCostPerTicket;
    }

    public long getTotalRevenue() {
        return totalRevenue;
    }

    public long getSubtotal() {
        return subtotal;
    }

    public long getTotalTax() {
        return totalTax;
    }

    public int getTicketCount() {
        return ticketCount;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public int getAverageTurnoverTimeMinutes() {
        return averageTurnoverTimeMinutes;
    }

    public long getAverageCostPerTicket() {
        return averageCostPerTicket;
    }
}
