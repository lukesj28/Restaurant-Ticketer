package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {
    private List<OrderItem> items;
    @JsonIgnore
    private long subtotal;
    @JsonIgnore
    private long total;
    @JsonIgnore
    private int taxRate;

    public Order(int taxRate) {
        this.items = new ArrayList<>();
        this.subtotal = 0;
        this.total = 0;
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
        long taxAmount = (subtotal * taxRate + 5000) / 10000;
        total = subtotal + taxAmount;
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
