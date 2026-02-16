package com.ticketer.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

public class Menu {
    private Map<String, List<MenuItem>> categories;
    private List<String> kitchenItems;
    private List<String> categoryOrder;

    @JsonCreator
    public Menu(
            @com.fasterxml.jackson.annotation.JsonProperty("categories") Map<String, List<MenuItem>> categories,
            @com.fasterxml.jackson.annotation.JsonProperty("kitchenItems") List<String> kitchenItems,
            @com.fasterxml.jackson.annotation.JsonProperty("categoryOrder") List<String> categoryOrder) {
        this.categories = categories;
        this.kitchenItems = kitchenItems != null ? kitchenItems : new ArrayList<>();
        this.categoryOrder = categoryOrder != null ? categoryOrder : new ArrayList<>(categories.keySet());
    }

    public Map<String, List<MenuItem>> getCategories() {
        return categories;
    }

    public List<String> getKitchenItems() {
        List<String> sortedKitchenItems = new ArrayList<>();

        for (String category : categoryOrder) {
            List<MenuItem> items = categories.get(category);
            if (items != null) {
                for (MenuItem item : items) {
                    if (this.kitchenItems.contains(item.name)) {
                        sortedKitchenItems.add(item.name);
                    }
                }
            }
        }

        for (String category : categories.keySet()) {
            if (!categoryOrder.contains(category)) {
                List<MenuItem> items = categories.get(category);
                if (items != null) {
                    for (MenuItem item : items) {
                        if (this.kitchenItems.contains(item.name)) {
                            sortedKitchenItems.add(item.name);
                        }
                    }
                }
            }
        }

        return sortedKitchenItems;
    }

    public void setKitchenItems(List<String> kitchenItems) {
        this.kitchenItems = kitchenItems;
    }

    public List<String> getCategoryOrder() {
        return categoryOrder;
    }

    public void setCategoryOrder(List<String> categoryOrder) {
        this.categoryOrder = categoryOrder;
    }

    public boolean addKitchenItem(String itemName) {
        if (!this.kitchenItems.contains(itemName)) {
            this.kitchenItems.add(itemName);
            return true;
        }
        return false;
    }

    public boolean removeKitchenItem(String itemName) {
        return this.kitchenItems.remove(itemName);
    }

    public boolean isKitchenItem(String itemName) {
        return this.kitchenItems.contains(itemName);
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
        for (List<MenuItem> items : categories.values()) {
            for (MenuItem item : items) {
                if (item.name.equals(name)) {
                    return item;
                }
            }
        }
        return null;
    }

    public static OrderItem getItem(MenuItem item, String sideName, String extraName) {
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

        return new OrderItem(item.name, resolvedSide, resolvedExtra, item.price, sidePrice, extraPrice, null);
    }
}
