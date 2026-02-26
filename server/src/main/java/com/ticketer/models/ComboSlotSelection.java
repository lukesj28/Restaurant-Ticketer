package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class ComboSlotSelection {
    private UUID slotId;
    private UUID selectedBaseItemId;
    private String selectedName;
    private long basePrice;

    @JsonCreator
    public ComboSlotSelection(
            @JsonProperty("slotId") UUID slotId,
            @JsonProperty("selectedBaseItemId") UUID selectedBaseItemId,
            @JsonProperty("selectedName") String selectedName,
            @JsonProperty("basePrice") long basePrice) {
        this.slotId = slotId;
        this.selectedBaseItemId = selectedBaseItemId;
        this.selectedName = selectedName;
        this.basePrice = basePrice;
    }

    public UUID getSlotId() { return slotId; }

    public UUID getSelectedBaseItemId() { return selectedBaseItemId; }

    public String getSelectedName() { return selectedName; }

    public long getBasePrice() { return basePrice; }
}
