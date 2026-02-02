package com.ticketer.models;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MenuItem {
    public String name;
    public int price;
    public boolean available;
    @JsonProperty("sides")
    public Map<String, Side> sideOptions;

    @JsonCreator
    public MenuItem(@JsonProperty("name") String name,
            @JsonProperty("price") int price,
            @JsonProperty("available") boolean available,
            @JsonProperty("sides") Map<String, Side> sideOptions) {
        this.name = name;
        this.price = price;
        this.available = available;
        this.sideOptions = sideOptions;
    }

    public boolean hasSides() {
        return sideOptions != null && !sideOptions.isEmpty();
    }

}
