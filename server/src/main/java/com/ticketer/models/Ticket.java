package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    private String status = "ACTIVE";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String comment;

    @com.fasterxml.jackson.annotation.JsonProperty("subtotal")
    private Long persistedSubtotal;

    @com.fasterxml.jackson.annotation.JsonProperty("total")
    private Long persistedTotal;

    @com.fasterxml.jackson.annotation.JsonProperty("tax")
    private Long persistedTax;

    @SuppressWarnings("unused")
    private Ticket() {
        this.orders = new ArrayList<>();
        this.createdAt = Instant.now();
        this.status = "ACTIVE";
    }

    public Ticket(int id) {
        this.id = id;
        this.tableNumber = "";
        this.orders = new ArrayList<>();
        this.createdAt = Instant.now();
        this.closedAt = null;
        this.status = "ACTIVE";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public long getSubtotal() {
        if (persistedSubtotal != null) {
            return persistedSubtotal;
        }
        return orders.stream().mapToLong(Order::getSubtotal).sum();
    }

    public long getTotal() {
        if (persistedTotal != null) {
            return persistedTotal;
        }
        return orders.stream().mapToLong(Order::getTotal).sum();
    }

    public long getTax() {
        if (persistedTax != null) {
            return persistedTax;
        }
        return orders.stream().mapToLong(Order::getTax).sum();
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
                if (item.getSelectedExtra() != null && !item.getSelectedExtra().isEmpty()
                        && !"none".equalsIgnoreCase(item.getSelectedExtra())) {
                    tally.merge(item.getSelectedExtra(), 1, Integer::sum);
                }
            }
        }
        return tally;
    }
}
