package utils.menu;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import utils.menu.dto.ComplexItem;
import utils.menu.dto.MenuItemView;
import models.Item;
import utils.menu.dto.Side;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MenuReader {

    public static final String MENU_FILE_PATH = "src/main/java/data/menu.json";
    private static final Gson gson = new Gson();

    public static JsonObject loadMenu() throws IOException {
        return gson.fromJson(new FileReader(MENU_FILE_PATH), JsonObject.class);
    }

    // List all
    public static List<MenuItemView> getAllItems() {
        List<MenuItemView> list = new ArrayList<>();
        try {
            JsonObject menu = loadMenu();
            for (String category : menu.keySet()) {
                JsonObject catObj = menu.getAsJsonObject(category);
                for (String key : catObj.keySet()) {
                    JsonObject item = catObj.getAsJsonObject(key);
                    double price = item.get("price").getAsDouble();
                    boolean avail = item.has("available") && item.get("available").getAsBoolean();
                    list.add(new MenuItemView(key, price, avail, category));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading menu.json: " + e.getMessage());
        }
        return list;
    }

    // Get details
    public static ComplexItem getItemDetails(String itemName) {
        try {
            JsonObject menu = loadMenu();
            for (String category : menu.keySet()) {
                JsonObject catObj = menu.getAsJsonObject(category);
                if (catObj.has(itemName)) {
                    JsonObject item = catObj.getAsJsonObject(itemName);
                    double price = item.get("price").getAsDouble();
                    boolean avail = item.has("available") && item.get("available").getAsBoolean();

                    Map<String, Side> sides = null;
                    if (item.has("sides")) {
                        Type sideType = new TypeToken<Map<String, Side>>() {
                        }.getType();
                        sides = new Gson().fromJson(item.get("sides"), sideType);
                    }
                    return new ComplexItem(itemName, price, avail, sides);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading menu.json: " + e.getMessage());
        }
        return null;
    }

    // Get item
    public static Item getItem(ComplexItem item, String sideName) {
        if (!item.hasSides()) {
            return new Item(item.name, null, item.basePrice);
        }

        if (sideName != null && item.sideOptions.containsKey(sideName)) {
            Side side = item.sideOptions.get(sideName);
            return new Item(item.name, sideName, item.basePrice + side.price);
        } else {
            if (sideName != null) {
                throw new IllegalArgumentException("Invalid side selection: " + sideName);
            }
            return new Item(item.name, null, item.basePrice);
        }
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
