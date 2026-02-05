package com.ticketer.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

public class Menu {
    private Map<String, List<MenuItem>> categories;
    private List<String> kitchenItems;

    @JsonCreator
    public Menu(
            @com.fasterxml.jackson.annotation.JsonProperty("categories") Map<String, List<MenuItem>> categories,
            @com.fasterxml.jackson.annotation.JsonProperty("kitchenItems") List<String> kitchenItems) {
        this.categories = categories;
        this.kitchenItems = kitchenItems != null ? kitchenItems : new ArrayList<>();
    }

    public Map<String, List<MenuItem>> getCategories() {
        return categories;
    }

    public List<String> getKitchenItems() {
        return kitchenItems;
    }

    public void setKitchenItems(List<String> kitchenItems) {
        this.kitchenItems = kitchenItems;
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

    public static OrderItem getItem(MenuItem item, String sideName) {
        if (!item.hasSides()) {
            return new OrderItem(item.name, null, item.price, 0);
        }

        if (sideName != null && item.sideOptions.containsKey(sideName)) {
            Side side = item.sideOptions.get(sideName);
            return new OrderItem(item.name, sideName, item.price, side.price);
        } else {
            if (sideName != null) {
                throw new IllegalArgumentException("Invalid side selection: " + sideName);
            }
            return new OrderItem(item.name, null, item.price, 0);
        }
    }
}
