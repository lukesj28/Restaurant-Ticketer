package com.ticketer.controllers;

import com.ticketer.models.Menu;
import com.ticketer.models.Item;
import com.ticketer.utils.menu.MenuEditor;
import com.ticketer.utils.menu.MenuReader;
import com.ticketer.utils.menu.dto.ComplexItem;
import com.ticketer.utils.menu.dto.MenuItemView;
import com.ticketer.exceptions.*;

import java.util.List;
import java.util.Map;

public class MenuController {

    private Menu menu;

    public MenuController() {
        refreshMenu();
    }

    public void refreshMenu() {
        this.menu = MenuReader.readMenu();
    }

    public Menu getMenu() {
        return menu;
    }

    public ComplexItem getItem(String name) {
        return menu != null ? menu.getItem(name) : null;
    }

    public Item getItem(ComplexItem item, String sideName) {
        return Menu.getItem(item, sideName);
    }

    public List<ComplexItem> getCategory(String categoryName) {
        if (menu == null) {
            return java.util.Collections.emptyList();
        }
        List<ComplexItem> items = menu.getCategory(categoryName.toLowerCase());
        return items != null ? items : java.util.Collections.emptyList();
    }

    public List<MenuItemView> getAllItems() {
        return menu != null ? menu.getAllItems() : java.util.Collections.emptyList();
    }

    public Map<String, List<ComplexItem>> getCategories() {
        return menu != null ? menu.getCategories() : java.util.Collections.emptyMap();
    }

    public void addItem(String category, String name, int price, Map<String, Integer> sides) {
        if (price < 0) {
            throw new ValidationException("Price cannot be negative");
        }
        MenuEditor.addItem(category, name, price, sides);
        refreshMenu();
    }

    public void editItemPrice(String itemName, int newPrice) {
        if (getItem(itemName) == null) {
            throw new EntityNotFoundException("Item with name " + itemName + " not found");
        }
        if (newPrice < 0) {
            throw new ValidationException("Price cannot be negative");
        }
        MenuEditor.editItemPrice(itemName, newPrice);
        refreshMenu();
    }

    public void editItemAvailability(String itemName, boolean available) {
        if (getItem(itemName) == null) {
            throw new EntityNotFoundException("Item with name " + itemName + " not found");
        }
        MenuEditor.editItemAvailability(itemName, available);
        refreshMenu();
    }

    public void renameItem(String oldName, String newName) {
        if (getItem(oldName) == null) {
            throw new EntityNotFoundException("Item with name " + oldName + " not found");
        }
        if (getItem(newName) != null) {
            throw new ResourceConflictException("Item with name " + newName + " already exists");
        }
        MenuEditor.renameItem(oldName, newName);
        refreshMenu();
    }

    public void removeItem(String itemName) {
        if (getItem(itemName) == null) {
            throw new EntityNotFoundException("Item with name " + itemName + " not found");
        }
        MenuEditor.removeItem(itemName);
        refreshMenu();
    }

    public void renameCategory(String oldCategory, String newCategory) {
        if (getCategory(oldCategory) == null) {
            throw new EntityNotFoundException("Category " + oldCategory + " not found");
        }
        MenuEditor.renameCategory(oldCategory, newCategory);
        refreshMenu();
    }

    public void changeCategory(String itemName, String newCategory) {
        if (getItem(itemName) == null) {
            throw new EntityNotFoundException("Item with name " + itemName + " not found");
        }
        MenuEditor.changeCategory(itemName, newCategory);
        refreshMenu();
    }

    public void updateSide(String itemName, String sideName, int newPrice) {
        if (getItem(itemName) == null) {
            throw new EntityNotFoundException("Item with name " + itemName + " not found");
        }
        if (newPrice < 0) {
            throw new ValidationException("Price cannot be negative");
        }
        MenuEditor.updateSide(itemName, sideName, newPrice);
        refreshMenu();
    }
}
