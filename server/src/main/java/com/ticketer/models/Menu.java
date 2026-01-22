package com.ticketer.models;

import com.ticketer.utils.menu.dto.MenuItem;
import com.ticketer.utils.menu.dto.MenuItemView;
import com.ticketer.utils.menu.dto.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Menu {
    private Map<String, List<MenuItem>> categories;

    public Menu(Map<String, List<MenuItem>> categories) {
        this.categories = categories;
    }

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
