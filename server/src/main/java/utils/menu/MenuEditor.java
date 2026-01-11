package utils.menu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class MenuEditor {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void addItem(String category, String name, double price, Map<String, Double> sides)
            throws IOException {
        category = category.toLowerCase();
        JsonObject menu = MenuReader.loadMenu();

        // Check category
        JsonObject categoryObj;
        if (menu.has(category)) {
            categoryObj = menu.getAsJsonObject(category);
        } else {
            categoryObj = new JsonObject();
            menu.add(category, categoryObj);
        }

        // Create item
        JsonObject itemObj = new JsonObject();
        itemObj.addProperty("price", price);
        itemObj.addProperty("available", true);

        if (sides != null && !sides.isEmpty()) {
            JsonObject sidesObj = new JsonObject();
            for (Map.Entry<String, Double> entry : sides.entrySet()) {
                JsonObject sideDetails = new JsonObject();
                sideDetails.addProperty("price", entry.getValue());
                sideDetails.addProperty("available", true);
                sidesObj.add(entry.getKey(), sideDetails);
            }
            itemObj.add("sides", sidesObj);
        }

        categoryObj.add(name, itemObj);
        saveMenu(menu);
    }

    // Edit Methods

    private static void saveMenu(JsonObject menu) throws IOException {
        try (FileWriter writer = new FileWriter(MenuReader.getMenuFilePath())) {
            gson.toJson(menu, writer);
        }
    }

    public static void editItemPrice(String itemName, double newPrice) throws IOException {
        JsonObject menu = MenuReader.loadMenu();
        String category = MenuReader.findCategoryOfItem(menu, itemName);
        if (category == null)
            throw new IllegalArgumentException("Item not found: " + itemName);

        JsonObject item = menu.getAsJsonObject(category).getAsJsonObject(itemName);
        item.addProperty("price", newPrice);
        saveMenu(menu);
    }

    public static void editItemAvailability(String itemName, boolean available) throws IOException {
        JsonObject menu = MenuReader.loadMenu();
        String category = MenuReader.findCategoryOfItem(menu, itemName);
        if (category == null)
            throw new IllegalArgumentException("Item not found: " + itemName);

        JsonObject item = menu.getAsJsonObject(category).getAsJsonObject(itemName);
        item.addProperty("available", available);
        saveMenu(menu);
    }

    public static void renameItem(String oldName, String newName) throws IOException {
        JsonObject menu = MenuReader.loadMenu();
        String category = MenuReader.findCategoryOfItem(menu, oldName);
        if (category == null)
            throw new IllegalArgumentException("Item not found: " + oldName);

        JsonObject catObj = menu.getAsJsonObject(category);
        JsonObject item = catObj.getAsJsonObject(oldName);
        catObj.remove(oldName);
        catObj.add(newName, item);
        saveMenu(menu);
    }

    public static void changeCategory(String itemName, String newCategory) throws IOException {
        newCategory = newCategory.toLowerCase();
        JsonObject menu = MenuReader.loadMenu();
        String oldCategory = MenuReader.findCategoryOfItem(menu, itemName);
        if (oldCategory == null)
            throw new IllegalArgumentException("Item not found: " + itemName);

        if (oldCategory.equals(newCategory))
            return;

        JsonObject oldCatObj = menu.getAsJsonObject(oldCategory);
        JsonObject item = oldCatObj.getAsJsonObject(itemName);
        oldCatObj.remove(itemName);

        JsonObject newCatObj;
        if (menu.has(newCategory)) {
            newCatObj = menu.getAsJsonObject(newCategory);
        } else {
            newCatObj = new JsonObject();
            menu.add(newCategory, newCatObj);
        }

        newCatObj.add(itemName, item);
        saveMenu(menu);
    }

    public static void updateSide(String itemName, String sideName, double newPrice) throws IOException {
        JsonObject menu = MenuReader.loadMenu();
        String category = MenuReader.findCategoryOfItem(menu, itemName);
        if (category == null)
            throw new IllegalArgumentException("Item not found: " + itemName);

        JsonObject item = menu.getAsJsonObject(category).getAsJsonObject(itemName);
        JsonObject sidesObj;
        if (item.has("sides")) {
            sidesObj = item.getAsJsonObject("sides");
        } else {
            sidesObj = new JsonObject();
            item.add("sides", sidesObj);
        }

        JsonObject sideDetails = new JsonObject();
        sideDetails.addProperty("price", newPrice);
        sideDetails.addProperty("available", true);
        sidesObj.add(sideName, sideDetails);

        saveMenu(menu);
    }
}
