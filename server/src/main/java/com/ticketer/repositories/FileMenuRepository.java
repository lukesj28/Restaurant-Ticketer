package com.ticketer.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketer.models.BaseItem;
import com.ticketer.models.CategoryEntry;
import com.ticketer.models.ComboItem;
import com.ticketer.models.ComboSlot;
import com.ticketer.models.CompositeComponent;
import com.ticketer.models.Menu;
import com.ticketer.models.MenuItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class FileMenuRepository implements MenuRepository {

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(FileMenuRepository.class);

    private final String filePath;
    private final ObjectMapper objectMapper;

    @Autowired
    public FileMenuRepository(ObjectMapper objectMapper) {
        this.filePath = System.getProperty("menu.file", "data/menu.json");
        this.objectMapper = objectMapper;
    }

    public FileMenuRepository(String filePath, ObjectMapper objectMapper) {
        this.filePath = filePath;
        this.objectMapper = objectMapper;
    }

    @Override
    public Menu getMenu() {
        File file = new File(filePath);
        if (!file.exists()) {
            logger.warn("Menu file not found at {}, returning empty menu", filePath);
            return new Menu(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), null);
        }

        try {
            MenuJson raw = objectMapper.readValue(file, MenuJson.class);

            Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
            if (raw.baseItems != null) {
                for (Map.Entry<String, BaseItemJson> e : raw.baseItems.entrySet()) {
                    UUID id = UUID.fromString(e.getKey());
                    BaseItemJson b = e.getValue();
                    List<CompositeComponent> components = null;
                    if (b.components != null && !b.components.isEmpty()) {
                        components = new ArrayList<>();
                        for (ComponentJson cj : b.components) {
                            components.add(new CompositeComponent(UUID.fromString(cj.baseItemId), cj.quantity));
                        }
                    }
                    baseItems.put(id, new BaseItem(id, b.name, b.price, b.available, b.kitchen, b.alcohol, components));
                }
            }

            Map<String, CategoryEntry> categories = new LinkedHashMap<>();
            if (raw.categories != null) {
                for (Map.Entry<String, CategoryJson> e : raw.categories.entrySet()) {
                    String catName = e.getKey();
                    CategoryJson catJson = e.getValue();
                    List<MenuItem> items = new ArrayList<>();
                    if (catJson.items != null) {
                        for (MenuItemJson mij : catJson.items) {
                            UUID id = UUID.fromString(mij.baseItemId);
                            List<String> sources = mij.sideSources != null
                                    ? mij.sideSources : new ArrayList<>();
                            items.add(new MenuItem(id, sources));
                        }
                    }
                    categories.put(catName, new CategoryEntry(catJson.visible, items));
                }
            }

            Map<UUID, ComboItem> combos = new LinkedHashMap<>();
            if (raw.combos != null) {
                for (Map.Entry<String, ComboJson> e : raw.combos.entrySet()) {
                    UUID id = UUID.fromString(e.getKey());
                    ComboJson cj = e.getValue();
                    List<UUID> components = new ArrayList<>();
                    if (cj.components != null) {
                        for (String s : cj.components) components.add(UUID.fromString(s));
                    }
                    List<ComboSlot> slots = new ArrayList<>();
                    if (cj.slots != null) {
                        for (ComboSlotJson sj : cj.slots) {
                            UUID slotId = UUID.fromString(sj.id);
                            List<UUID> options = new ArrayList<>();
                            if (sj.options != null) {
                                for (String s : sj.options) options.add(UUID.fromString(s));
                            }
                            List<UUID> optionOrder = new ArrayList<>();
                            if (sj.optionOrder != null) {
                                for (String s : sj.optionOrder) optionOrder.add(UUID.fromString(s));
                            } else {
                                optionOrder = new ArrayList<>(options);
                            }
                            slots.add(new ComboSlot(slotId, sj.name, options, optionOrder, sj.required));
                        }
                    }
                    combos.put(id, new ComboItem(id, cj.name, cj.category, components, slots,
                            cj.price, cj.available, cj.kitchen));
                }
            }

            logger.info("Successfully loaded menu from {}", filePath);
            return new Menu(baseItems, categories, combos, raw.categoryOrder);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load menu from " + filePath, e);
        }
    }

    @Override
    public void saveMenu(Menu menu) {
        MenuJson raw = new MenuJson();

        raw.baseItems = new LinkedHashMap<>();
        for (Map.Entry<UUID, BaseItem> e : menu.getBaseItems().entrySet()) {
            BaseItem b = e.getValue();
            BaseItemJson bj = new BaseItemJson();
            bj.name = b.getName();
            bj.price = b.getPrice();
            bj.available = b.isAvailable();
            bj.kitchen = b.isKitchen();
            bj.alcohol = b.isAlcohol();
            if (b.getComponents() != null && !b.getComponents().isEmpty()) {
                bj.components = new ArrayList<>();
                for (CompositeComponent cc : b.getComponents()) {
                    ComponentJson cj = new ComponentJson();
                    cj.baseItemId = cc.getBaseItemId().toString();
                    cj.quantity = cc.getQuantity();
                    bj.components.add(cj);
                }
            }
            raw.baseItems.put(e.getKey().toString(), bj);
        }

        raw.categories = new LinkedHashMap<>();
        List<String> order = menu.getCategoryOrder();
        List<String> keys = (order != null && !order.isEmpty())
                ? order : new ArrayList<>(menu.getCategories().keySet());
        for (String catName : keys) {
            CategoryEntry entry = menu.getCategories().get(catName);
            if (entry == null) continue;
            CategoryJson cj = new CategoryJson();
            cj.visible = entry.isVisible();
            cj.items = new ArrayList<>();
            for (MenuItem mi : entry.getItems()) {
                MenuItemJson mij = new MenuItemJson();
                mij.baseItemId = mi.getBaseItemId().toString();
                mij.sideSources = mi.getSideSources();
                cj.items.add(mij);
            }
            raw.categories.put(catName, cj);
        }

        raw.combos = new LinkedHashMap<>();
        for (Map.Entry<UUID, ComboItem> e : menu.getCombos().entrySet()) {
            ComboItem c = e.getValue();
            ComboJson cj = new ComboJson();
            cj.name = c.getName();
            cj.category = c.getCategory();
            cj.components = new ArrayList<>();
            for (UUID id : c.getComponents()) cj.components.add(id.toString());
            cj.slots = new ArrayList<>();
            for (ComboSlot slot : c.getSlots()) {
                ComboSlotJson sj = new ComboSlotJson();
                sj.id = slot.getId().toString();
                sj.name = slot.getName();
                sj.options = new ArrayList<>();
                for (UUID id : slot.getOptions()) sj.options.add(id.toString());
                sj.optionOrder = new ArrayList<>();
                for (UUID id : slot.getOptionOrder()) sj.optionOrder.add(id.toString());
                sj.required = slot.isRequired();
                cj.slots.add(sj);
            }
            cj.price = c.getPrice();
            cj.available = c.isAvailable();
            cj.kitchen = c.isKitchen();
            raw.combos.put(e.getKey().toString(), cj);
        }

        raw.categoryOrder = menu.getCategoryOrder();

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), raw);
            logger.info("Successfully saved menu to {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save menu to " + filePath, e);
        }
    }

    static class MenuJson {
        public Map<String, BaseItemJson> baseItems;
        public Map<String, CategoryJson> categories;
        public Map<String, ComboJson> combos;
        public List<String> categoryOrder;
    }

    static class BaseItemJson {
        public String name;
        public long price;
        public boolean available;
        public boolean kitchen;
        public boolean alcohol;
        public List<ComponentJson> components;
    }

    static class ComponentJson {
        public String baseItemId;
        public double quantity;
    }

    static class CategoryJson {
        public boolean visible = true;
        public List<MenuItemJson> items;
    }

    static class MenuItemJson {
        public String baseItemId;
        public List<String> sideSources;
    }

    static class ComboJson {
        public String name;
        public String category;
        public List<String> components;
        public List<ComboSlotJson> slots;
        public Long price;
        public boolean available;
        public boolean kitchen;
    }

    static class ComboSlotJson {
        public String id;
        public String name;
        public List<String> options;
        public List<String> optionOrder;
        public boolean required;
    }
}
