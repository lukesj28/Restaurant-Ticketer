package com.ticketer.repositories;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.ticketer.models.Menu;
import com.ticketer.utils.menu.dto.MenuItem;
import com.ticketer.utils.menu.dto.Side;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileMenuRepository implements MenuRepository {

    private final String filePath;
    private final Gson gson;

    public FileMenuRepository() {
        this.filePath = System.getProperty("menu.file", "data/menu.json");
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Side.class, new SideDeserializer())
                .setPrettyPrinting()
                .create();
    }

    public FileMenuRepository(String filePath) {
        this.filePath = filePath;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Side.class, new SideDeserializer())
                .setPrettyPrinting()
                .create();
    }

    @Override
    public Menu getMenu() {
        try (FileReader reader = new FileReader(filePath)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json == null) {
                return new Menu(new HashMap<>());
            }
            return parseMenu(json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load menu from " + filePath, e);
        }
    }

    @Override
    public void saveMenu(Menu menu) {
        JsonObject json = new JsonObject();

        for (Map.Entry<String, List<MenuItem>> catEntry : menu.getCategories().entrySet()) {
            JsonObject catJson = new JsonObject();
            for (MenuItem item : catEntry.getValue()) {
                JsonObject itemJson = new JsonObject();
                itemJson.addProperty("price", item.basePrice / 100.0);
                itemJson.addProperty("available", item.available);

                if (item.sideOptions != null && !item.sideOptions.isEmpty()) {
                    JsonObject sidesJson = new JsonObject();
                    for (Map.Entry<String, Side> sideEntry : item.sideOptions.entrySet()) {
                        JsonObject sideJson = new JsonObject();
                        sideJson.addProperty("price", sideEntry.getValue().price / 100.0);
                        sideJson.addProperty("available", sideEntry.getValue().available);
                        sidesJson.add(sideEntry.getKey(), sideJson);
                    }
                    itemJson.add("sides", sidesJson);
                }
                catJson.add(item.name, itemJson);
            }
            json.add(catEntry.getKey(), catJson);
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(json, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save menu to " + filePath, e);
        }
    }

    private Menu parseMenu(JsonObject json) {
        Map<String, List<MenuItem>> categories = new HashMap<>();

        for (String categoryKey : json.keySet()) {
            JsonObject categoryJson = json.getAsJsonObject(categoryKey);
            List<MenuItem> items = new ArrayList<>();

            for (String itemKey : categoryJson.keySet()) {
                JsonObject itemJson = categoryJson.getAsJsonObject(itemKey);
                int price = (int) Math.round(itemJson.get("price").getAsDouble() * 100);
                boolean avail = itemJson.has("available") && itemJson.get("available").getAsBoolean();

                Map<String, Side> sides = null;
                if (itemJson.has("sides")) {
                    Type sideType = new TypeToken<Map<String, Side>>() {
                    }.getType();
                    sides = gson.fromJson(itemJson.get("sides"), sideType);
                }

                items.add(new MenuItem(itemKey, price, avail, sides));
            }
            categories.put(categoryKey, items);
        }
        return new Menu(categories);
    }

    private static class SideDeserializer implements JsonDeserializer<Side> {
        @Override
        public Side deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObj = json.getAsJsonObject();
            Side side = new Side();
            side.price = (int) Math.round(jsonObj.get("price").getAsDouble() * 100);
            side.available = jsonObj.get("available").getAsBoolean();
            return side;
        }
    }
}
