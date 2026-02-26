package com.ticketer.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Menu {
    private Map<UUID, BaseItem> baseItems;
    private Map<String, CategoryEntry> categories;
    private Map<UUID, ComboItem> combos;
    private List<String> categoryOrder;

    @JsonCreator
    public Menu(
            @JsonProperty("baseItems") Map<UUID, BaseItem> baseItems,
            @JsonProperty("categories") Map<String, CategoryEntry> categories,
            @JsonProperty("combos") Map<UUID, ComboItem> combos,
            @JsonProperty("categoryOrder") List<String> categoryOrder) {
        this.baseItems = baseItems != null ? baseItems : new LinkedHashMap<>();
        this.categories = categories != null ? categories : new LinkedHashMap<>();
        this.combos = combos != null ? combos : new LinkedHashMap<>();
        this.categoryOrder = categoryOrder != null
                ? categoryOrder
                : new ArrayList<>(this.categories.keySet());
    }

    public Map<UUID, BaseItem> getBaseItems() { return baseItems; }

    public BaseItem getBaseItem(UUID id) { return baseItems.get(id); }

    public void addBaseItem(BaseItem item) {
        baseItems.put(item.getId(), item);
    }

    public boolean removeBaseItem(UUID id) {
        return baseItems.remove(id) != null;
    }

    public Map<String, CategoryEntry> getCategories() { return categories; }

    public CategoryEntry getCategory(String name) { return categories.get(name); }

    public List<MenuItem> getCategoryItems(String name) {
        CategoryEntry entry = categories.get(name);
        return entry != null ? entry.getItems() : null;
    }

    public void addCategory(String name, CategoryEntry entry) {
        categories.put(name, entry);
        if (!categoryOrder.contains(name)) {
            categoryOrder.add(name);
        }
    }

    public boolean removeCategory(String name) {
        boolean removed = categories.remove(name) != null;
        if (removed) categoryOrder.remove(name);
        return removed;
    }

    public void renameCategory(String oldName, String newName) {
        CategoryEntry entry = categories.remove(oldName);
        if (entry != null) {
            categories.put(newName, entry);
            int idx = categoryOrder.indexOf(oldName);
            if (idx >= 0) categoryOrder.set(idx, newName);
        }
    }

    public void setCategoryVisible(String name, boolean visible) {
        CategoryEntry entry = categories.get(name);
        if (entry != null) entry.setVisible(visible);
    }

    public boolean isCategoryVisible(String name) {
        CategoryEntry entry = categories.get(name);
        return entry != null && entry.isVisible();
    }

    public void addMenuItem(String category, MenuItem item) {
        CategoryEntry entry = categories.get(category);
        if (entry == null) {
            entry = new CategoryEntry(true, new ArrayList<>());
            categories.put(category, entry);
            if (!categoryOrder.contains(category)) categoryOrder.add(category);
        }
        entry.getItems().add(item);
    }

    public boolean removeMenuItem(String category, UUID baseItemId) {
        CategoryEntry entry = categories.get(category);
        if (entry == null) return false;
        boolean removed = entry.getItems().removeIf(mi -> baseItemId.equals(mi.getBaseItemId()));
        if (removed && entry.getItems().isEmpty()) {
            categories.remove(category);
            categoryOrder.remove(category);
        }
        return removed;
    }

    public MenuItem findMenuItem(String category, UUID baseItemId) {
        CategoryEntry entry = categories.get(category);
        if (entry == null) return null;
        return entry.getItems().stream()
                .filter(mi -> baseItemId.equals(mi.getBaseItemId()))
                .findFirst().orElse(null);
    }

    public String findCategoryForBaseItem(UUID baseItemId) {
        for (Map.Entry<String, CategoryEntry> e : categories.entrySet()) {
            for (MenuItem mi : e.getValue().getItems()) {
                if (baseItemId.equals(mi.getBaseItemId())) return e.getKey();
            }
        }
        return null;
    }

    public List<BaseItem> getSideOptions(List<String> sourceCategoryNames) {
        List<BaseItem> result = new ArrayList<>();
        if (sourceCategoryNames == null) return result;
        for (String catName : sourceCategoryNames) {
            CategoryEntry entry = categories.get(catName);
            if (entry == null) continue;
            for (MenuItem mi : entry.getItems()) {
                BaseItem item = baseItems.get(mi.getBaseItemId());
                if (item != null && item.isAvailable()) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    public Map<UUID, ComboItem> getCombos() { return combos; }

    public ComboItem getCombo(UUID id) { return combos.get(id); }

    public void addCombo(ComboItem combo) {
        combos.put(combo.getId(), combo);
    }

    public boolean removeCombo(UUID id) {
        return combos.remove(id) != null;
    }

    public List<ComboItem> getCombosForCategory(String category) {
        List<ComboItem> result = new ArrayList<>();
        for (ComboItem c : combos.values()) {
            if (category.equals(c.getCategory())) result.add(c);
        }
        return result;
    }

    public List<String> getCategoryOrder() { return categoryOrder; }

    public void setCategoryOrder(List<String> categoryOrder) { this.categoryOrder = categoryOrder; }
}
