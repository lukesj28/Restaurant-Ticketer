package com.ticketer.models;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MenuItem {
    public String name;
    public long price;
    public boolean available;
    public boolean kitchen;
    @JsonProperty("sides")
    public Map<String, Side> sideOptions;
    @JsonProperty("sideOrder")
    public List<String> sideOrder;
    @JsonProperty("extras")
    public Map<String, Extra> extraOptions;
    @JsonProperty("extraOrder")
    public List<String> extraOrder;

    @JsonCreator
    public MenuItem(@JsonProperty("name") String name,
            @JsonProperty("price") long price,
            @JsonProperty("available") boolean available,
            @JsonProperty("sides") Map<String, Side> sideOptions,
            @JsonProperty("sideOrder") List<String> sideOrder,
            @JsonProperty("extras") Map<String, Extra> extraOptions,
            @JsonProperty("extraOrder") List<String> extraOrder) {
        this.name = name;
        this.price = price;
        this.available = available;
        this.sideOptions = sideOptions;
        this.sideOrder = sideOrder != null ? sideOrder
                : (sideOptions != null ? new java.util.ArrayList<>(sideOptions.keySet()) : new java.util.ArrayList<>());
        this.extraOptions = extraOptions;
        this.extraOrder = extraOrder != null ? extraOrder
                : (extraOptions != null ? new java.util.ArrayList<>(extraOptions.keySet()) : new java.util.ArrayList<>());
    }

    public MenuItem(MenuItem other) {
        this.name = other.name;
        this.price = other.price;
        this.available = other.available;
        this.kitchen = other.kitchen;
        if (other.sideOptions != null) {
            this.sideOptions = new java.util.HashMap<>();
            for (Map.Entry<String, Side> entry : other.sideOptions.entrySet()) {
                this.sideOptions.put(entry.getKey(), new Side(entry.getValue()));
            }
        }
        this.sideOrder = other.sideOrder != null ? new java.util.ArrayList<>(other.sideOrder) : null;
        if (other.extraOptions != null) {
            this.extraOptions = new java.util.HashMap<>();
            for (Map.Entry<String, Extra> entry : other.extraOptions.entrySet()) {
                this.extraOptions.put(entry.getKey(), new Extra(entry.getValue()));
            }
        }
        this.extraOrder = other.extraOrder != null ? new java.util.ArrayList<>(other.extraOrder) : null;
    }

    public boolean hasSides() {
        return sideOptions != null && !sideOptions.isEmpty();
    }

    public boolean hasExtras() {
        return extraOptions != null && !extraOptions.isEmpty();
    }

    public List<String> getSideOrder() {
        return sideOrder;
    }

    public void setSideOrder(List<String> sideOrder) {
        this.sideOrder = sideOrder;
    }

    public List<String> getExtraOrder() {
        return extraOrder;
    }

    public void setExtraOrder(List<String> extraOrder) {
        this.extraOrder = extraOrder;
    }
}
