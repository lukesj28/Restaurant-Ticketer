package com.ticketer.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

public class Menu {
    private Map<String, List<MenuItem>> categories;
    private List<String> categoryOrder;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private Map<String, MenuItem> nameToItemMap = new java.util.HashMap<>();

    @JsonCreator
    public Menu(
            @com.fasterxml.jackson.annotation.JsonProperty("categories") Map<String, List<MenuItem>> categories,
            @com.fasterxml.jackson.annotation.JsonProperty("categoryOrder") List<String> categoryOrder) {
        this.categories = categories;
        this.categoryOrder = categoryOrder != null ? categoryOrder : new ArrayList<>(categories.keySet());
        
        for (List<MenuItem> items : this.categories.values()) {
            if (items != null) {
                for (MenuItem item : items) {
                    nameToItemMap.put(item.name, item);
                }
            }
        }
    }

    public Map<String, List<MenuItem>> getCategories() {
        return categories;
    }

    public List<String> getCategoryOrder() {
        return categoryOrder;
    }

    public void setCategoryOrder(List<String> categoryOrder) {
        this.categoryOrder = categoryOrder;
    }

    public List<MenuItem> getCategory(String categoryName) {
        return categories.get(categoryName);
    }

    public List<MenuItemView> getAllItems() {
        List<MenuItemView> list = new ArrayList<>();
        for (String category : categories.keySet()) {
            List<MenuItem> items = categories.get(category);
            for (MenuItem item : items) {
                list.add(new MenuItemView(
                        item.name,
                        item.price,
                        item.available));
            }
        }
        return list;
    }

    public MenuItem getItem(String name) {
        return nameToItemMap.get(name);
    }

    public void addItem(String category, MenuItem item) {
        List<MenuItem> items = categories.get(category);
        if (items == null) {
            items = new ArrayList<>();
            categories.put(category, items);
            if (!categoryOrder.contains(category)) {
                categoryOrder.add(category);
            }
        }
        items.add(item);
        nameToItemMap.put(item.name, item);
    }

    public boolean removeItem(String category, String itemName) {
        List<MenuItem> items = categories.get(category);
        if (items == null) return false;
        
        boolean removed = items.removeIf(i -> i.name.equals(itemName));
        if (removed) {
            nameToItemMap.remove(itemName);
            if (items.isEmpty()) {
                categories.remove(category);
            }
        }
        return removed;
    }

    public void renameItem(String oldName, String newName) {
        MenuItem item = nameToItemMap.remove(oldName);
        if (item != null) {
            item.name = newName;
            nameToItemMap.put(newName, item);
        }
    }
    
    public void changeCategory(String oldCategory, String newCategory, String itemName) {
        List<MenuItem> oldItems = categories.get(oldCategory);
        if (oldItems == null) return;
        
        MenuItem item = null;
        for (int i = 0; i < oldItems.size(); i++) {
            if (oldItems.get(i).name.equals(itemName)) {
                item = oldItems.remove(i);
                break;
            }
        }
        
        if (item == null) return;
        
        if (oldItems.isEmpty()) {
            categories.remove(oldCategory);
        }
        
        List<MenuItem> newItems = categories.get(newCategory);
        if (newItems == null) {
            newItems = new ArrayList<>();
            categories.put(newCategory, newItems);
            if (!categoryOrder.contains(newCategory)) {
                categoryOrder.add(newCategory);
            }
        }
        newItems.add(item);
    }

    public static OrderItem getItem(String category, MenuItem item, String sideName, String extraName) {
        long sidePrice = 0;
        String resolvedSide = null;
        if (item.hasSides()) {
            if (sideName != null && item.sideOptions.containsKey(sideName)) {
                resolvedSide = sideName;
                sidePrice = item.sideOptions.get(sideName).price;
            } else if (sideName != null) {
                throw new IllegalArgumentException("Invalid side selection: " + sideName);
            }
        }

        long extraPrice = 0;
        String resolvedExtra = null;
        if (item.hasExtras()) {
            if (extraName != null && item.extraOptions.containsKey(extraName)) {
                resolvedExtra = extraName;
                extraPrice = item.extraOptions.get(extraName).price;
            } else if (extraName != null) {
                throw new IllegalArgumentException("Invalid extra selection: " + extraName);
            }
        }

        return new OrderItem(category, item.name, resolvedSide, resolvedExtra, item.price, sidePrice, extraPrice, null);
    }
}
