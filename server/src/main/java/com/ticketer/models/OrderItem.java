package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderItem {
    private String name;
    private String selectedSide;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String selectedExtra;
    private long mainPrice;
    private long sidePrice;
    private long extraPrice;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String comment;

    @JsonCreator
    public OrderItem(@JsonProperty("name") String name,
            @JsonProperty("selectedSide") String selectedSide,
            @JsonProperty("selectedExtra") String selectedExtra,
            @JsonProperty("mainPrice") long mainPrice,
            @JsonProperty("sidePrice") long sidePrice,
            @JsonProperty("extraPrice") long extraPrice,
            @JsonProperty("comment") String comment) {
        this.name = name;
        this.selectedSide = selectedSide;
        this.selectedExtra = selectedExtra;
        this.mainPrice = mainPrice;
        this.sidePrice = sidePrice;
        this.extraPrice = extraPrice;
        this.comment = comment;
    }

    @Override
    public String toString() {
        if (selectedSide != null) {
            return String.format("Item: %s ($%.2f), Side: %s ($%.2f), Total: $%.2f",
                    name, mainPrice / 100.0, selectedSide, sidePrice / 100.0, (mainPrice + sidePrice + extraPrice) / 100.0);
        }
        return String.format("Item: %s, Total: $%.2f", name, mainPrice / 100.0);
    }

    public String getName() {
        return name;
    }

    public String getSelectedSide() {
        return selectedSide;
    }

    public String getSelectedExtra() {
        return selectedExtra;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public long getPrice() {
        return mainPrice + sidePrice + extraPrice;
    }

    public long getMainPrice() {
        return mainPrice;
    }

    public long getSidePrice() {
        return sidePrice;
    }

    public long getExtraPrice() {
        return extraPrice;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OrderItem orderItem = (OrderItem) o;
        return java.util.Objects.equals(name, orderItem.name) &&
                java.util.Objects.equals(selectedSide, orderItem.selectedSide) &&
                java.util.Objects.equals(selectedExtra, orderItem.selectedExtra);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, selectedSide, selectedExtra);
    }

}
