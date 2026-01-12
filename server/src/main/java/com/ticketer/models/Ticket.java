package com.ticketer.models;

import java.util.ArrayList;
import java.util.List;

public class Ticket {
    private int id;
    private String tableNumber;
    private List<Order> orders;
    private double subtotal;
    private double taxRate;
    private double total;
    private long createdAt;

    public Ticket(int id, double taxRate) {
        this.id = id;
        this.taxRate = taxRate;
        this.tableNumber = "";
        this.orders = new ArrayList<>();
        this.subtotal = 0.0;
        this.total = 0.0;
        this.createdAt = System.currentTimeMillis();
    }

    public void addOrder(Order order) {
        orders.add(order);
        subtotal += order.getPrice();
        total = subtotal * (1 + taxRate);
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public int getId() {
        return id;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public List<Order> getOrders() {
        return new ArrayList<>(orders);
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getTotal() {
        return total;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
