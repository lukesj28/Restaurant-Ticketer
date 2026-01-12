package com.ticketer.models;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private List<Item> items;
    private double price;

    public Order() {
        this.items = new ArrayList<>();
        this.price = 0.0;
    }

    public void addItem(Item item) {
        items.add(item);
        price += item.getPrice();
    }

    public List<Item> getItems() {
        return new ArrayList<>(items);
    }

    public double getPrice() {
        return price;
    }
}
