package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderItem {
    private String name;
    private String selectedSide;
    private int mainPrice;
    private int sidePrice;

    @JsonCreator
    public OrderItem(@JsonProperty("name") String name,
            @JsonProperty("selectedSide") String selectedSide,
            @JsonProperty("mainPrice") int mainPrice,
            @JsonProperty("sidePrice") int sidePrice) {
        this.name = name;
        this.selectedSide = selectedSide;
        this.mainPrice = mainPrice;
        this.sidePrice = sidePrice;
    }

    @Override
    public String toString() {
        if (selectedSide != null) {
            return String.format("Item: %s ($%.2f), Side: %s ($%.2f), Total: $%.2f",
                    name, mainPrice / 100.0, selectedSide, sidePrice / 100.0, (mainPrice + sidePrice) / 100.0);
        }
        return String.format("Item: %s, Total: $%.2f", name, mainPrice / 100.0);
    }

    public String getName() {
        return name;
    }

    public String getSelectedSide() {
        return selectedSide;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public int getPrice() {
        return mainPrice + sidePrice;
    }

    public int getMainPrice() {
        return mainPrice;
    }

    public int getSidePrice() {
        return sidePrice;
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
