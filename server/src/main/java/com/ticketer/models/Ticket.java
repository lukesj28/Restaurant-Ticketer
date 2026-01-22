package com.ticketer.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Ticket {
    private int id;
    private String tableNumber;
    private List<Order> orders;
    private Instant createdAt;
    private Instant closedAt;

    public Ticket(int id) {
        this.id = id;
        this.tableNumber = "";
        this.orders = new ArrayList<>();
        this.createdAt = Instant.now();
        this.closedAt = null;
    }

    public void addOrder(Order order) {
        orders.add(order);
    }

    public boolean removeOrder(Order order) {
        return orders.remove(order);
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

    public int getSubtotal() {
        return orders.stream().mapToInt(Order::getSubtotal).sum();
    }

    public int getTotal() {
        return orders.stream().mapToInt(Order::getTotal).sum();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }
}
