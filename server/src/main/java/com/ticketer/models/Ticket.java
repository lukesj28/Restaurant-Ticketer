package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Ticket {
    private int id;
    private String tableNumber;
    private List<Order> orders;
    private Instant createdAt;
    private Instant closedAt;

    @com.fasterxml.jackson.annotation.JsonProperty("subtotal")
    private Integer persistedSubtotal;

    @com.fasterxml.jackson.annotation.JsonProperty("total")
    private Integer persistedTotal;

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

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public int getSubtotal() {
        if (persistedSubtotal != null) {
            return persistedSubtotal;
        }
        return orders.stream().mapToInt(Order::getSubtotal).sum();
    }

    public int getTotal() {
        if (persistedTotal != null) {
            return persistedTotal;
        }
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

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @JsonIgnore
    public java.util.Map<String, Integer> getTally() {
        java.util.Map<String, Integer> tally = new java.util.HashMap<>();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                if (!"none".equalsIgnoreCase(item.getName())) {
                    tally.merge(item.getName(), 1, Integer::sum);
                }
                if (item.getSelectedSide() != null && !item.getSelectedSide().isEmpty()
                        && !"none".equalsIgnoreCase(item.getSelectedSide())) {
                    tally.merge(item.getSelectedSide(), 1, Integer::sum);
                }
            }
        }
        return tally;
    }
}
