package com.ticketer.utils.menu.dto;

public class MenuItemView {
    public String name;
    public int price;
    public boolean available;
    public String category;

    public MenuItemView(String name, int price, boolean available, String category) {
        this.name = name;
        this.price = price;
        this.available = available;
        this.category = category;
    }

    @Override
    public String toString() {
        return String.format("%s: $%.2f [%s]", name, price / 100.0, available ? "Available" : "Out of Stock");
    }
}
