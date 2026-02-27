package com.ticketer.services;

import com.ticketer.dtos.KitchenItemDto;
import com.ticketer.dtos.KitchenOrderGroupDto;
import com.ticketer.dtos.KitchenTicketDto;
import com.ticketer.dtos.Requests.SlotSelectionRequest;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.InvalidInputException;
import com.ticketer.models.BaseItem;
import com.ticketer.models.CategoryEntry;
import com.ticketer.models.ComboComponentSnapshot;
import com.ticketer.models.ComboItem;
import com.ticketer.models.ComboSlot;
import com.ticketer.models.ComboSlotSelection;
import com.ticketer.models.Menu;
import com.ticketer.models.MenuItem;
import com.ticketer.models.Order;
import com.ticketer.models.OrderItem;
import com.ticketer.models.Ticket;
import com.ticketer.repositories.MenuRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

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

    public Menu getMenu() {
        lock.readLock().lock();
        try {
            return currentMenu;
        } finally {
            lock.readLock().unlock();
        }
    }

    public BaseItem getBaseItem(UUID id) {
        lock.readLock().lock();
        try {
            BaseItem item = currentMenu.getBaseItem(id);
            if (item == null) throw new EntityNotFoundException("Base item not found: " + id);
            return item;
        } finally {
            lock.readLock().unlock();
        }
    }

    public BaseItem createBaseItem(String name, long price, boolean kitchen) {
        return createBaseItem(name, price, kitchen, null);
    }

    public BaseItem createBaseItem(String name, long price, boolean kitchen,
            List<com.ticketer.models.CompositeComponent> components) {
        if (name == null || name.trim().isEmpty())
            throw new InvalidInputException("Name cannot be empty");
        if (price < 0)
            throw new InvalidInputException("Price cannot be negative");

        lock.writeLock().lock();
        try {
            UUID id = UUID.randomUUID();
            BaseItem item = new BaseItem(id, name.trim(), price, true, kitchen, components);
            currentMenu.addBaseItem(item);
            menuRepository.saveMenu(currentMenu);
            return item;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateBaseItemPrice(UUID id, long price) {
        if (price < 0) throw new InvalidInputException("Price cannot be negative");
        lock.writeLock().lock();
        try {
            BaseItem item = requireBaseItem(id);
            item.setPrice(price);
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateBaseItemAvailability(UUID id, boolean available) {
        lock.writeLock().lock();
        try {
            requireBaseItem(id).setAvailable(available);
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateBaseItemKitchen(UUID id, boolean kitchen) {
        lock.writeLock().lock();
        try {
            requireBaseItem(id).setKitchen(kitchen);
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void renameBaseItem(UUID id, String newName) {
        if (newName == null || newName.trim().isEmpty())
            throw new InvalidInputException("New name cannot be empty");
        lock.writeLock().lock();
        try {
            requireBaseItem(id).setName(newName.trim());
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteBaseItem(UUID id) {
        lock.writeLock().lock();
        try {
            if (!currentMenu.removeBaseItem(id))
                throw new EntityNotFoundException("Base item not found: " + id);
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> getCategoryOrder() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(currentMenu.getCategoryOrder());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setCategoryVisible(String name, boolean visible) {
        lock.writeLock().lock();
        try {
            CategoryEntry entry = currentMenu.getCategory(name);
            if (entry == null) throw new EntityNotFoundException("Category not found: " + name);
            entry.setVisible(visible);
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void renameCategory(String oldName, String newName) {
        if (newName == null || newName.trim().isEmpty())
            throw new InvalidInputException("New category name cannot be empty");
        lock.writeLock().lock();
        try {
            if (currentMenu.getCategory(oldName) == null)
                throw new EntityNotFoundException("Category not found: " + oldName);
            currentMenu.renameCategory(oldName, newName.trim());
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteCategory(String name) {
        lock.writeLock().lock();
        try {
            if (!currentMenu.removeCategory(name))
                throw new EntityNotFoundException("Category not found: " + name);
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void reorderCategories(List<String> order) {
        if (order == null || order.isEmpty())
            throw new InvalidInputException("Order list cannot be empty");
        lock.writeLock().lock();
        try {
            for (String cat : order) {
                if (currentMenu.getCategory(cat) == null)
                    throw new EntityNotFoundException("Category not found: " + cat);
            }
            currentMenu.setCategoryOrder(new ArrayList<>(order));
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addMenuItemToCategory(String category, UUID baseItemId, List<String> sideSources) {
        lock.writeLock().lock();
        try {
            requireBaseItem(baseItemId);
            CategoryEntry entry = currentMenu.getCategory(category);
            if (entry == null) {
                entry = new CategoryEntry(true, new ArrayList<>());
                currentMenu.addCategory(category, entry);
            }
            boolean exists = entry.getItems().stream()
                    .anyMatch(mi -> baseItemId.equals(mi.getBaseItemId()));
            if (exists)
                throw new InvalidInputException("Item already in category: " + category);
            currentMenu.addMenuItem(category, new MenuItem(baseItemId,
                    sideSources != null ? sideSources : Collections.emptyList()));
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeMenuItemFromCategory(String category, UUID baseItemId) {
        lock.writeLock().lock();
        try {
            if (!currentMenu.removeMenuItem(category, baseItemId))
                throw new EntityNotFoundException("Item not found in category: " + category);
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void setSideSources(String category, UUID baseItemId, List<String> sideSources) {
        lock.writeLock().lock();
        try {
            MenuItem mi = currentMenu.findMenuItem(category, baseItemId);
            if (mi == null)
                throw new EntityNotFoundException("Item not found in category: " + category);
            mi.setSideSources(sideSources != null ? sideSources : Collections.emptyList());
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void reorderItemsInCategory(String category, List<UUID> order) {
        if (order == null || order.isEmpty())
            throw new InvalidInputException("Order list cannot be empty");
        lock.writeLock().lock();
        try {
            CategoryEntry entry = currentMenu.getCategory(category);
            if (entry == null)
                throw new EntityNotFoundException("Category not found: " + category);
            Map<UUID, MenuItem> itemMap = new LinkedHashMap<>();
            for (MenuItem mi : entry.getItems()) {
                itemMap.put(mi.getBaseItemId(), mi);
            }
            for (UUID id : order) {
                if (!itemMap.containsKey(id))
                    throw new EntityNotFoundException("Item not in category: " + id);
            }
            List<MenuItem> reordered = order.stream()
                    .map(itemMap::get)
                    .collect(Collectors.toList());
            entry.setItems(reordered);
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void moveMenuItemToCategory(String fromCategory, UUID baseItemId, String toCategory) {
        if (toCategory == null || toCategory.trim().isEmpty())
            throw new InvalidInputException("Target category cannot be empty");
        lock.writeLock().lock();
        try {
            MenuItem mi = currentMenu.findMenuItem(fromCategory, baseItemId);
            if (mi == null)
                throw new EntityNotFoundException("Item not found in category: " + fromCategory);
            currentMenu.removeMenuItem(fromCategory, baseItemId);
            currentMenu.addMenuItem(toCategory, mi);
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ComboItem getCombo(UUID id) {
        lock.readLock().lock();
        try {
            ComboItem combo = currentMenu.getCombo(id);
            if (combo == null) throw new EntityNotFoundException("Combo not found: " + id);
            return combo;
        } finally {
            lock.readLock().unlock();
        }
    }

    public ComboItem createCombo(String name, String category, List<UUID> componentIds,
            List<com.ticketer.dtos.Requests.ComboSlotRequest> slotRequests, Long price, boolean kitchen) {
        if (name == null || name.trim().isEmpty())
            throw new InvalidInputException("Combo name cannot be empty");
        lock.writeLock().lock();
        try {
            if (componentIds != null) {
                for (UUID cid : componentIds) requireBaseItem(cid);
            }
            List<ComboSlot> slots = new ArrayList<>();
            if (slotRequests != null) {
                for (com.ticketer.dtos.Requests.ComboSlotRequest sr : slotRequests) {
                    UUID slotId = sr.id() != null ? sr.id() : UUID.randomUUID();
                    List<UUID> options = sr.optionIds() != null ? sr.optionIds() : Collections.emptyList();
                    for (UUID oid : options) requireBaseItem(oid);
                    boolean required = sr.required() != null && sr.required();
                    slots.add(new ComboSlot(slotId, sr.name(), new ArrayList<>(options),
                            new ArrayList<>(options), required, sr.categorySource()));
                }
            }
            UUID id = UUID.randomUUID();
            ComboItem combo = new ComboItem(id, name.trim(), category,
                    componentIds != null ? componentIds : Collections.emptyList(),
                    slots, price, true, kitchen);
            currentMenu.addCombo(combo);
            menuRepository.saveMenu(currentMenu);
            return combo;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateCombo(UUID id, String name, Long price, Boolean available, Boolean kitchen) {
        lock.writeLock().lock();
        try {
            ComboItem combo = currentMenu.getCombo(id);
            if (combo == null) throw new EntityNotFoundException("Combo not found: " + id);
            if (name != null && !name.trim().isEmpty()) combo.setName(name.trim());
            if (price != null) combo.setPrice(price);
            if (available != null) combo.setAvailable(available);
            if (kitchen != null) combo.setKitchen(kitchen);
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteCombo(UUID id) {
        lock.writeLock().lock();
        try {
            if (!currentMenu.removeCombo(id))
                throw new EntityNotFoundException("Combo not found: " + id);
            menuRepository.saveMenu(currentMenu);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public OrderItem createItemOrderItem(UUID menuItemId, UUID selectedSideId) {
        lock.readLock().lock();
        try {
            BaseItem baseItem = requireBaseItem(menuItemId);
            if (!baseItem.isAvailable())
                throw new InvalidInputException("Item is not available: " + baseItem.getName());

            UUID sideId = null;
            String sideName = null;
            long sidePrice = 0;
            if (selectedSideId != null) {
                BaseItem sideItem = requireBaseItem(selectedSideId);
                sideId = sideItem.getId();
                sideName = sideItem.getName();
                sidePrice = sideItem.getPrice();
            }
            return OrderItem.forItem(menuItemId, baseItem.getName(), sideId, sideName,
                    baseItem.getPrice(), sidePrice);
        } finally {
            lock.readLock().unlock();
        }
    }

    public OrderItem createComboOrderItem(UUID comboId, List<SlotSelectionRequest> slotSelections) {
        lock.readLock().lock();
        try {
            ComboItem combo = currentMenu.getCombo(comboId);
            if (combo == null) throw new EntityNotFoundException("Combo not found: " + comboId);

            List<ComboComponentSnapshot> components = combo.getComponents().stream()
                    .map(cid -> {
                        BaseItem item = currentMenu.getBaseItem(cid);
                        return item != null
                                ? new ComboComponentSnapshot(item.getId(), item.getName(), item.getPrice())
                                : null;
                    })
                    .filter(s -> s != null)
                    .collect(Collectors.toList());

            List<ComboSlotSelection> selections = new ArrayList<>();
            Map<UUID, UUID> slotMap = new LinkedHashMap<>();
            if (slotSelections != null) {
                for (SlotSelectionRequest sel : slotSelections) {
                    BaseItem selected = requireBaseItem(sel.selectedBaseItemId());
                    selections.add(new ComboSlotSelection(sel.slotId(), selected.getId(),
                            selected.getName(), selected.getPrice()));
                    slotMap.put(sel.slotId(), sel.selectedBaseItemId());
                }
            }

            long price = combo.computeBasePrice(currentMenu.getBaseItems(), slotMap);
            return OrderItem.forCombo(comboId, combo.getName(), components, selections, price);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isKitchenRelevant(OrderItem item) {
        lock.readLock().lock();
        try {
            return kitchenRelevantUnlocked(item);
        } finally {
            lock.readLock().unlock();
        }
    }

    private boolean kitchenRelevantUnlocked(OrderItem item) {
        if (item.isCombo()) {
            ComboItem combo = currentMenu.getCombo(item.getComboId());
            if (combo != null && combo.isKitchen()) return true;
            if (item.getComponents() != null) {
                for (ComboComponentSnapshot comp : item.getComponents()) {
                    BaseItem bi = currentMenu.getBaseItem(comp.getBaseItemId());
                    if (bi != null && bi.isKitchen()) return true;
                }
            }
            if (item.getSlotSelections() != null) {
                for (ComboSlotSelection sel : item.getSlotSelections()) {
                    BaseItem bi = currentMenu.getBaseItem(sel.getSelectedBaseItemId());
                    if (bi != null && bi.isKitchen()) return true;
                }
            }
            return false;
        } else {
            BaseItem bi = currentMenu.getBaseItem(item.getMenuItemId());
            if (bi != null && bi.isKitchen()) return true;
            if (item.getSelectedSideId() != null) {
                BaseItem side = currentMenu.getBaseItem(item.getSelectedSideId());
                if (side != null && side.isKitchen()) return true;
            }
            return false;
        }
    }

    public KitchenTicketDto getKitchenDetails(Ticket ticket) {
        lock.readLock().lock();
        try {
            Map<String, Integer> tally = new LinkedHashMap<>();
            List<KitchenOrderGroupDto> kitchenOrders = new ArrayList<>();

            for (Order order : ticket.getOrders()) {
                for (OrderItem item : order.getItems()) {
                    addToTally(tally, item);
                }
            }

            for (Order order : ticket.getOrders()) {
                List<KitchenItemDto> groupItems = buildKitchenGroupItems(order);
                if (!groupItems.isEmpty()
                        || (order.getComment() != null && !order.getComment().trim().isEmpty())) {
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

    private void addToTally(Map<String, Integer> tally, OrderItem item) {
        if (item.isCombo()) {
            ComboItem combo = currentMenu.getCombo(item.getComboId());
            if (combo != null && combo.isKitchen()) {
                tally.merge(item.getName(), 1, Integer::sum);
            }
            if (item.getComponents() != null) {
                for (ComboComponentSnapshot comp : item.getComponents()) {
                    BaseItem bi = currentMenu.getBaseItem(comp.getBaseItemId());
                    if (bi != null && bi.isKitchen()) {
                        tally.merge(comp.getName(), 1, Integer::sum);
                    }
                }
            }
            if (item.getSlotSelections() != null) {
                for (ComboSlotSelection sel : item.getSlotSelections()) {
                    BaseItem bi = currentMenu.getBaseItem(sel.getSelectedBaseItemId());
                    if (bi != null && bi.isKitchen()) {
                        tally.merge(sel.getSelectedName(), 1, Integer::sum);
                    }
                }
            }
        } else {
            BaseItem bi = currentMenu.getBaseItem(item.getMenuItemId());
            if (bi != null && bi.isKitchen()) {
                tally.merge(item.getName(), 1, Integer::sum);
            }
            if (item.getSelectedSideId() != null) {
                BaseItem side = currentMenu.getBaseItem(item.getSelectedSideId());
                if (side != null && side.isKitchen() && item.getSelectedSide() != null) {
                    tally.merge(item.getSelectedSide(), 1, Integer::sum);
                }
            }
        }
    }

    private List<KitchenItemDto> buildKitchenGroupItems(Order order) {
        List<KitchenItemDto> groupItems = new ArrayList<>();
        Map<String, Integer> groupCounts = new LinkedHashMap<>();
        Map<String, String> groupSide = new LinkedHashMap<>();
        Map<String, List<String>> groupSlotNames = new LinkedHashMap<>();
        Map<String, Boolean> groupIsCombo = new LinkedHashMap<>();

        for (OrderItem item : order.getItems()) {
            if (!kitchenRelevantUnlocked(item)) continue;

            if (item.getComment() != null && !item.getComment().trim().isEmpty()) {
                if (item.isCombo()) {
                    List<String> slotNames = slotSelectionNames(item);
                    groupItems.add(new KitchenItemDto(item.getName(), null, 1, item.getComment(), slotNames));
                } else {
                    groupItems.add(new KitchenItemDto(item.getName(), item.getSelectedSide(), 1, item.getComment(), null));
                }
                continue;
            }

            if (item.isCombo()) {
                String slotKey = buildSlotKey(item);
                String groupKey = "combo|" + item.getName() + "|" + slotKey;
                groupCounts.merge(groupKey, 1, Integer::sum);
                groupSlotNames.putIfAbsent(groupKey, slotSelectionNames(item));
                groupIsCombo.put(groupKey, true);
            } else {
                String side = item.getSelectedSide() != null ? item.getSelectedSide() : "";
                String groupKey = "item|" + item.getName() + "|" + side;
                groupCounts.merge(groupKey, 1, Integer::sum);
                groupSide.put(groupKey, side.isEmpty() ? null : side);
                groupIsCombo.put(groupKey, false);
            }
        }

        for (Map.Entry<String, Integer> entry : groupCounts.entrySet()) {
            String key = entry.getKey();
            int count = entry.getValue();
            String[] parts = key.split("\\|", 3);
            String name = parts.length > 1 ? parts[1] : key;
            if (Boolean.TRUE.equals(groupIsCombo.get(key))) {
                groupItems.add(new KitchenItemDto(name, null, count, null, groupSlotNames.get(key)));
            } else {
                groupItems.add(new KitchenItemDto(name, groupSide.get(key), count, null, null));
            }
        }

        return groupItems;
    }

    private String buildSlotKey(OrderItem item) {
        if (item.getSlotSelections() == null) return "";
        return item.getSlotSelections().stream()
                .map(s -> s.getSlotId() + ":" + s.getSelectedBaseItemId())
                .collect(Collectors.joining(","));
    }

    private List<String> slotSelectionNames(OrderItem item) {
        if (item.getSlotSelections() == null) return null;
        return item.getSlotSelections().stream()
                .map(ComboSlotSelection::getSelectedName)
                .collect(Collectors.toList());
    }

    private BaseItem requireBaseItem(UUID id) {
        BaseItem item = currentMenu.getBaseItem(id);
        if (item == null) throw new EntityNotFoundException("Base item not found: " + id);
        return item;
    }
}
