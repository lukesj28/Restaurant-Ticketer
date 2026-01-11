package com.ticketer.utils.menu.dto;

public class MenuItemView {
    public String name;
    public double price;
    public boolean available;
    public String category;

    public MenuItemView(String name, double price, boolean available, String category) {
        this.name = name;
        this.price = price;
        this.available = available;
        this.category = category;
    }

    @Override
    public String toString() {
        return String.format("%s: $%.2f [%s]", name, price, available ? "Available" : "Out of Stock");
    }
}
