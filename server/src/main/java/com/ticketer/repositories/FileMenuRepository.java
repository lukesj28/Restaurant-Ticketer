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
            return new Menu(new HashMap<>());
        }

        try (FileReader reader = new FileReader(file)) {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(reader);
            if (root == null || root.isNull()) {
                return new Menu(new HashMap<>());
            }

            Map<String, java.util.List<com.ticketer.models.MenuItem>> categories = new HashMap<>();
            java.util.Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = root.fields();

            while (fields.hasNext()) {
                Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> categoryEntry = fields.next();
                String categoryName = categoryEntry.getKey();
                com.fasterxml.jackson.databind.JsonNode categoryNode = categoryEntry.getValue();

                java.util.List<com.ticketer.models.MenuItem> items = new java.util.ArrayList<>();
                java.util.Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> itemFields = categoryNode
                        .fields();

                while (itemFields.hasNext()) {
                    Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> itemEntry = itemFields.next();
                    String itemName = itemEntry.getKey();
                    com.fasterxml.jackson.databind.JsonNode itemNode = itemEntry.getValue();

                    double priceDouble = itemNode.has("price") ? itemNode.get("price").asDouble() : 0.0;
                    boolean available = itemNode.has("available") && itemNode.get("available").asBoolean();

                    Map<String, com.ticketer.models.Side> sides = null;
                    if (itemNode.has("sides")) {
                        sides = objectMapper.convertValue(itemNode.get("sides"),
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, com.ticketer.models.Side>>() {
                                });
                    }
                    items.add(new com.ticketer.models.MenuItem(itemName, (int) Math.round(priceDouble * 100), available,
                            sides));
                }
                categories.put(categoryName, items);
            }
            return new Menu(categories);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load menu from " + filePath, e);
        }
    }

    @Override
    public void saveMenu(Menu menu) {
        try (FileWriter writer = new FileWriter(filePath)) {
            com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();

            for (Map.Entry<String, java.util.List<com.ticketer.models.MenuItem>> categoryEntry : menu.getCategories()
                    .entrySet()) {
                com.fasterxml.jackson.databind.node.ObjectNode categoryNode = objectMapper.createObjectNode();

                for (com.ticketer.models.MenuItem item : categoryEntry.getValue()) {
                    com.fasterxml.jackson.databind.node.ObjectNode itemNode = objectMapper.createObjectNode();
                    itemNode.put("price", item.basePrice / 100.0);
                    itemNode.put("available", item.available);

                    if (item.sideOptions != null && !item.sideOptions.isEmpty()) {
                        itemNode.set("sides", objectMapper.valueToTree(item.sideOptions));
                    }
                    categoryNode.set(item.name, itemNode);
                }
                root.set(categoryEntry.getKey(), categoryNode);
            }
            objectMapper.writeValue(writer, root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save menu to " + filePath, e);
        }
    }
}
