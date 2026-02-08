package com.ticketer.models;

public class MenuItemView {
    public String name;
    public long price;
    public boolean available;

    public MenuItemView(String name, long price, boolean available) {
        this.name = name;
        this.price = price;
        this.available = available;
    }

    @Override
    public String toString() {
        return String.format("%s: $%.2f [%s]", name, price / 100.0, available ? "Available" : "Out of Stock");
    }
}
