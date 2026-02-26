package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class ComboComponentSnapshot {
    private UUID baseItemId;
    private String name;
    private long basePrice;

    @JsonCreator
    public ComboComponentSnapshot(
            @JsonProperty("baseItemId") UUID baseItemId,
            @JsonProperty("name") String name,
            @JsonProperty("basePrice") long basePrice) {
        this.baseItemId = baseItemId;
        this.name = name;
        this.basePrice = basePrice;
    }

    public UUID getBaseItemId() { return baseItemId; }

    public String getName() { return name; }

    public long getBasePrice() { return basePrice; }
}
