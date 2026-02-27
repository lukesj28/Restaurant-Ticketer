package com.ticketer.repositories;

import com.ticketer.models.BaseItem;
import com.ticketer.models.CategoryEntry;
import com.ticketer.models.CompositeComponent;
import com.ticketer.models.Menu;
import com.ticketer.models.MenuItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FileMenuRepositoryTest {

    private static final String TEST_FILE = "target/repo-test-menu.json";
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        new File(TEST_FILE).delete();
        mapper = new com.ticketer.config.JacksonConfig().objectMapper();
    }

    @AfterEach
    public void tearDown() {
        new File(TEST_FILE).delete();
    }

    @Test
    public void testGetMenuFileNotFound() {
        FileMenuRepository repo = new FileMenuRepository(TEST_FILE, mapper);
        Menu menu = repo.getMenu();
        assertNotNull(menu);
        assertTrue(menu.getCategories().isEmpty());
        assertTrue(menu.getBaseItems().isEmpty());
    }

    @Test
    public void testGetMenuCorruptedJson() throws IOException {
        Files.write(new File(TEST_FILE).toPath(), "{ invalid json ".getBytes());
        FileMenuRepository repo = new FileMenuRepository(TEST_FILE, mapper);
        assertThrows(RuntimeException.class, () -> repo.getMenu());
    }

    @Test
    public void testSaveAndLoad() {
        FileMenuRepository repo = new FileMenuRepository(TEST_FILE, mapper);
        Menu menu = new Menu(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>());
        repo.saveMenu(menu);

        File f = new File(TEST_FILE);
        assertTrue(f.exists());

        Menu loaded = repo.getMenu();
        assertNotNull(loaded);
        assertTrue(loaded.getCategories().isEmpty());
        assertTrue(loaded.getBaseItems().isEmpty());
    }

    @Test
    public void testLoadMenuFromJson() throws IOException {
        UUID id = UUID.randomUUID();
        String json = "{"
                + "\"baseItems\": {"
                + "  \"" + id + "\": {\"name\": \"Burger\", \"price\": 1000, \"available\": true, \"kitchen\": true}"
                + "},"
                + "\"categories\": {"
                + "  \"mains\": {\"visible\": true, \"items\": [{\"baseItemId\": \"" + id + "\", \"sideSources\": []}]}"
                + "},"
                + "\"combos\": {},"
                + "\"categoryOrder\": [\"mains\"]"
                + "}";
        Files.write(new File(TEST_FILE).toPath(), json.getBytes());

        FileMenuRepository repo = new FileMenuRepository(TEST_FILE, mapper);
        Menu loaded = repo.getMenu();

        assertNotNull(loaded);
        assertTrue(loaded.getCategories().containsKey("mains"));
        assertEquals(1, loaded.getCategories().get("mains").getItems().size());
        assertEquals(id, loaded.getCategories().get("mains").getItems().get(0).getBaseItemId());
        assertNotNull(loaded.getBaseItem(id));
        assertEquals("Burger", loaded.getBaseItem(id).getName());
        assertTrue(loaded.getBaseItem(id).isKitchen());
    }

    @Test
    public void testKitchenFieldSaveAndLoad() {
        FileMenuRepository repo = new FileMenuRepository(TEST_FILE, mapper);

        UUID id = UUID.randomUUID();
        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(id, new BaseItem(id, "Fish", 1000, true, true));

        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem(id, Collections.emptyList()));

        Map<String, CategoryEntry> categories = new LinkedHashMap<>();
        categories.put("mains", new CategoryEntry(true, items));

        Menu menu = new Menu(baseItems, categories, new LinkedHashMap<>(), new ArrayList<>(categories.keySet()));
        repo.saveMenu(menu);

        Menu loaded = repo.getMenu();
        assertNotNull(loaded);
        assertTrue(loaded.getBaseItem(id).isKitchen());
    }

    @Test
    public void testCompositeComponentsSaveAndLoad() {
        FileMenuRepository repo = new FileMenuRepository(TEST_FILE, mapper);

        UUID subId = UUID.randomUUID();
        UUID compositeId = UUID.randomUUID();
        List<CompositeComponent> components = List.of(new CompositeComponent(subId, 2.5));

        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(subId, new BaseItem(subId, "Sub", 500, true, true));
        baseItems.put(compositeId, new BaseItem(compositeId, "Composite", 0, true, true, components));

        Menu menu = new Menu(baseItems, new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>());
        repo.saveMenu(menu);

        Menu loaded = repo.getMenu();
        BaseItem loadedComposite = loaded.getBaseItem(compositeId);
        assertNotNull(loadedComposite);
        assertNotNull(loadedComposite.getComponents());
        assertEquals(1, loadedComposite.getComponents().size());
        assertEquals(subId, loadedComposite.getComponents().get(0).getBaseItemId());
        assertEquals(2.5, loadedComposite.getComponents().get(0).getQuantity(), 0.001);

        assertNull(loaded.getBaseItem(subId).getComponents());
    }

    @Test
    public void testCategoryOrderPreserved() {
        FileMenuRepository repo = new FileMenuRepository(TEST_FILE, mapper);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(id1, new BaseItem(id1, "Starter", 500, true, false));
        baseItems.put(id2, new BaseItem(id2, "Main", 1500, true, false));

        Map<String, CategoryEntry> categories = new LinkedHashMap<>();
        categories.put("starters", new CategoryEntry(true,
                Collections.singletonList(new MenuItem(id1, Collections.emptyList()))));
        categories.put("mains", new CategoryEntry(true,
                Collections.singletonList(new MenuItem(id2, Collections.emptyList()))));

        List<String> order = List.of("mains", "starters");
        Menu menu = new Menu(baseItems, categories, new LinkedHashMap<>(), order);
        repo.saveMenu(menu);

        Menu loaded = repo.getMenu();
        assertEquals(List.of("mains", "starters"), loaded.getCategoryOrder());
    }
}
