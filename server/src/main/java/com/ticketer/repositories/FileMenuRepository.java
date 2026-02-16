package com.ticketer.repositories;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ticketer.models.Extra;
import com.ticketer.models.Menu;
import com.ticketer.models.MenuItem;
import com.ticketer.models.Side;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class FileMenuRepository implements MenuRepository {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FileMenuRepository.class);

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
            return new Menu(new HashMap<>(), new ArrayList<>(), null);
        }

        try (FileReader reader = new FileReader(file)) {
            JsonNode root = objectMapper.readTree(reader);
            if (root == null || root.isNull()) {
                return new Menu(new HashMap<>(), new ArrayList<>(), null);
            }

            Map<String, List<MenuItem>> categories = new HashMap<>();
            List<String> kitchenItems = new ArrayList<>();
            List<String> categoryOrder = null;

            JsonNode categoriesNode;
            if (root.has("categories")) {
                categoriesNode = root.get("categories");
                if (root.has("kitchenItems")) {
                    kitchenItems = objectMapper.convertValue(root.get("kitchenItems"),
                            new TypeReference<List<String>>() {
                            });
                }
                if (root.has("categoryOrder")) {
                    categoryOrder = objectMapper.convertValue(root.get("categoryOrder"),
                            new TypeReference<List<String>>() {
                            });
                }
            } else {
                logger.warn("Menu file {} uses invalid format or is missing 'categories' field. Returning empty menu.",
                        filePath);
                return new Menu(new HashMap<>(), new ArrayList<>(), null);
            }

            Iterator<Map.Entry<String, JsonNode>> fields = categoriesNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> categoryEntry = fields.next();
                String categoryName = categoryEntry.getKey();
                JsonNode categoryNode = categoryEntry.getValue();

                List<MenuItem> items = new ArrayList<>();
                Iterator<Map.Entry<String, JsonNode>> itemFields = categoryNode
                        .fields();

                while (itemFields.hasNext()) {
                    Map.Entry<String, JsonNode> itemEntry = itemFields.next();
                    String itemName = itemEntry.getKey();
                    JsonNode itemNode = itemEntry.getValue();

                    int price = itemNode.has("price") ? itemNode.get("price").asInt() : 0;
                    boolean available = itemNode.has("available") && itemNode.get("available").asBoolean();

                    Map<String, Side> sides = null;
                    List<String> sideOrder = null;
                    if (itemNode.has("sides")) {
                        sides = objectMapper.convertValue(itemNode.get("sides"),
                                new TypeReference<Map<String, Side>>() {
                                });
                    }
                    if (itemNode.has("sideOrder")) {
                        sideOrder = objectMapper.convertValue(itemNode.get("sideOrder"),
                                new TypeReference<List<String>>() {
                                });
                    }
                    Map<String, Extra> extras = null;
                    List<String> extraOrder = null;
                    if (itemNode.has("extras")) {
                        extras = objectMapper.convertValue(itemNode.get("extras"),
                                new TypeReference<Map<String, Extra>>() {
                                });
                    }
                    if (itemNode.has("extraOrder")) {
                        extraOrder = objectMapper.convertValue(itemNode.get("extraOrder"),
                                new TypeReference<List<String>>() {
                                });
                    }
                    items.add(new MenuItem(itemName, price, available, sides, sideOrder, extras, extraOrder));
                }
                categories.put(categoryName, items);
            }
            logger.info("Successfully loaded menu from {}", filePath);
            return new Menu(categories, kitchenItems, categoryOrder);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load menu from " + filePath, e);
        }
    }

    @Override
    public void saveMenu(Menu menu) {
        try (FileWriter writer = new FileWriter(filePath)) {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode categoriesNode = objectMapper.createObjectNode();

            List<String> categoryOrder = menu.getCategoryOrder();
            List<String> categoryKeys = categoryOrder != null && !categoryOrder.isEmpty()
                    ? categoryOrder
                    : new ArrayList<>(menu.getCategories().keySet());

            for (String categoryName : categoryKeys) {
                List<MenuItem> items = menu.getCategories().get(categoryName);
                if (items == null)
                    continue;

                ObjectNode categoryNode = objectMapper.createObjectNode();

                for (MenuItem item : items) {
                    ObjectNode itemNode = objectMapper.createObjectNode();
                    itemNode.put("price", item.price);
                    itemNode.put("available", item.available);

                    if (item.sideOptions != null && !item.sideOptions.isEmpty()) {
                        itemNode.set("sides", objectMapper.valueToTree(item.sideOptions));
                    }
                    if (item.sideOrder != null && !item.sideOrder.isEmpty()) {
                        ArrayNode sideOrderNode = objectMapper.createArrayNode();
                        item.sideOrder.forEach(sideOrderNode::add);
                        itemNode.set("sideOrder", sideOrderNode);
                    }
                    if (item.extraOptions != null && !item.extraOptions.isEmpty()) {
                        itemNode.set("extras", objectMapper.valueToTree(item.extraOptions));
                    }
                    if (item.extraOrder != null && !item.extraOrder.isEmpty()) {
                        ArrayNode extraOrderNode = objectMapper.createArrayNode();
                        item.extraOrder.forEach(extraOrderNode::add);
                        itemNode.set("extraOrder", extraOrderNode);
                    }
                    categoryNode.set(item.name, itemNode);
                }
                categoriesNode.set(categoryName, categoryNode);
            }

            root.set("categories", categoriesNode);
            root.set("kitchenItems", objectMapper.valueToTree(menu.getKitchenItems()));

            if (categoryOrder != null && !categoryOrder.isEmpty()) {
                ArrayNode categoryOrderNode = objectMapper.createArrayNode();
                categoryOrder.forEach(categoryOrderNode::add);
                root.set("categoryOrder", categoryOrderNode);
            }

            objectMapper.writeValue(writer, root);
            logger.info("Successfully saved menu to {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save menu to " + filePath, e);
        }
    }
}
