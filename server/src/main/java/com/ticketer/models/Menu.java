package com.ticketer.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Menu {
    private Map<String, List<MenuItem>> categories;

    @JsonCreator
    public Menu(Map<String, List<MenuItem>> categories) {
        this.categories = categories;
    }

    @JsonValue
    public Map<String, List<MenuItem>> getCategories() {
        return categories;
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
                        item.basePrice,
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
            return new OrderItem(item.name, null, item.basePrice);
        }

        if (sideName != null && item.sideOptions.containsKey(sideName)) {
            Side side = item.sideOptions.get(sideName);
            return new OrderItem(item.name, sideName, item.basePrice + side.price);
        } else {
            if (sideName != null) {
                throw new IllegalArgumentException("Invalid side selection: " + sideName);
            }
            return new OrderItem(item.name, null, item.basePrice);
        }
    }
}
