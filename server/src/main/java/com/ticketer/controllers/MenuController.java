package com.ticketer.controllers;

import com.ticketer.models.Menu;
import com.ticketer.models.Item;
import com.ticketer.utils.menu.MenuEditor;
import com.ticketer.utils.menu.MenuReader;
import com.ticketer.utils.menu.dto.ComplexItem;
import com.ticketer.utils.menu.dto.MenuItemView;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MenuController {

    private Menu menu;

    public MenuController() throws IOException {
        refreshMenu();
    }

    public void refreshMenu() throws IOException {
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
        return menu != null ? menu.getCategory(categoryName) : null;
    }

    public List<MenuItemView> getAllItems() {
        return menu != null ? menu.getAllItems() : java.util.Collections.emptyList();
    }

    public Map<String, List<ComplexItem>> getCategories() {
        return menu != null ? menu.getCategories() : java.util.Collections.emptyMap();
    }

    public void addItem(String category, String name, double price, Map<String, Double> sides) throws IOException {
        MenuEditor.addItem(category, name, price, sides);
        refreshMenu();
    }

    public void editItemPrice(String itemName, double newPrice) throws IOException {
        MenuEditor.editItemPrice(itemName, newPrice);
        refreshMenu();
    }

    public void editItemAvailability(String itemName, boolean available) throws IOException {
        MenuEditor.editItemAvailability(itemName, available);
        refreshMenu();
    }

    public void renameItem(String oldName, String newName) throws IOException {
        MenuEditor.renameItem(oldName, newName);
        refreshMenu();
    }

    public void removeItem(String itemName) throws IOException {
        MenuEditor.removeItem(itemName);
        refreshMenu();
    }

    public void renameCategory(String oldCategory, String newCategory) throws IOException {
        MenuEditor.renameCategory(oldCategory, newCategory);
        refreshMenu();
    }

    public void changeCategory(String itemName, String newCategory) throws IOException {
        MenuEditor.changeCategory(itemName, newCategory);
        refreshMenu();
    }

    public void updateSide(String itemName, String sideName, double newPrice) throws IOException {
        MenuEditor.updateSide(itemName, sideName, newPrice);
        refreshMenu();
    }
}
