package com.ticketer.models;

public class Item {
    private String name;
    private String selectedSide;
    private double price;

    public Item(String name, String selectedSide, double price) {
        this.name = name;
        this.selectedSide = selectedSide;
        this.price = price;
    }

    @Override
    public String toString() {
        if (selectedSide != null) {
            return String.format("Item: %s, Side: %s, Total: $%.2f", name, selectedSide, price);
        }
        return String.format("Item: %s, Total: $%.2f", name, price);
    }

    public String getName() {
        return name;
    }

    public String getSelectedSide() {
        return selectedSide;
    }

    public double getPrice() {
        return price;
    }

}
