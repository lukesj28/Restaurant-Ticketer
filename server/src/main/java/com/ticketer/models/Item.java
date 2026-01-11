package com.ticketer.models;

public class Item {
    public String name;
    public String selectedSide;
    public double totalPrice;

    public Item(String name, String selectedSide, double totalPrice) {
        this.name = name;
        this.selectedSide = selectedSide;
        this.totalPrice = totalPrice;
    }

    @Override
    public String toString() {
        if (selectedSide != null) {
            return String.format("Item: %s, Side: %s, Total: $%.2f", name, selectedSide, totalPrice);
        }
        return String.format("Item: %s, Total: $%.2f", name, totalPrice);
    }
}
