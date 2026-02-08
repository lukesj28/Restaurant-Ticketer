package com.ticketer.services;

import com.ticketer.models.Menu;
import com.ticketer.models.OrderItem;
import com.ticketer.repositories.MenuRepository;
import com.ticketer.models.MenuItem;
import com.ticketer.models.MenuItemView;
import com.ticketer.models.Side;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.InvalidInputException;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MenuService.class);

    private final MenuRepository menuRepository;
    private Menu currentMenu;

    @Autowired
    public MenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
        this.currentMenu = menuRepository.getMenu();
    }

    public void refreshMenu() {
        this.currentMenu = menuRepository.getMenu();
    }

    public MenuItem getItem(String name) {
        MenuItem item = currentMenu.getItem(name);
        if (item == null) {
            throw new EntityNotFoundException("Item not found: " + name);
        }
        return item;
    }

    public OrderItem createOrderItem(String itemName, String sideName) {
        MenuItem item = getItem(itemName);
        return Menu.getItem(item, sideName);
    }

    public List<MenuItem> getCategory(String categoryName) {
        List<MenuItem> items = currentMenu.getCategory(categoryName.toLowerCase());
        if (items == null) {
            throw new EntityNotFoundException("Category not found: " + categoryName);
        }
        return items;
    }

    public String getCategoryOfItem(String itemName) {
        for (Map.Entry<String, List<MenuItem>> entry : currentMenu.getCategories().entrySet()) {
            for (MenuItem item : entry.getValue()) {
                if (item.name.equals(itemName)) {
                    return entry.getKey();
                }
            }
        }
        throw new EntityNotFoundException("Item not found: " + itemName);
    }

    public Map<String, List<MenuItem>> getCategories() {
        return currentMenu.getCategories();
    }

    public List<MenuItemView> getAllItems() {
        return currentMenu.getAllItems();
    }

    public void addItem(String category, String name, long price, Map<String, Long> sides) {
        logger.info("Adding item {} to category {} with price {}", name, category, price);
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Item name cannot be empty");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new InvalidInputException("Category cannot be empty");
        }
        if (price < 0) {
            throw new InvalidInputException("Price cannot be negative");
        }

        category = category.toLowerCase();
        Map<String, List<MenuItem>> categories = currentMenu.getCategories();

        List<MenuItem> items = categories.get(category);
        if (items == null) {
            items = new java.util.ArrayList<>();
            categories.put(category, items);
        }

        Map<String, Side> sideObjects = null;
        if (sides != null && !sides.isEmpty()) {
            sideObjects = new java.util.HashMap<>();
            for (Map.Entry<String, Long> entry : sides.entrySet()) {
                Side side = new Side();
                side.price = entry.getValue();
                side.available = true;
                sideObjects.put(entry.getKey(), side);
            }
        }

        items.add(new MenuItem(name, price, true, sideObjects, null));
        menuRepository.saveMenu(currentMenu);
    }

    public void editItemPrice(String itemName, long newPrice) {
        logger.info("Editing price for item {} to {}", itemName, newPrice);
        if (newPrice < 0) {
            throw new InvalidInputException("Price cannot be negative");
        }
        MenuItem item = getItem(itemName);
        item.price = newPrice;
        menuRepository.saveMenu(currentMenu);
    }

    public void editItemAvailability(String itemName, boolean available) {
        logger.info("Setting availability for item {} to {}", itemName, available);
        MenuItem item = getItem(itemName);
        item.available = available;
        menuRepository.saveMenu(currentMenu);
    }

    public void renameItem(String oldName, String newName) {
        logger.info("Renaming item {} to {}", oldName, newName);
        if (newName == null || newName.trim().isEmpty()) {
            throw new InvalidInputException("New name cannot be empty");
        }
        MenuItem item = getItem(oldName);
        item.name = newName;

        if (currentMenu.isKitchenItem(oldName)) {
            currentMenu.removeKitchenItem(oldName);
            currentMenu.addKitchenItem(newName);
        }

        menuRepository.saveMenu(currentMenu);
    }

    public void removeItem(String itemName) {
        logger.info("Removing item {}", itemName);
        String category = getCategoryOfItem(itemName);
        List<MenuItem> items = currentMenu.getCategory(category);
        items.removeIf(i -> i.name.equals(itemName));

        if (items.isEmpty()) {
            currentMenu.getCategories().remove(category);
        }

        currentMenu.removeKitchenItem(itemName);

        menuRepository.saveMenu(currentMenu);
    }

    public void renameCategory(String oldCategory, String newCategory) {
        logger.info("Renaming category {} to {}", oldCategory, newCategory);
        if (newCategory == null || newCategory.trim().isEmpty()) {
            throw new InvalidInputException("New category name cannot be empty");
        }
        oldCategory = oldCategory.toLowerCase();
        newCategory = newCategory.toLowerCase();
        Map<String, List<MenuItem>> categories = currentMenu.getCategories();

        if (!categories.containsKey(oldCategory)) {
            throw new EntityNotFoundException("Category not found: " + oldCategory);
        }

        List<MenuItem> items = categories.remove(oldCategory);
        if (categories.containsKey(newCategory)) {
            categories.get(newCategory).addAll(items);
        } else {
            categories.put(newCategory, items);
        }
        menuRepository.saveMenu(currentMenu);
    }

    public void changeCategory(String itemName, String newCategory) {
        logger.info("Changing category for item {} to {}", itemName, newCategory);
        if (newCategory == null || newCategory.trim().isEmpty()) {
            throw new InvalidInputException("New category name cannot be empty");
        }
        String oldCategory = getCategoryOfItem(itemName);
        newCategory = newCategory.toLowerCase();

        if (oldCategory.equals(newCategory))
            return;

        List<MenuItem> oldItems = currentMenu.getCategory(oldCategory);
        MenuItem item = getItem(itemName);

        oldItems.remove(item);

        if (oldItems.isEmpty()) {
            currentMenu.getCategories().remove(oldCategory);
        }

        Map<String, List<MenuItem>> categories = currentMenu.getCategories();
        List<MenuItem> newItems = categories.get(newCategory);
        if (newItems == null) {
            newItems = new java.util.ArrayList<>();
            categories.put(newCategory, newItems);
        }
        newItems.add(item);

        menuRepository.saveMenu(currentMenu);
    }

    public void updateSide(String itemName, String sideName, Long price, Boolean available) {
        logger.info("Updating side {} for item {}: price={}, available={}", sideName, itemName, price, available);
        MenuItem item = getItem(itemName);
        if (item.sideOptions == null || !item.sideOptions.containsKey(sideName)) {
            throw new EntityNotFoundException("Side not found: " + sideName);
        }

        Side side = item.sideOptions.get(sideName);
        if (price != null) {
            side.price = price;
        }
        if (available != null) {
            side.available = available;
        }

        menuRepository.saveMenu(currentMenu);
    }

    public void addSide(String itemName, String sideName, long price) {
        logger.info("Adding side {} to item {} with price {}", sideName, itemName, price);
        if (sideName == null || sideName.trim().isEmpty()) {
            throw new InvalidInputException("Side name cannot be empty");
        }
        if ("none".equalsIgnoreCase(sideName.trim())) {
            throw new InvalidInputException("Cannot add 'none' side manually");
        }
        if (price < 0) {
            throw new InvalidInputException("Side price cannot be negative");
        }

        MenuItem item = getItem(itemName);
        if (item.sideOptions == null) {
            item.sideOptions = new java.util.HashMap<>();
        }

        if (item.sideOptions.containsKey(sideName)) {
            throw new InvalidInputException("Side already exists: " + sideName);
        }

        if (item.sideOptions.isEmpty() || (item.sideOptions.size() == 1 && item.sideOptions.containsKey("none"))) {
            if (!item.sideOptions.containsKey("none")) {
                Side noneSide = new Side();
                noneSide.price = 0;
                noneSide.available = true;
                item.sideOptions.put("none", noneSide);
            }
        }

        Side side = new Side();
        side.price = price;
        side.available = true;
        item.sideOptions.put(sideName, side);

        menuRepository.saveMenu(currentMenu);
    }

    public void removeSide(String itemName, String sideName) {
        logger.info("Removing side {} from item {}", sideName, itemName);
        if ("none".equalsIgnoreCase(sideName)) {
            throw new InvalidInputException("Cannot remove 'none' side directly");
        }

        MenuItem item = getItem(itemName);
        if (item.sideOptions == null || !item.sideOptions.containsKey(sideName)) {
            throw new EntityNotFoundException("Side not found: " + sideName);
        }

        item.sideOptions.remove(sideName);

        if (item.sideOptions.size() == 1 && item.sideOptions.containsKey("none")) {
            item.sideOptions = null;
        } else if (item.sideOptions.isEmpty()) {
            item.sideOptions = null;
        }

        menuRepository.saveMenu(currentMenu);
    }

    public List<String> getKitchenItems() {
        return currentMenu.getKitchenItems();
    }

    public void addKitchenItem(String itemName) {
        logger.info("Adding item {} to kitchen list", itemName);
        getItem(itemName);

        if (currentMenu.addKitchenItem(itemName)) {
            menuRepository.saveMenu(currentMenu);
        }
    }

    public void removeKitchenItem(String itemName) {
        logger.info("Removing item {} from kitchen list", itemName);
        if (currentMenu.removeKitchenItem(itemName)) {
            menuRepository.saveMenu(currentMenu);
        }
    }

    public void deleteCategory(String categoryName) {
        logger.info("Deleting category {}", categoryName);
        categoryName = categoryName.toLowerCase();
        Map<String, List<MenuItem>> categories = currentMenu.getCategories();

        if (!categories.containsKey(categoryName)) {
            throw new EntityNotFoundException("Category not found: " + categoryName);
        }

        List<MenuItem> items = categories.remove(categoryName);

        if (items != null) {
            for (MenuItem item : items) {
                currentMenu.removeKitchenItem(item.name);
            }
        }

        List<String> categoryOrder = currentMenu.getCategoryOrder();
        if (categoryOrder != null) {
            categoryOrder.remove(categoryName);
        }

        menuRepository.saveMenu(currentMenu);
    }

    public List<String> getCategoryOrder() {
        return currentMenu.getCategoryOrder();
    }

    public void reorderCategories(List<String> order) {
        logger.info("Reordering categories: {}", order);
        if (order == null || order.isEmpty()) {
            throw new InvalidInputException("Order list cannot be empty");
        }

        Map<String, List<MenuItem>> categories = currentMenu.getCategories();
        for (String categoryName : order) {
            if (!categories.containsKey(categoryName.toLowerCase())) {
                throw new EntityNotFoundException("Category not found: " + categoryName);
            }
        }

        List<String> normalizedOrder = order.stream()
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toList());

        currentMenu.setCategoryOrder(normalizedOrder);
        menuRepository.saveMenu(currentMenu);
    }

    public void reorderItemsInCategory(String categoryName, List<String> order) {
        logger.info("Reordering items in category {}: {}", categoryName, order);
        if (order == null || order.isEmpty()) {
            throw new InvalidInputException("Order list cannot be empty");
        }

        categoryName = categoryName.toLowerCase();
        List<MenuItem> items = currentMenu.getCategory(categoryName);
        if (items == null) {
            throw new EntityNotFoundException("Category not found: " + categoryName);
        }

        Map<String, MenuItem> itemMap = new java.util.HashMap<>();
        for (MenuItem item : items) {
            itemMap.put(item.name, item);
        }

        for (String itemName : order) {
            if (!itemMap.containsKey(itemName)) {
                throw new EntityNotFoundException("Item not found in category: " + itemName);
            }
        }

        List<MenuItem> reorderedItems = new java.util.ArrayList<>();
        for (String itemName : order) {
            reorderedItems.add(itemMap.get(itemName));
        }

        currentMenu.getCategories().put(categoryName, reorderedItems);
        menuRepository.saveMenu(currentMenu);
    }

    public void reorderSidesInItem(String itemName, List<String> order) {
        logger.info("Reordering sides in item {}: {}", itemName, order);
        if (order == null || order.isEmpty()) {
            throw new InvalidInputException("Order list cannot be empty");
        }

        MenuItem item = getItem(itemName);
        if (item.sideOptions == null || item.sideOptions.isEmpty()) {
            throw new EntityNotFoundException("Item has no sides: " + itemName);
        }

        for (String sideName : order) {
            if (!item.sideOptions.containsKey(sideName)) {
                throw new EntityNotFoundException("Side not found: " + sideName);
            }
        }

        item.sideOrder = order;
        menuRepository.saveMenu(currentMenu);
    }

}
