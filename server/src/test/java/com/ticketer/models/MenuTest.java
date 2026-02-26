package com.ticketer.models;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MenuTest {

    private Menu buildSimpleMenu() {
        UUID itemId = UUID.randomUUID();
        BaseItem baseItem = new BaseItem(itemId, "Burger", 1000, true, true);
        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(itemId, baseItem);

        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem(itemId, Collections.emptyList()));
        CategoryEntry entry = new CategoryEntry(true, items);
        Map<String, CategoryEntry> categories = new LinkedHashMap<>();
        categories.put("mains", entry);

        return new Menu(baseItems, categories, new LinkedHashMap<>(), new ArrayList<>(categories.keySet()));
    }

    @Test
    public void testGetBaseItem() {
        Menu menu = buildSimpleMenu();
        UUID id = menu.getBaseItems().keySet().iterator().next();
        BaseItem item = menu.getBaseItem(id);
        assertNotNull(item);
        assertEquals("Burger", item.getName());
    }

    @Test
    public void testGetCategory() {
        Menu menu = buildSimpleMenu();
        assertNotNull(menu.getCategory("mains"));
        assertNull(menu.getCategory("desserts"));
    }

    @Test
    public void testGetCategories() {
        Menu menu = buildSimpleMenu();
        assertEquals(1, menu.getCategories().size());
        assertTrue(menu.getCategories().containsKey("mains"));
    }

    @Test
    public void testAddAndRemoveBaseItem() {
        Menu menu = buildSimpleMenu();
        UUID newId = UUID.randomUUID();
        BaseItem newItem = new BaseItem(newId, "Pizza", 1200, true, false);
        menu.addBaseItem(newItem);
        assertNotNull(menu.getBaseItem(newId));
        assertTrue(menu.removeBaseItem(newId));
        assertNull(menu.getBaseItem(newId));
    }

    @Test
    public void testAddMenuItem() {
        Menu menu = buildSimpleMenu();
        UUID newId = UUID.randomUUID();
        BaseItem newItem = new BaseItem(newId, "Fries", 500, true, true);
        menu.addBaseItem(newItem);
        menu.addMenuItem("mains", new MenuItem(newId, Collections.emptyList()));
        assertEquals(2, menu.getCategory("mains").getItems().size());
    }

    @Test
    public void testRemoveMenuItem() {
        Menu menu = buildSimpleMenu();
        UUID id = menu.getCategory("mains").getItems().get(0).getBaseItemId();
        assertTrue(menu.removeMenuItem("mains", id));
        assertNull(menu.getCategory("mains"));
    }

    @Test
    public void testRenameCategory() {
        Menu menu = buildSimpleMenu();
        menu.renameCategory("mains", "entrees");
        assertNull(menu.getCategory("mains"));
        assertNotNull(menu.getCategory("entrees"));
        assertTrue(menu.getCategoryOrder().contains("entrees"));
        assertFalse(menu.getCategoryOrder().contains("mains"));
    }

    @Test
    public void testCategoryOrder() {
        Menu menu = buildSimpleMenu();
        assertTrue(menu.getCategoryOrder().contains("mains"));
    }

    @Test
    public void testAddComboAndRetrieve() {
        Menu menu = buildSimpleMenu();
        UUID comboId = UUID.randomUUID();
        ComboItem combo = new ComboItem(comboId, "Meal Deal", "mains",
                Collections.emptyList(), Collections.emptyList(), 2000L, true, true);
        menu.addCombo(combo);
        assertNotNull(menu.getCombo(comboId));
        assertEquals("Meal Deal", menu.getCombo(comboId).getName());
        assertTrue(menu.removeCombo(comboId));
        assertNull(menu.getCombo(comboId));
    }

    @Test
    public void testGetSideOptions() {
        UUID sideId = UUID.randomUUID();
        BaseItem sideItem = new BaseItem(sideId, "Chips", 200, true, true);
        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(sideId, sideItem);

        List<MenuItem> sideItems = new ArrayList<>();
        sideItems.add(new MenuItem(sideId, Collections.emptyList()));
        CategoryEntry sideEntry = new CategoryEntry(false, sideItems);

        Map<String, CategoryEntry> categories = new LinkedHashMap<>();
        categories.put("side options", sideEntry);

        Menu menu = new Menu(baseItems, categories, new LinkedHashMap<>(), new ArrayList<>());
        List<BaseItem> options = menu.getSideOptions(Collections.singletonList("side options"));
        assertEquals(1, options.size());
        assertEquals("Chips", options.get(0).getName());
    }
}
