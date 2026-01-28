package com.ticketer.models;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MenuItem {
    public String name;
    @JsonIgnore
    public int basePrice;
    public boolean available;
    @JsonProperty("sides")
    public Map<String, Side> sideOptions;

    @JsonCreator
    public MenuItem(@JsonProperty("name") String name,
            @JsonProperty("price") double priceDouble,
            @JsonProperty("available") boolean available,
            @JsonProperty("sides") Map<String, Side> sideOptions) {
        this.name = name;
        this.basePrice = (int) Math.round(priceDouble * 100);
        this.available = available;
        this.sideOptions = sideOptions;
    }

    public MenuItem(String name, int basePrice, boolean available, Map<String, Side> sideOptions) {
        this.name = name;
        this.basePrice = basePrice;
        this.available = available;
        this.sideOptions = sideOptions;
    }

    public boolean hasSides() {
        return sideOptions != null && !sideOptions.isEmpty();
    }

    @JsonGetter("price")
    public double getPriceDouble() {
        return basePrice / 100.0;
    }
}
