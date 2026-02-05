package com.ticketer.repositories;

import com.ticketer.models.Menu;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class FileMenuRepositoryTest {

    private static final String TEST_FILE = "target/repo-test-menu.json";
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        new File(TEST_FILE).delete();
        mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
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
        assertTrue(menu.getKitchenItems().isEmpty());
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
        Menu menu = new Menu(new java.util.HashMap<>(), new java.util.ArrayList<>());
        repo.saveMenu(menu);

        File f = new File(TEST_FILE);
        assertTrue(f.exists());

        Menu loaded = repo.getMenu();
        assertNotNull(loaded);
        assertTrue(loaded.getCategories().isEmpty());
    }

    @Test
    public void testLoadMenu() throws IOException {
        String json = "{" +
                "\"categories\": {" +
                "\"mains\": {" +
                "\"Burger\": { \"price\": 1000, \"available\": true }" +
                "}" +
                "}," +
                "\"kitchenItems\": [\"Burger\"]" +
                "}";
        Files.write(new File(TEST_FILE).toPath(), json.getBytes());

        FileMenuRepository repo = new FileMenuRepository(TEST_FILE, mapper);
        Menu loaded = repo.getMenu();

        assertNotNull(loaded);
        assertTrue(loaded.getCategories().containsKey("mains"));
        assertEquals(1, loaded.getKitchenItems().size());
        assertEquals("Burger", loaded.getKitchenItems().get(0));
    }
}
