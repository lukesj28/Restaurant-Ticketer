package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {
    private List<OrderItem> items;
    private long subtotal;
    private long total;
    private long tax;
    private int taxRate;

    public Order(int taxRate) {
        this.items = new ArrayList<>();
        this.subtotal = 0;
        this.total = 0;
        this.tax = 0;
        this.taxRate = taxRate;
    }

    public Order() {
        this(0);
    }

    public void addItem(OrderItem item) {
        items.add(item);
        subtotal += item.getPrice();
        updateTotal();
    }

    public boolean removeItem(OrderItem item) {
        int index = items.indexOf(item);
        if (index >= 0) {
            OrderItem actualItem = items.get(index);
            items.remove(index);
            subtotal -= actualItem.getPrice();
            updateTotal();
            return true;
        }
        return false;
    }

    private void updateTotal() {
        this.tax = (subtotal * taxRate + 5000) / 10000;
        total = subtotal + tax;
    }

    public void setTaxRate(int taxRate) {
        this.taxRate = taxRate;
        updateTotal();
    }

    public int getTaxRate() {
        return taxRate;
    }

    public List<OrderItem> getItems() {
        return new ArrayList<>(items);
    }

    public long getSubtotal() {
        return subtotal;
    }

    public long getTotal() {
        return total;
    }

    public long getTax() {
        return tax;
    }

    @JsonSetter("items")
    public void setItems(List<OrderItem> newItems) {
        this.items = new ArrayList<>();
        this.subtotal = 0;
        this.total = 0;
        this.tax = 0;
        if (newItems != null) {
            for (OrderItem item : newItems) {
                addItem(item);
            }
        }
    }
}
