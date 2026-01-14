package com.ticketer.utils.menu;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.ticketer.utils.menu.dto.ComplexItem;
import com.ticketer.utils.menu.dto.Side;
import com.ticketer.models.Menu;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuReader {

    public static String getMenuFilePath() {
        return System.getProperty("menu.file", "data/menu.json");
    }

    private static final Gson gson = new Gson();

    public static JsonObject loadMenu() {
        try (FileReader reader = new FileReader(getMenuFilePath())) {
            return gson.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            throw new com.ticketer.exceptions.StorageException("Failed to load menu", e);
        }
    }

    public static Menu readMenu() {
        JsonObject json = loadMenu();
        Map<String, List<ComplexItem>> categories = new HashMap<>();

        for (String categoryKey : json.keySet()) {
            JsonObject categoryJson = json.getAsJsonObject(categoryKey);
            List<ComplexItem> items = new ArrayList<>();

            for (String itemKey : categoryJson.keySet()) {
                JsonObject itemJson = categoryJson.getAsJsonObject(itemKey);
                double price = itemJson.get("price").getAsDouble();
                boolean avail = itemJson.has("available") && itemJson.get("available").getAsBoolean();

                Map<String, Side> sides = null;
                if (itemJson.has("sides")) {
                    Type sideType = new TypeToken<Map<String, Side>>() {
                    }.getType();
                    sides = gson.fromJson(itemJson.get("sides"), sideType);
                }

                items.add(new ComplexItem(itemKey, price, avail, sides));
            }
            categories.put(categoryKey, items);
        }
        return new Menu(categories);
    }

    public static String findCategoryOfItem(JsonObject menu, String itemName) {
        for (String cat : menu.keySet()) {
            JsonObject catObj = menu.getAsJsonObject(cat);
            if (catObj.has(itemName)) {
                return cat;
            }
        }
        return null;
    }
}
