package com.ticketer.models;

import java.util.ArrayList;
import java.util.List;

public class Ticket {
    private int id;
    private String tableNumber;
    private List<Order> orders;
    private long createdAt;

    public Ticket(int id) {
        this.id = id;
        this.tableNumber = "";
        this.orders = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
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

    public double getSubtotal() {
        return orders.stream().mapToDouble(Order::getSubtotal).sum();
    }

    public double getTotal() {
        return orders.stream().mapToDouble(Order::getTotal).sum();
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
