package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public class BaseItem {
    private UUID id;
    private String name;
    private long price;
    private boolean available;
    private boolean kitchen;
    private List<CompositeComponent> components;

    @JsonCreator
    public BaseItem(
            @JsonProperty("id") UUID id,
            @JsonProperty("name") String name,
            @JsonProperty("price") long price,
            @JsonProperty("available") boolean available,
            @JsonProperty("kitchen") boolean kitchen,
            @JsonProperty("components") List<CompositeComponent> components) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.available = available;
        this.kitchen = kitchen;
        this.components = components;
    }

    public BaseItem(UUID id, String name, long price, boolean available, boolean kitchen) {
        this(id, name, price, available, kitchen, null);
    }

    public BaseItem(BaseItem other) {
        this.id = other.id;
        this.name = other.name;
        this.price = other.price;
        this.available = other.available;
        this.kitchen = other.kitchen;
        this.components = other.components;
    }

    public UUID getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public long getPrice() { return price; }

    public void setPrice(long price) { this.price = price; }

    public boolean isAvailable() { return available; }

    public void setAvailable(boolean available) { this.available = available; }

    public boolean isKitchen() { return kitchen; }

    public void setKitchen(boolean kitchen) { this.kitchen = kitchen; }

    public List<CompositeComponent> getComponents() { return components; }

    public void setComponents(List<CompositeComponent> components) { this.components = components; }
}
