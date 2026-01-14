package com.ticketer.models;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private List<Item> items;
    private double subtotal;
    private double total;
    private double taxRate;

    public Order(double taxRate) {
        this.items = new ArrayList<>();
        this.subtotal = 0.0;
        this.total = 0.0;
        this.taxRate = taxRate;
    }

    public Order() {
        this(0.0);
    }

    public void addItem(Item item) {
        items.add(item);
        subtotal += item.getPrice();
        total = subtotal * (1 + taxRate);
    }

    public boolean removeItem(Item item) {
        if (items.remove(item)) {
            subtotal -= item.getPrice();
            total = subtotal * (1 + taxRate);
            return true;
        }
        return false;
    }

    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
        total = subtotal * (1 + taxRate);
    }

    public double getTaxRate() {
        return taxRate;
    }

    public List<Item> getItems() {
        return new ArrayList<>(items);
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getTotal() {
        return total;
    }
}
