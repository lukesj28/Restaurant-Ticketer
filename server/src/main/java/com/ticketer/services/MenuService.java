package com.ticketer.services;

import com.ticketer.models.Menu;
import com.ticketer.models.OrderItem;
import com.ticketer.models.Ticket;
import com.ticketer.repositories.MenuRepository;
import com.ticketer.models.Extra;
import com.ticketer.models.MenuItem;
import com.ticketer.models.MenuItemView;
import com.ticketer.models.Side;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.InvalidInputException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MenuService.class);

    private final MenuRepository menuRepository;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Menu currentMenu;

    @Autowired
    public MenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
        this.currentMenu = menuRepository.getMenu();
    }

    public void refreshMenu() {
        lock.writeLock().lock();
        try {
            this.currentMenu = menuRepository.getMenu();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MenuItem getItem(String category, String name) {
        lock.readLock().lock();
        try {
            return findItem(category, name);
        } finally {
            lock.readLock().unlock();
        }
    }

    private MenuItem findItem(String category, String name) {
        List<MenuItem> items = currentMenu.getCategory(category.toLowerCase());
        if (items != null) {
            for (MenuItem item : items) {
                if (item.name.equals(name)) {
                    return item;
                }
            }
        }
        throw new EntityNotFoundException("Item not found: " + name + " in category: " + category);
    }

    public OrderItem createOrderItem(String category, String itemName, String sideName, String extraName) {
        lock.readLock().lock();
        try {
            MenuItem item = findItem(category, itemName);
            return Menu.getItem(item, sideName, extraName);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<MenuItem> getCategory(String categoryName) {
        lock.readLock().lock();
        try {
            List<MenuItem> items = currentMenu.getCategory(categoryName.toLowerCase());
            if (items == null) {
                throw new EntityNotFoundException("Category not found: " + categoryName);
            }
            return new java.util.ArrayList<>(items);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, List<MenuItem>> getCategories() {
        lock.readLock().lock();
        try {
            Map<String, List<MenuItem>> copy = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, List<MenuItem>> entry : currentMenu.getCategories().entrySet()) {
                copy.put(entry.getKey(), new java.util.ArrayList<>(entry.getValue()));
            }
            return copy;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<MenuItemView> getAllItems() {
        lock.readLock().lock();
        try {
            return currentMenu.getAllItems();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addItem(String category, String name, long price, Map<String, Long> sides, boolean kitchen) {
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
        lock.writeLock().lock();
        try {
            Map<String, List<MenuItem>> categories = currentMenu.getCategories();

            List<MenuItem> items = categories.get(category);
            if (items == null) {
                items = new java.util.ArrayList<>();
                categories.put(category, items);
                if (!currentMenu.getCategoryOrder().contains(category)) {
                    currentMenu.getCategoryOrder().add(category);
                }
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

            MenuItem newItem = new MenuItem(name, price, true, sideObjects, null, null, null);
            newItem.kitchen = kitchen;
            items.add(newItem);
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addItem(String category, String name, long price, Map<String, Long> sides) {
        addItem(category, name, price, sides, false);
    }

    public void editItemPrice(String category, String itemName, long newPrice) {
        logger.info("Editing price for item {} in category {} to {}", itemName, category, newPrice);
        if (newPrice < 0) {
            throw new InvalidInputException("Price cannot be negative");
        }
        lock.writeLock().lock();
        try {
            MenuItem item = findItem(category, itemName);
            item.price = newPrice;
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void editItemAvailability(String category, String itemName, boolean available) {
        logger.info("Setting availability for item {} in category {} to {}", itemName, category, available);
        lock.writeLock().lock();
        try {
            MenuItem item = findItem(category, itemName);
            item.available = available;
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void editItemKitchen(String category, String itemName, boolean kitchen) {
        logger.info("Setting kitchen for item {} in category {} to {}", itemName, category, kitchen);
        lock.writeLock().lock();
        try {
            MenuItem item = findItem(category, itemName);
            item.kitchen = kitchen;
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void renameItem(String category, String oldName, String newName) {
        logger.info("Renaming item {} in category {} to {}", oldName, category, newName);
        if (newName == null || newName.trim().isEmpty()) {
            throw new InvalidInputException("New name cannot be empty");
        }
        lock.writeLock().lock();
        try {
            MenuItem item = findItem(category, oldName);
            item.name = newName;
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeItem(String category, String itemName) {
        logger.info("Removing item {} from category {}", itemName, category);
        lock.writeLock().lock();
        try {
            List<MenuItem> items = currentMenu.getCategory(category.toLowerCase());
            if (items == null) {
                throw new EntityNotFoundException("Category not found: " + category);
            }

            boolean removed = items.removeIf(i -> i.name.equals(itemName));

            if (!removed) {
                throw new EntityNotFoundException("Item not found: " + itemName + " in category: " + category);
            }

            if (items.isEmpty()) {
                currentMenu.getCategories().remove(category.toLowerCase());
            }

            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void renameCategory(String oldCategory, String newCategory) {
        logger.info("Renaming category {} to {}", oldCategory, newCategory);
        if (newCategory == null || newCategory.trim().isEmpty()) {
            throw new InvalidInputException("New category name cannot be empty");
        }
        oldCategory = oldCategory.toLowerCase();
        newCategory = newCategory.toLowerCase();

        lock.writeLock().lock();
        try {
            Map<String, List<MenuItem>> categories = currentMenu.getCategories();

            if (!categories.containsKey(oldCategory)) {
                throw new EntityNotFoundException("Category not found: " + oldCategory);
            }

            List<MenuItem> items = categories.remove(oldCategory);
            boolean requestMerge = categories.containsKey(newCategory);

            if (requestMerge) {
                categories.get(newCategory).addAll(items);
            } else {
                categories.put(newCategory, items);
            }

            List<String> order = currentMenu.getCategoryOrder();
            if (order != null) {
                int index = order.indexOf(oldCategory);
                if (index != -1) {
                    if (requestMerge) {
                        order.remove(index);
                    } else {
                        order.set(index, newCategory);
                    }
                } else {
                    if (!order.contains(newCategory)) {
                        order.add(newCategory);
                    }
                }
            }

            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void changeCategory(String category, String itemName, String newCategory) {
        logger.info("Changing category for item {} in {} to {}", itemName, category, newCategory);
        if (newCategory == null || newCategory.trim().isEmpty()) {
            throw new InvalidInputException("New category name cannot be empty");
        }

        lock.writeLock().lock();
        try {
            String oldCategory = category.toLowerCase();
            newCategory = newCategory.toLowerCase();

            if (oldCategory.equals(newCategory))
                return;

            List<MenuItem> oldItems = currentMenu.getCategory(oldCategory);
            MenuItem item = null;
            if (oldItems != null) {
                for (MenuItem i : oldItems) {
                    if (i.name.equals(itemName)) {
                        item = i;
                        break;
                    }
                }
            }

            if (item == null) {
                throw new EntityNotFoundException("Item not found: " + itemName + " in category: " + category);
            }

            if (oldItems != null) {
                oldItems.remove(item);
            }

            if (oldItems != null && oldItems.isEmpty()) {
                currentMenu.getCategories().remove(oldCategory);
            }

            Map<String, List<MenuItem>> categories = currentMenu.getCategories();
            List<MenuItem> newItems = categories.get(newCategory);
            if (newItems == null) {
                newItems = new java.util.ArrayList<>();
                categories.put(newCategory, newItems);
                if (!currentMenu.getCategoryOrder().contains(newCategory)) {
                    currentMenu.getCategoryOrder().add(newCategory);
                }
            }
            newItems.add(item);

            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateSide(String category, String itemName, String sideName, Long price, Boolean available, Boolean kitchen) {
        logger.info("Updating side {} for item {} in {}: price={}, available={}, kitchen={}", sideName, itemName, category, price, available, kitchen);
        lock.writeLock().lock();
        try {
            MenuItem item = findItem(category, itemName);
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
            if (kitchen != null) {
                side.kitchen = kitchen;
            }

            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateSide(String category, String itemName, String sideName, Long price, Boolean available) {
        updateSide(category, itemName, sideName, price, available, null);
    }

    public void addSide(String category, String itemName, String sideName, long price) {
        logger.info("Adding side {} to item {} in {} with price {}", sideName, itemName, category, price);
        if (sideName == null || sideName.trim().isEmpty()) {
            throw new InvalidInputException("Side name cannot be empty");
        }
        if ("none".equalsIgnoreCase(sideName.trim())) {
            throw new InvalidInputException("Cannot add 'none' side manually");
        }
        if (price < 0) {
            throw new InvalidInputException("Side price cannot be negative");
        }

        lock.writeLock().lock();
        try {
            MenuItem item = findItem(category, itemName);
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

            if (item.sideOrder == null) {
                item.sideOrder = new java.util.ArrayList<>(item.sideOptions.keySet());
            } else {
                if (!item.sideOrder.contains(sideName)) {
                    item.sideOrder.add(sideName);
                }
            }

            if (item.sideOrder.contains("none")) {
                item.sideOrder.remove("none");
                item.sideOrder.add("none");
            }

            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeSide(String category, String itemName, String sideName) {
        logger.info("Removing side {} from item {} in {}", sideName, itemName, category);
        if ("none".equalsIgnoreCase(sideName)) {
            throw new InvalidInputException("Cannot remove 'none' side directly");
        }

        lock.writeLock().lock();
        try {
            MenuItem item = findItem(category, itemName);
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
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteCategory(String categoryName) {
        logger.info("Deleting category {}", categoryName);
        categoryName = categoryName.toLowerCase();

        lock.writeLock().lock();
        try {
            Map<String, List<MenuItem>> categories = currentMenu.getCategories();

            if (!categories.containsKey(categoryName)) {
                throw new EntityNotFoundException("Category not found: " + categoryName);
            }

            categories.remove(categoryName);

            List<String> categoryOrder = currentMenu.getCategoryOrder();
            if (categoryOrder != null) {
                categoryOrder.remove(categoryName);
            }

            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> getCategoryOrder() {
        lock.readLock().lock();
        try {
            return new java.util.ArrayList<>(currentMenu.getCategoryOrder());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void reorderCategories(List<String> order) {
        logger.info("Reordering categories: {}", order);
        if (order == null || order.isEmpty()) {
            throw new InvalidInputException("Order list cannot be empty");
        }

        lock.writeLock().lock();
        try {
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
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void reorderItemsInCategory(String categoryName, List<String> order) {
        logger.info("Reordering items in category {}: {}", categoryName, order);
        if (order == null || order.isEmpty()) {
            throw new InvalidInputException("Order list cannot be empty");
        }

        categoryName = categoryName.toLowerCase();

        lock.writeLock().lock();
        try {
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
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void reorderSidesInItem(String category, String itemName, List<String> order) {
        logger.info("Reordering sides in item {} in {}: {}", itemName, category, order);
        if (order == null || order.isEmpty()) {
            throw new InvalidInputException("Order list cannot be empty");
        }

        lock.writeLock().lock();
        try {
            MenuItem item = findItem(category, itemName);
            if (item.sideOptions == null || item.sideOptions.isEmpty()) {
                throw new EntityNotFoundException("Item has no sides: " + itemName);
            }

            for (String sideName : order) {
                if (!item.sideOptions.containsKey(sideName)) {
                    throw new EntityNotFoundException("Side not found: " + sideName);
                }
            }

            List<String> newOrder = new java.util.ArrayList<>(order);

            if (item.sideOptions.containsKey("none")) {
                newOrder.remove("none");
                newOrder.add("none");
            }

            item.sideOrder = newOrder;
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addExtra(String category, String itemName, String extraName, long price) {
        logger.info("Adding extra {} to item {} in {} with price {}", extraName, itemName, category, price);
        if (extraName == null || extraName.trim().isEmpty()) {
            throw new InvalidInputException("Extra name cannot be empty");
        }
        if ("none".equalsIgnoreCase(extraName.trim())) {
            throw new InvalidInputException("Cannot add 'none' extra manually");
        }
        if (price < 0) {
            throw new InvalidInputException("Extra price cannot be negative");
        }

        lock.writeLock().lock();
        try {
            MenuItem item = findItem(category, itemName);
            if (item.extraOptions == null) {
                item.extraOptions = new java.util.HashMap<>();
            }

            if (item.extraOptions.containsKey(extraName)) {
                throw new InvalidInputException("Extra already exists: " + extraName);
            }

            if (item.extraOptions.isEmpty() || (item.extraOptions.size() == 1 && item.extraOptions.containsKey("none"))) {
                if (!item.extraOptions.containsKey("none")) {
                    Extra noneExtra = new Extra();
                    noneExtra.price = 0;
                    noneExtra.available = true;
                    item.extraOptions.put("none", noneExtra);
                }
            }

            Extra extra = new Extra();
            extra.price = price;
            extra.available = true;
            item.extraOptions.put(extraName, extra);

            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateExtra(String category, String itemName, String extraName, Long price, Boolean available, Boolean kitchen) {
        logger.info("Updating extra {} for item {} in {}: price={}, available={}, kitchen={}", extraName, itemName, category, price, available, kitchen);
        lock.writeLock().lock();
        try {
            MenuItem item = findItem(category, itemName);
            if (item.extraOptions == null || !item.extraOptions.containsKey(extraName)) {
                throw new EntityNotFoundException("Extra not found: " + extraName);
            }

            Extra extra = item.extraOptions.get(extraName);
            if (price != null) {
                extra.price = price;
            }
            if (available != null) {
                extra.available = available;
            }
            if (kitchen != null) {
                extra.kitchen = kitchen;
            }

            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateExtra(String category, String itemName, String extraName, Long price, Boolean available) {
        updateExtra(category, itemName, extraName, price, available, null);
    }

    public void removeExtra(String category, String itemName, String extraName) {
        logger.info("Removing extra {} from item {} in {}", extraName, itemName, category);
        if ("none".equalsIgnoreCase(extraName)) {
            throw new InvalidInputException("Cannot remove 'none' extra directly");
        }

        lock.writeLock().lock();
        try {
            MenuItem item = findItem(category, itemName);
            if (item.extraOptions == null || !item.extraOptions.containsKey(extraName)) {
                throw new EntityNotFoundException("Extra not found: " + extraName);
            }

            item.extraOptions.remove(extraName);

            if (item.extraOptions.size() == 1 && item.extraOptions.containsKey("none")) {
                item.extraOptions = null;
            } else if (item.extraOptions.isEmpty()) {
                item.extraOptions = null;
            }

            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void reorderExtrasInItem(String category, String itemName, List<String> order) {
        logger.info("Reordering extras in item {} in {}: {}", itemName, category, order);
        if (order == null || order.isEmpty()) {
            throw new InvalidInputException("Order list cannot be empty");
        }

        lock.writeLock().lock();
        try {
            MenuItem item = findItem(category, itemName);
            if (item.extraOptions == null || item.extraOptions.isEmpty()) {
                throw new EntityNotFoundException("Item has no extras: " + itemName);
            }

            for (String extraName : order) {
                if (!item.extraOptions.containsKey(extraName)) {
                    throw new EntityNotFoundException("Extra not found: " + extraName);
                }
            }

            item.extraOrder = order;
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isKitchenRelevant(OrderItem orderItem) {
        lock.readLock().lock();
        try {
            for (Map.Entry<String, List<MenuItem>> entry : currentMenu.getCategories().entrySet()) {
                for (MenuItem menuItem : entry.getValue()) {
                    if (menuItem.name.equals(orderItem.getName())) {
                        if (menuItem.kitchen) return true;
                        if (orderItem.getSelectedSide() != null && menuItem.sideOptions != null) {
                            Side side = menuItem.sideOptions.get(orderItem.getSelectedSide());
                            if (side != null && side.kitchen) return true;
                        }
                        if (orderItem.getSelectedExtra() != null && menuItem.extraOptions != null) {
                            Extra extra = menuItem.extraOptions.get(orderItem.getSelectedExtra());
                            if (extra != null && extra.kitchen) return true;
                        }
                        return false;
                    }
                }
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    public java.util.Map<String, Integer> getKitchenTally(Ticket ticket) {
        lock.readLock().lock();
        try {
            java.util.Map<String, Integer> tally = new java.util.LinkedHashMap<>();
            for (com.ticketer.models.Order order : ticket.getOrders()) {
                for (OrderItem item : order.getItems()) {
                    MenuItem menuItem = findMenuItemByName(item.getName());
                    if (menuItem == null) continue;

                    if (menuItem.kitchen) {
                        tally.merge(item.getName(), 1, Integer::sum);
                    }
                    if (item.getSelectedSide() != null && !"none".equalsIgnoreCase(item.getSelectedSide())
                            && menuItem.sideOptions != null) {
                        Side side = menuItem.sideOptions.get(item.getSelectedSide());
                        if (side != null && side.kitchen) {
                            tally.merge(item.getSelectedSide(), 1, Integer::sum);
                        }
                    }
                    if (item.getSelectedExtra() != null && !"none".equalsIgnoreCase(item.getSelectedExtra())
                            && menuItem.extraOptions != null) {
                        Extra extra = menuItem.extraOptions.get(item.getSelectedExtra());
                        if (extra != null && extra.kitchen) {
                            tally.merge(item.getSelectedExtra(), 1, Integer::sum);
                        }
                    }
                }
            }
            return tally;
        } finally {
            lock.readLock().unlock();
        }
    }

    private MenuItem findMenuItemByName(String name) {
        for (List<MenuItem> items : currentMenu.getCategories().values()) {
            for (MenuItem item : items) {
                if (item.name.equals(name)) {
                    return item;
                }
            }
        }
        return null;
    }
}
