package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItem {

    public static final String TYPE_ITEM = "ITEM";
    public static final String TYPE_COMBO = "COMBO";

    private String type;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID menuItemId;

    private String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID selectedSideId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String selectedSide;

    private long mainPrice;
    private long sidePrice;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID comboId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ComboComponentSnapshot> components;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ComboSlotSelection> slotSelections;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String comment;

    @JsonCreator
    public OrderItem(
            @JsonProperty("type") String type,
            @JsonProperty("menuItemId") UUID menuItemId,
            @JsonProperty("name") String name,
            @JsonProperty("selectedSideId") UUID selectedSideId,
            @JsonProperty("selectedSide") String selectedSide,
            @JsonProperty("mainPrice") long mainPrice,
            @JsonProperty("sidePrice") long sidePrice,
            @JsonProperty("comboId") UUID comboId,
            @JsonProperty("components") List<ComboComponentSnapshot> components,
            @JsonProperty("slotSelections") List<ComboSlotSelection> slotSelections,
            @JsonProperty("comment") String comment) {
        this.type = type != null ? type : TYPE_ITEM;
        this.menuItemId = menuItemId;
        this.name = name;
        this.selectedSideId = selectedSideId;
        this.selectedSide = selectedSide;
        this.mainPrice = mainPrice;
        this.sidePrice = sidePrice;
        this.comboId = comboId;
        this.components = components;
        this.slotSelections = slotSelections;
        this.comment = comment;
    }

    public static OrderItem forItem(UUID menuItemId, String name,
            UUID selectedSideId, String selectedSide,
            long mainPrice, long sidePrice) {
        return new OrderItem(TYPE_ITEM, menuItemId, name,
                selectedSideId, selectedSide,
                mainPrice, sidePrice,
                null, null, null, null);
    }

    public static OrderItem forCombo(UUID comboId, String name,
            List<ComboComponentSnapshot> components,
            List<ComboSlotSelection> slotSelections,
            long price) {
        return new OrderItem(TYPE_COMBO, null, name,
                null, null,
                price, 0,
                comboId, components, slotSelections, null);
    }

    public boolean isCombo() {
        return TYPE_COMBO.equals(type);
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public long getPrice() {
        return mainPrice + sidePrice;
    }

    public String getType() { return type; }

    public UUID getMenuItemId() { return menuItemId; }

    public String getName() { return name; }

    public UUID getSelectedSideId() { return selectedSideId; }

    public String getSelectedSide() { return selectedSide; }

    public long getMainPrice() { return mainPrice; }

    public void setMainPrice(long mainPrice) { this.mainPrice = mainPrice; }

    public long getSidePrice() { return sidePrice; }

    public void setSidePrice(long sidePrice) { this.sidePrice = sidePrice; }

    public UUID getComboId() { return comboId; }

    public List<ComboComponentSnapshot> getComponents() { return components; }

    public List<ComboSlotSelection> getSlotSelections() { return slotSelections; }

    public String getComment() { return comment; }

    public void setComment(String comment) { this.comment = comment; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem other = (OrderItem) o;
        if (TYPE_COMBO.equals(type)) {
            return java.util.Objects.equals(comboId, other.comboId)
                    && java.util.Objects.equals(slotSelections == null ? null :
                            slotSelections.stream()
                                    .map(s -> s.getSlotId() + ":" + s.getSelectedBaseItemId())
                                    .sorted().collect(java.util.stream.Collectors.joining(",")),
                            other.slotSelections == null ? null :
                            other.slotSelections.stream()
                                    .map(s -> s.getSlotId() + ":" + s.getSelectedBaseItemId())
                                    .sorted().collect(java.util.stream.Collectors.joining(",")));
        }
        return java.util.Objects.equals(menuItemId, other.menuItemId)
                && java.util.Objects.equals(selectedSideId, other.selectedSideId);
    }

    @Override
    public int hashCode() {
        if (TYPE_COMBO.equals(type)) {
            return java.util.Objects.hash(comboId);
        }
        return java.util.Objects.hash(menuItemId, selectedSideId);
    }

    @Override
    public String toString() {
        if (TYPE_COMBO.equals(type)) {
            return String.format("Combo: %s ($%.2f)", name, getPrice() / 100.0);
        }
        if (selectedSide != null) {
            return String.format("Item: %s ($%.2f), Side: %s ($%.2f), Total: $%.2f",
                    name, mainPrice / 100.0, selectedSide, sidePrice / 100.0, getPrice() / 100.0);
        }
        return String.format("Item: %s, Total: $%.2f", name, mainPrice / 100.0);
    }
}
