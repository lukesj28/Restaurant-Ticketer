package com.ticketer.models;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MenuItem {
    public String name;
    public int price;
    public boolean available;
    @JsonProperty("sides")
    public Map<String, Side> sideOptions;
    @JsonProperty("sideOrder")
    public List<String> sideOrder;

    @JsonCreator
    public MenuItem(@JsonProperty("name") String name,
            @JsonProperty("price") int price,
            @JsonProperty("available") boolean available,
            @JsonProperty("sides") Map<String, Side> sideOptions,
            @JsonProperty("sideOrder") List<String> sideOrder) {
        this.name = name;
        this.price = price;
        this.available = available;
        this.sideOptions = sideOptions;
        this.sideOrder = sideOrder != null ? sideOrder
                : (sideOptions != null ? new java.util.ArrayList<>(sideOptions.keySet()) : new java.util.ArrayList<>());
    }

    public boolean hasSides() {
        return sideOptions != null && !sideOptions.isEmpty();
    }

    public List<String> getSideOrder() {
        return sideOrder;
    }

    public void setSideOrder(List<String> sideOrder) {
        this.sideOrder = sideOrder;
    }
}
