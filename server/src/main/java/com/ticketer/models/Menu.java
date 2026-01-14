package com.ticketer.models;

import com.ticketer.utils.menu.dto.ComplexItem;
import com.ticketer.utils.menu.dto.MenuItemView;
import com.ticketer.utils.menu.dto.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Menu {
    private Map<String, List<ComplexItem>> categories;

    public Menu(Map<String, List<ComplexItem>> categories) {
        this.categories = categories;
    }

    public Map<String, List<ComplexItem>> getCategories() {
        return categories;
    }

    public List<ComplexItem> getCategory(String categoryName) {
        return categories.get(categoryName);
    }

    public List<MenuItemView> getAllItems() {
        List<MenuItemView> list = new ArrayList<>();
        for (String category : categories.keySet()) {
            List<ComplexItem> items = categories.get(category);
            for (ComplexItem item : items) {
                list.add(new MenuItemView(
                        item.name,
                        item.basePrice,
                        item.available,
                        category));
            }
        }
        return list;
    }

    public ComplexItem getItem(String name) {
        for (List<ComplexItem> items : categories.values()) {
            for (ComplexItem item : items) {
                if (item.name.equals(name)) {
                    return item;
                }
            }
        }
        return null;
    }

    public static Item getItem(ComplexItem item, String sideName) {
        if (!item.hasSides()) {
            return new Item(item.name, null, item.basePrice);
        }

        if (sideName != null && item.sideOptions.containsKey(sideName)) {
            Side side = item.sideOptions.get(sideName);
            return new Item(item.name, sideName, item.basePrice + side.price);
        } else {
            if (sideName != null) {
                throw new IllegalArgumentException("Invalid side selection: " + sideName);
            }
            return new Item(item.name, null, item.basePrice);
        }
    }
}
