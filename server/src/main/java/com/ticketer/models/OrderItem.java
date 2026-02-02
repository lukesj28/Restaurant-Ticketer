package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderItem {
    private String name;
    private String selectedSide;
    private int price;

    @JsonCreator
    public OrderItem(@JsonProperty("name") String name,
            @JsonProperty("selectedSide") String selectedSide,
            @JsonProperty("price") int price) {
        this.name = name;
        this.selectedSide = selectedSide;
        this.price = price;
    }

    @Override
    public String toString() {
        if (selectedSide != null) {
            return String.format("Item: %s, Side: %s, Total: $%.2f", name, selectedSide, price / 100.0);
        }
        return String.format("Item: %s, Total: $%.2f", name, price / 100.0);
    }

    public String getName() {
        return name;
    }

    public String getSelectedSide() {
        return selectedSide;
    }

    public int getPrice() {
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OrderItem orderItem = (OrderItem) o;
        return java.util.Objects.equals(name, orderItem.name) &&
                java.util.Objects.equals(selectedSide, orderItem.selectedSide);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, selectedSide);
    }

}
