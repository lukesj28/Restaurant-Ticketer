package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Ticket {
    private int id;
    private String tableNumber;
    private List<Order> orders;
    private Instant createdAt;
    private Instant closedAt;

    @SuppressWarnings("unused")
    private Ticket() {
        this.orders = new ArrayList<>();
        this.createdAt = Instant.now();
    }

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

    @JsonIgnore
    public int getSubtotal() {
        return orders.stream().mapToInt(Order::getSubtotal).sum();
    }

    @JsonGetter("subtotal")
    public double getSubtotalDouble() {
        return getSubtotal() / 100.0;
    }

    @JsonIgnore
    public int getTotal() {
        return orders.stream().mapToInt(Order::getTotal).sum();
    }

    @JsonGetter("total")
    public double getTotalDouble() {
        return getTotal() / 100.0;
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

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @JsonIgnore
    public java.util.Map<String, Integer> getTally() {
        java.util.Map<String, Integer> tally = new java.util.HashMap<>();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                tally.merge(item.getName(), 1, Integer::sum);
                if (item.getSelectedSide() != null && !item.getSelectedSide().isEmpty()) {
                    tally.merge(item.getSelectedSide(), 1, Integer::sum);
                }
            }
        }
        return tally;
    }
}
