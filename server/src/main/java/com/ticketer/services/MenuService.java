package com.ticketer.services;

import com.ticketer.models.Menu;
import com.ticketer.models.OrderItem;
import com.ticketer.models.Ticket;
import com.ticketer.repositories.MenuRepository;
import com.ticketer.models.Extra;
import com.ticketer.models.MenuAddon;
import com.ticketer.models.MenuItem;
import com.ticketer.models.MenuItemView;
import com.ticketer.models.Side;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.InvalidInputException;
import com.ticketer.dtos.KitchenTicketDto;
import com.ticketer.dtos.KitchenOrderGroupDto;
import com.ticketer.dtos.KitchenItemDto;

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
    private static final String NONE_OPTION = "none";

    private <T extends MenuAddon> void addAddon(String category, String itemName, String addonName, long price, java.util.Map<String, T> options, java.util.function.Supplier<T> factory, java.util.function.Consumer<java.util.Map<String, T>> optionsSetter) {
        if (addonName == null || addonName.trim().isEmpty()) {
            throw new InvalidInputException("Addon name cannot be empty");
        }
        if (NONE_OPTION.equalsIgnoreCase(addonName.trim())) {
            throw new InvalidInputException("Cannot add 'none' option manually");
        }
        if (price < 0) {
            throw new InvalidInputException("Price cannot be negative");
        }

        MenuItem item = findItem(category, itemName);
        java.util.Map<String, T> targetOptions = options;
        if (targetOptions == null) {
            targetOptions = new java.util.HashMap<>();
            optionsSetter.accept(targetOptions);
        }

        if (targetOptions.containsKey(addonName)) {
            throw new InvalidInputException("Addon already exists: " + addonName);
        }

        if (targetOptions.isEmpty() || (targetOptions.size() == 1 && targetOptions.containsKey(NONE_OPTION))) {
            if (!targetOptions.containsKey(NONE_OPTION)) {
                T noneOption = factory.get();
                noneOption.price = 0;
                noneOption.available = true;
                targetOptions.put(NONE_OPTION, noneOption);
            }
        }

        T newAddon = factory.get();
        newAddon.price = price;
        newAddon.available = true;
        targetOptions.put(addonName, newAddon);
    }

    private <T extends MenuAddon> void updateAddon(String category, String itemName, String addonName, java.util.Map<String, T> options, Long price, Boolean available, Boolean kitchen) {
        if (options == null || !options.containsKey(addonName)) {
            throw new EntityNotFoundException("Addon not found: " + addonName);
        }

        T addon = options.get(addonName);
        if (price != null) {
            addon.price = price;
        }
        if (available != null) {
            addon.available = available;
        }
        if (kitchen != null) {
            addon.kitchen = kitchen;
        }
    }

    private <T extends MenuAddon> void removeAddon(String category, String itemName, String addonName, java.util.Map<String, T> options, java.util.function.Consumer<java.util.Map<String, T>> optionsSetter) {
        if (NONE_OPTION.equalsIgnoreCase(addonName)) {
            throw new InvalidInputException("Cannot remove 'none' option directly");
        }

        if (options == null || !options.containsKey(addonName)) {
            throw new EntityNotFoundException("Addon not found: " + addonName);
        }

        options.remove(addonName);

        if (options.size() == 1 && options.containsKey(NONE_OPTION)) {
            optionsSetter.accept(null);
        } else if (options.isEmpty()) {
            optionsSetter.accept(null);
        }
    }


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
            MenuItem item = findItem(category, name);
            return new MenuItem(item);
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
            
            long sidePrice = 0;
            if (sideName != null && !NONE_OPTION.equalsIgnoreCase(sideName) && item.sideOptions != null && item.sideOptions.containsKey(sideName)) {
                sidePrice = item.sideOptions.get(sideName).price;
            }

            long extraPrice = 0;
            if (extraName != null && !NONE_OPTION.equalsIgnoreCase(extraName) && item.extraOptions != null && item.extraOptions.containsKey(extraName)) {
                extraPrice = item.extraOptions.get(extraName).price;
            }

            return new OrderItem(category, item.name, sideName, extraName, item.price, sidePrice, extraPrice, null);
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
            List<MenuItem> copy = new java.util.ArrayList<>();
            for (MenuItem item : items) {
                copy.add(new MenuItem(item));
            }
            return copy;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, List<MenuItem>> getCategories() {
        lock.readLock().lock();
        try {
            Map<String, List<MenuItem>> copy = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, List<MenuItem>> entry : currentMenu.getCategories().entrySet()) {
                List<MenuItem> listCopy = new java.util.ArrayList<>();
                for (MenuItem item : entry.getValue()) {
                    listCopy.add(new MenuItem(item));
                }
                copy.put(entry.getKey(), listCopy);
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
            currentMenu.addItem(category, newItem);
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
            findItem(category, oldName);
            
            currentMenu.renameItem(oldName, newName);
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeItem(String category, String itemName) {
        logger.info("Removing item {} from category {}", itemName, category);
        lock.writeLock().lock();
        try {
            boolean removed = currentMenu.removeItem(category.toLowerCase(), itemName);

            if (!removed) {
                throw new EntityNotFoundException("Item not found: " + itemName + " in category: " + category);
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

            findItem(oldCategory, itemName);

            currentMenu.changeCategory(oldCategory, newCategory, itemName);

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
             updateAddon(category, itemName, sideName, item.sideOptions, price, available, kitchen);
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
        lock.writeLock().lock();
        try {
            MenuItem item = findItem(category, itemName);
            addAddon(category, itemName, sideName, price, item.sideOptions, Side::new, map -> item.sideOptions = map);
            
            if (item.sideOrder == null) {
                 item.sideOrder = new java.util.ArrayList<>(item.sideOptions.keySet());
            } else {
                 if (!item.sideOrder.contains(sideName)) {
                     item.sideOrder.add(sideName);
                 }
            }
            if (item.sideOrder.contains(NONE_OPTION)) {
                item.sideOrder.remove(NONE_OPTION);
                item.sideOrder.add(NONE_OPTION);
            }
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeSide(String category, String itemName, String sideName) {
        logger.info("Removing side {} from item {} in {}", sideName, itemName, category);
        lock.writeLock().lock();
        try {
             MenuItem item = findItem(category, itemName);
             removeAddon(category, itemName, sideName, item.sideOptions, map -> item.sideOptions = map);
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

            if (item.sideOptions.containsKey(NONE_OPTION)) {
                newOrder.remove(NONE_OPTION);
                newOrder.add(NONE_OPTION);
            }

            item.sideOrder = newOrder;
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addExtra(String category, String itemName, String extraName, long price) {
        logger.info("Adding extra {} to item {} in {} with price {}", extraName, itemName, category, price);
        lock.writeLock().lock();
        try {
             MenuItem item = findItem(category, itemName);
             addAddon(category, itemName, extraName, price, item.extraOptions, Extra::new, map -> item.extraOptions = map);
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
             updateAddon(category, itemName, extraName, item.extraOptions, price, available, kitchen);
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
        lock.writeLock().lock();
        try {
             MenuItem item = findItem(category, itemName);
             removeAddon(category, itemName, extraName, item.extraOptions, map -> item.extraOptions = map);
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
             MenuItem menuItem = currentMenu.getItem(orderItem.getName());
             
             if (menuItem != null) {
                 if (menuItem.kitchen) return true;
                 if (orderItem.getSelectedSide() != null && menuItem.sideOptions != null) {
                     Side side = menuItem.sideOptions.get(orderItem.getSelectedSide());
                     if (side != null && side.kitchen) return true;
                 }
                 if (orderItem.getSelectedExtra() != null && menuItem.extraOptions != null) {
                     Extra extra = menuItem.extraOptions.get(orderItem.getSelectedExtra());
                     if (extra != null && extra.kitchen) return true;
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
                    MenuItem menuItem = currentMenu.getItem(item.getName());
                    if (menuItem == null) continue;

                    if (menuItem.kitchen) {
                        tally.merge(item.getName(), 1, Integer::sum);
                    }
                    if (item.getSelectedSide() != null && !NONE_OPTION.equalsIgnoreCase(item.getSelectedSide())
                            && menuItem.sideOptions != null) {
                        Side side = menuItem.sideOptions.get(item.getSelectedSide());
                        if (side != null && side.kitchen) {
                            tally.merge(item.getSelectedSide(), 1, Integer::sum);
                        }
                    }
                    if (item.getSelectedExtra() != null && !NONE_OPTION.equalsIgnoreCase(item.getSelectedExtra())
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

    public KitchenTicketDto getKitchenDetails(Ticket ticket) {
        lock.readLock().lock();
        try {
            java.util.Map<String, Integer> tally = new java.util.LinkedHashMap<>();
            java.util.List<KitchenOrderGroupDto> kitchenOrders = new java.util.ArrayList<>();

            for (com.ticketer.models.Order order : ticket.getOrders()) {
                for (OrderItem item : order.getItems()) {
                    MenuItem menuItem = currentMenu.getItem(item.getName());
                    if (menuItem == null) continue;

                    if (menuItem.kitchen) {
                        tally.merge(item.getName(), 1, Integer::sum);
                    }
                    if (item.getSelectedSide() != null && !NONE_OPTION.equalsIgnoreCase(item.getSelectedSide())
                            && menuItem.sideOptions != null) {
                        Side side = menuItem.sideOptions.get(item.getSelectedSide());
                        if (side != null && side.kitchen) {
                            tally.merge(item.getSelectedSide(), 1, Integer::sum);
                        }
                    }
                    if (item.getSelectedExtra() != null && !NONE_OPTION.equalsIgnoreCase(item.getSelectedExtra())
                            && menuItem.extraOptions != null) {
                        Extra extra = menuItem.extraOptions.get(item.getSelectedExtra());
                        if (extra != null && extra.kitchen) {
                            tally.merge(item.getSelectedExtra(), 1, Integer::sum);
                        }
                    }
                }
            }

            for (com.ticketer.models.Order order : ticket.getOrders()) {
                List<KitchenItemDto> groupItems = new java.util.ArrayList<>();
                java.util.Map<String, java.util.Map<String, java.util.Map<String, Integer>>> grouped = new java.util.LinkedHashMap<>();

                for (OrderItem item : order.getItems()) {
                    boolean relevant = false;
                    MenuItem menuItem = null;
                    if (item.getCategory() != null) {
                        try {
                            menuItem = findItem(item.getCategory(), item.getName());
                        } catch (EntityNotFoundException e) {
                        }
                    }
                    
                    if (menuItem != null) {
                        if (menuItem.kitchen) relevant = true;
                        else if (item.getSelectedSide() != null && !NONE_OPTION.equalsIgnoreCase(item.getSelectedSide()) && menuItem.sideOptions != null) {
                            Side side = menuItem.sideOptions.get(item.getSelectedSide());
                            if (side != null && side.kitchen) relevant = true;
                        }
                        else if (item.getSelectedExtra() != null && !NONE_OPTION.equalsIgnoreCase(item.getSelectedExtra()) && menuItem.extraOptions != null) {
                            Extra extra = menuItem.extraOptions.get(item.getSelectedExtra());
                            if (extra != null && extra.kitchen) relevant = true;
                        }
                    }
                    
                    if (!relevant) continue;

                    if (item.getComment() != null && !item.getComment().trim().isEmpty()) {
                        groupItems.add(new KitchenItemDto(item.getName(), item.getSelectedSide(), item.getSelectedExtra(), 1, item.getComment()));
                    } else {
                        String sideKey = item.getSelectedSide() != null ? item.getSelectedSide() : "";
                        String extraKey = item.getSelectedExtra() != null ? item.getSelectedExtra() : "";
                        grouped.computeIfAbsent(item.getName(), k -> new java.util.LinkedHashMap<>())
                               .computeIfAbsent(sideKey, k -> new java.util.LinkedHashMap<>())
                               .merge(extraKey, 1, Integer::sum);
                    }
                }

                for (java.util.Map.Entry<String, java.util.Map<String, java.util.Map<String, Integer>>> nameEntry : grouped.entrySet()) {
                    for (java.util.Map.Entry<String, java.util.Map<String, Integer>> sideEntry : nameEntry.getValue().entrySet()) {
                        String side = sideEntry.getKey().isEmpty() ? null : sideEntry.getKey();
                        for (java.util.Map.Entry<String, Integer> extraEntry : sideEntry.getValue().entrySet()) {
                            String extra = extraEntry.getKey().isEmpty() ? null : extraEntry.getKey();
                            groupItems.add(new KitchenItemDto(nameEntry.getKey(), side, extra, extraEntry.getValue(), null));
                        }
                    }
                }

                if (!groupItems.isEmpty() || (order.getComment() != null && !order.getComment().trim().isEmpty())) {
                    kitchenOrders.add(new KitchenOrderGroupDto(order.getComment(), groupItems));
                }
            }

            return new KitchenTicketDto(
                    ticket.getId(),
                    ticket.getTableNumber(),
                    tally,
                    kitchenOrders,
                    ticket.getCreatedAt() != null ? ticket.getCreatedAt().toString() : null,
                    ticket.getComment());

        } finally {
            lock.readLock().unlock();
        }
    }


}
