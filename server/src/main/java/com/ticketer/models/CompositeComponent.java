package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class CompositeComponent {
    private UUID baseItemId;
    private double quantity;

    @JsonCreator
    public CompositeComponent(
            @JsonProperty("baseItemId") UUID baseItemId,
            @JsonProperty("quantity") double quantity) {
        this.baseItemId = baseItemId;
        this.quantity = quantity;
    }

    public UUID getBaseItemId() { return baseItemId; }

    public double getQuantity() { return quantity; }
}
