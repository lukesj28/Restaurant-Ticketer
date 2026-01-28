package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

public class Side {
    @JsonIgnore
    public int price;
    public boolean available;

    @JsonGetter("price")
    public double getPriceDouble() {
        return price / 100.0;
    }

    @JsonSetter("price")
    public void setPriceDouble(double priceDouble) {
        this.price = (int) Math.round(priceDouble * 100);
    }
}
