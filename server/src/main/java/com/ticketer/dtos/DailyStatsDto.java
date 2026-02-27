package com.ticketer.dtos;

public class DailyStatsDto {
    private long totalRevenue;
    private long subtotal;
    private long totalTax;
    private int ticketCount;
    private int orderCount;
    private int averageTurnoverTimeMinutes;
    private long averageCostPerTicket;
    private long barSubtotal;
    private long barTax;
    private long barTotal;

    public DailyStatsDto(long totalRevenue, long subtotal, long totalTax, int ticketCount, int orderCount,
            int averageTurnoverTimeMinutes, long averageCostPerTicket,
            long barSubtotal, long barTax, long barTotal) {
        this.totalRevenue = totalRevenue;
        this.subtotal = subtotal;
        this.totalTax = totalTax;
        this.ticketCount = ticketCount;
        this.orderCount = orderCount;
        this.averageTurnoverTimeMinutes = averageTurnoverTimeMinutes;
        this.averageCostPerTicket = averageCostPerTicket;
        this.barSubtotal = barSubtotal;
        this.barTax = barTax;
        this.barTotal = barTotal;
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

    public long getBarSubtotal() {
        return barSubtotal;
    }

    public long getBarTax() {
        return barTax;
    }

    public long getBarTotal() {
        return barTotal;
    }
}
