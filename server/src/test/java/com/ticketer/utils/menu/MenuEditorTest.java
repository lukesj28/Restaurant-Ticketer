package com.ticketer.utils.menu;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.ticketer.utils.menu.MenuEditor;
import com.ticketer.utils.menu.MenuReader;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class MenuEditorTest {

    private static final String ORIGINAL_MENU_PATH = "data/menu.json";
    private static final String TEST_MENU_PATH = "target/test-menu.json";

    @Before
    public void setUp() throws IOException {
        File dataDir = new File("target");
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }
        Files.copy(new File(ORIGINAL_MENU_PATH).toPath(), new File(TEST_MENU_PATH).toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        System.setProperty("menu.file", TEST_MENU_PATH);
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(new File(TEST_MENU_PATH).toPath());
        System.clearProperty("menu.file");
    }

    @Test
    public void testAddItem() throws IOException {
        Map<String, Double> sides = new HashMap<>();
        sides.put("test_side", 1.50);

        MenuEditor.addItem("mains", "test_item", 9.99, sides);

        JsonObject menu = MenuReader.loadMenu();
        JsonObject item = menu.getAsJsonObject("mains").getAsJsonObject("test_item");

        assertNotNull(item);
        assertEquals(9.99, item.get("price").getAsDouble(), 0.001);
        assertTrue(item.get("available").getAsBoolean());
        assertEquals(1.50, item.getAsJsonObject("sides").getAsJsonObject("test_side").get("price").getAsDouble(),
                0.001);
    }

    @Test
    public void testAddItemNoSides() throws IOException {
        MenuEditor.addItem("mains", "simple_item", 5.00, null);

        JsonObject menu = MenuReader.loadMenu();
        JsonObject item = menu.getAsJsonObject("mains").getAsJsonObject("simple_item");

        assertNotNull(item);
        assertEquals(5.00, item.get("price").getAsDouble(), 0.001);
        assertFalse(item.has("sides"));
    }

    @Test
    public void testEditItemPrice() throws IOException {
        MenuEditor.editItemPrice("haddock", 20.00);

        JsonObject menu = MenuReader.loadMenu();
        double newPrice = menu.getAsJsonObject("mains").getAsJsonObject("haddock").get("price").getAsDouble();

        assertEquals(20.00, newPrice, 0.001);
    }

    @Test
    public void testEditItemAvailability() throws IOException {
        MenuEditor.editItemAvailability("haddock", false);

        JsonObject menu = MenuReader.loadMenu();
        boolean avail = menu.getAsJsonObject("mains").getAsJsonObject("haddock").get("available").getAsBoolean();

        assertFalse(avail);
    }

    @Test
    public void testConstructor() {
        new MenuEditor();
    }

    @Test
    public void testAddItemNewCategory() throws IOException {
        MenuEditor.addItem("specials", "daily_special", 12.99, null);
        JsonObject menu = MenuReader.loadMenu();
        assertTrue(menu.has("specials"));
        assertNotNull(menu.getAsJsonObject("specials").getAsJsonObject("daily_special"));
    }

    @Test
    public void testChangeCategorySame() throws IOException {
        MenuEditor.changeCategory("haddock", "mains");
        JsonObject menu = MenuReader.loadMenu();
        assertTrue(menu.getAsJsonObject("mains").has("haddock"));
    }

    @Test
    public void testChangeCategoryNew() throws IOException {
        MenuEditor.changeCategory("haddock", "seafood_delight");
        JsonObject menu = MenuReader.loadMenu();
        assertFalse(menu.getAsJsonObject("mains").has("haddock"));
        assertTrue(menu.has("seafood_delight"));
        assertTrue(menu.getAsJsonObject("seafood_delight").has("haddock"));
    }

    @Test
    public void testUpdateSideNoSides() throws IOException {
        MenuEditor.addItem("mains", "plain_fish", 10.00, null);
        MenuEditor.updateSide("plain_fish", "lemon", 0.50);

        JsonObject menu = MenuReader.loadMenu();
        JsonObject item = menu.getAsJsonObject("mains").getAsJsonObject("plain_fish");
        assertTrue(item.has("sides"));
        assertEquals(0.50, item.getAsJsonObject("sides").getAsJsonObject("lemon").get("price").getAsDouble(), 0.001);
    }

    @Test
    public void testRenameItem() throws IOException {
        MenuEditor.renameItem("haddock", "super_haddock");

        JsonObject menu = MenuReader.loadMenu();
        JsonObject mains = menu.getAsJsonObject("mains");

        assertFalse(mains.has("haddock"));
        assertTrue(mains.has("super_haddock"));
        assertEquals(15.99, mains.getAsJsonObject("super_haddock").get("price").getAsDouble(), 0.001);
    }

    @Test
    public void testChangeCategory() throws IOException {
        MenuEditor.changeCategory("haddock", "drinks");

        JsonObject menu = MenuReader.loadMenu();

        assertFalse(menu.getAsJsonObject("mains").has("haddock"));
        assertTrue(menu.getAsJsonObject("drinks").has("haddock"));
    }

    @Test
    public void testUpdateSide() throws IOException {
        MenuEditor.updateSide("haddock", "chips", 5.00);

        JsonObject menu = MenuReader.loadMenu();
        double sidePrice = menu.getAsJsonObject("mains")
                .getAsJsonObject("haddock")
                .getAsJsonObject("sides")
                .getAsJsonObject("chips")
                .get("price").getAsDouble();

        assertEquals(5.00, sidePrice, 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEditItemPriceNotFound() throws IOException {
        MenuEditor.editItemPrice("non_existent_item", 10.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEditItemAvailabilityNotFound() throws IOException {
        MenuEditor.editItemAvailability("non_existent_item", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRenameItemNotFound() throws IOException {
        MenuEditor.renameItem("non_existent_item", "new_name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChangeCategoryNotFound() throws IOException {
        MenuEditor.changeCategory("non_existent_item", "new_cat");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSideNotFound() throws IOException {
        MenuEditor.updateSide("non_existent_item", "side", 1.0);
    }
}
