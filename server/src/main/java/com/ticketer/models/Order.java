package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String comment;

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

    public OrderItem removeItemByIndex(int index) {
        if (index < 0 || index >= items.size()) {
            return null;
        }
        OrderItem removed = items.remove(index);
        subtotal -= removed.getPrice();
        updateTotal();
        return removed;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
