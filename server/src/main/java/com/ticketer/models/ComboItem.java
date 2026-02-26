package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ComboItem {
    private UUID id;
    private String name;
    private String category;
    private List<UUID> components;
    private List<ComboSlot> slots;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long price;

    private boolean available;
    private boolean kitchen;

    @JsonCreator
    public ComboItem(
            @JsonProperty("id") UUID id,
            @JsonProperty("name") String name,
            @JsonProperty("category") String category,
            @JsonProperty("components") List<UUID> components,
            @JsonProperty("slots") List<ComboSlot> slots,
            @JsonProperty("price") Long price,
            @JsonProperty("available") boolean available,
            @JsonProperty("kitchen") boolean kitchen) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.components = components != null ? components : new ArrayList<>();
        this.slots = slots != null ? slots : new ArrayList<>();
        this.price = price;
        this.available = available;
        this.kitchen = kitchen;
    }

    public UUID getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }

    public void setCategory(String category) { this.category = category; }

    public List<UUID> getComponents() { return components; }

    public void setComponents(List<UUID> components) { this.components = components; }

    public List<ComboSlot> getSlots() { return slots; }

    public void setSlots(List<ComboSlot> slots) { this.slots = slots; }

    public Long getPrice() { return price; }

    public void setPrice(Long price) { this.price = price; }

    public boolean isAvailable() { return available; }

    public void setAvailable(boolean available) { this.available = available; }

    public boolean isKitchen() { return kitchen; }

    public void setKitchen(boolean kitchen) { this.kitchen = kitchen; }

    public long computeBasePrice(java.util.Map<UUID, BaseItem> baseItemMap,
            java.util.Map<UUID, UUID> slotSelections) {
        if (price != null) return price;
        long total = 0;
        for (UUID compId : components) {
            BaseItem item = baseItemMap.get(compId);
            if (item != null) total += item.getPrice();
        }
        if (slotSelections != null) {
            for (UUID selectedId : slotSelections.values()) {
                BaseItem item = baseItemMap.get(selectedId);
                if (item != null) total += item.getPrice();
            }
        }
        return total;
    }
}
