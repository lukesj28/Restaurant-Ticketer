package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private List<OrderItem> items;
    @JsonIgnore
    private int subtotal;
    @JsonIgnore
    private int total;
    @JsonIgnore
    private double taxRate;

    public Order(double taxRate) {
        this.items = new ArrayList<>();
        this.subtotal = 0;
        this.total = 0;
        this.taxRate = taxRate;
    }

    public Order() {
        this(0.0);
    }

    public void addItem(OrderItem item) {
        items.add(item);
        subtotal += item.getPrice();
        total = (int) Math.round(subtotal * (1 + taxRate));
    }

    public boolean removeItem(OrderItem item) {
        if (items.remove(item)) {
            subtotal -= item.getPrice();
            total = (int) Math.round(subtotal * (1 + taxRate));
            return true;
        }
        return false;
    }

    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
        total = (int) Math.round(subtotal * (1 + taxRate));
    }

    public double getTaxRate() {
        return taxRate;
    }

    public List<OrderItem> getItems() {
        return new ArrayList<>(items);
    }

    public int getSubtotal() {
        return subtotal;
    }

    public int getTotal() {
        return total;
    }

    @JsonGetter("subtotal")
    public double getSubtotalDouble() {
        return subtotal / 100.0;
    }

    @JsonGetter("total")
    public double getTotalDouble() {
        return total / 100.0;
    }

    @JsonSetter("items")
    public void setItems(List<OrderItem> newItems) {
        this.items = new ArrayList<>();
        this.subtotal = 0;
        this.total = 0;
        if (newItems != null) {
            for (OrderItem item : newItems) {
                addItem(item);
            }
        }
    }
}
