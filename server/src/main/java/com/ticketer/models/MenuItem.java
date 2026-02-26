package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MenuItem {
    private UUID baseItemId;

    private List<String> sideSources;

    @JsonCreator
    public MenuItem(
            @JsonProperty("baseItemId") UUID baseItemId,
            @JsonProperty("sideSources") List<String> sideSources) {
        this.baseItemId = baseItemId;
        this.sideSources = sideSources != null ? sideSources : new ArrayList<>();
    }

    public UUID getBaseItemId() { return baseItemId; }

    public List<String> getSideSources() { return sideSources; }

    public void setSideSources(List<String> sideSources) {
        this.sideSources = sideSources != null ? sideSources : new ArrayList<>();
    }

    public boolean hasSideSlot() {
        return sideSources != null && !sideSources.isEmpty();
    }
}
