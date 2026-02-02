package com.ticketer.repositories;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
            return new Menu(new HashMap<>());
        }

        try (FileReader reader = new FileReader(file)) {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(reader);
            if (root == null || root.isNull()) {
                return new Menu(new HashMap<>());
            }

            Map<String, List<MenuItem>> categories = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();

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
                    if (itemNode.has("sides")) {
                        sides = objectMapper.convertValue(itemNode.get("sides"),
                                new TypeReference<Map<String, Side>>() {
                                });
                    }
                    items.add(new MenuItem(itemName, price, available,
                            sides));
                }
                categories.put(categoryName, items);
            }
            logger.info("Successfully loaded menu from {}", filePath);
            return new Menu(categories);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load menu from " + filePath, e);
        }
    }

    @Override
    public void saveMenu(Menu menu) {
        try (FileWriter writer = new FileWriter(filePath)) {
            ObjectNode root = objectMapper.createObjectNode();

            for (Map.Entry<String, List<MenuItem>> categoryEntry : menu.getCategories()
                    .entrySet()) {
                ObjectNode categoryNode = objectMapper.createObjectNode();

                for (MenuItem item : categoryEntry.getValue()) {
                    ObjectNode itemNode = objectMapper.createObjectNode();
                    itemNode.put("price", item.price);
                    itemNode.put("available", item.available);

                    if (item.sideOptions != null && !item.sideOptions.isEmpty()) {
                        itemNode.set("sides", objectMapper.valueToTree(item.sideOptions));
                    }
                    categoryNode.set(item.name, itemNode);
                }
                root.set(categoryEntry.getKey(), categoryNode);
            }
            objectMapper.writeValue(writer, root);
            logger.info("Successfully saved menu to {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save menu to " + filePath, e);
        }
    }
}
