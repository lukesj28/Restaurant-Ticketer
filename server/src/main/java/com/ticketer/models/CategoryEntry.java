package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class CategoryEntry {
    private boolean visible;
    private List<MenuItem> items;

    @JsonCreator
    public CategoryEntry(
            @JsonProperty("visible") boolean visible,
            @JsonProperty("items") List<MenuItem> items) {
        this.visible = visible;
        this.items = items != null ? items : new ArrayList<>();
    }

    public CategoryEntry() {
        this.visible = true;
        this.items = new ArrayList<>();
    }

    public boolean isVisible() { return visible; }

    public void setVisible(boolean visible) { this.visible = visible; }

    public List<MenuItem> getItems() { return items; }

    public void setItems(List<MenuItem> items) { this.items = items; }
}
