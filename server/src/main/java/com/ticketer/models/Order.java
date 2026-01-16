package com.ticketer.models;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private List<OrderItem> items;
    private int subtotal;
    private int total;
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
}
