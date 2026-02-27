package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ComboSlot {
    private UUID id;
    private String name;
    private List<UUID> options;
    private List<UUID> optionOrder;
    private boolean required;
    private String categorySource;

    @JsonCreator
    public ComboSlot(
            @JsonProperty("id") UUID id,
            @JsonProperty("name") String name,
            @JsonProperty("options") List<UUID> options,
            @JsonProperty("optionOrder") List<UUID> optionOrder,
            @JsonProperty("required") boolean required,
            @JsonProperty("categorySource") String categorySource) {
        this.id = id;
        this.name = name;
        this.options = options != null ? options : new ArrayList<>();
        this.optionOrder = optionOrder != null ? optionOrder : new ArrayList<>(this.options);
        this.required = required;
        this.categorySource = categorySource;
    }

    public ComboSlot(UUID id, String name, List<UUID> options, List<UUID> optionOrder, boolean required) {
        this(id, name, options, optionOrder, required, null);
    }

    public UUID getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public List<UUID> getOptions() { return options; }

    public void setOptions(List<UUID> options) { this.options = options; }

    public List<UUID> getOptionOrder() { return optionOrder; }

    public void setOptionOrder(List<UUID> optionOrder) { this.optionOrder = optionOrder; }

    public boolean isRequired() { return required; }

    public void setRequired(boolean required) { this.required = required; }

    public String getCategorySource() { return categorySource; }

    public void setCategorySource(String categorySource) { this.categorySource = categorySource; }
}
