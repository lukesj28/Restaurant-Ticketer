package com.ticketer.repositories;

import com.ticketer.models.Menu;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class FileMenuRepositoryTest {

    private static final String TEST_FILE = "target/repo-test-menu.json";
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @Before
    public void setUp() {
        new File(TEST_FILE).delete();
        mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
    }

    @After
    public void tearDown() {
        new File(TEST_FILE).delete();
    }

    @Test
    public void testGetMenuFileNotFound() {
        FileMenuRepository repo = new FileMenuRepository(TEST_FILE, mapper);
        Menu menu = repo.getMenu();
        assertNotNull(menu);
        assertTrue(menu.getCategories().isEmpty());
    }

    @Test
    public void testGetMenuCorruptedJson() throws IOException {
        Files.write(new File(TEST_FILE).toPath(), "{ invalid json ".getBytes());
        FileMenuRepository repo = new FileMenuRepository(TEST_FILE, mapper);
        try {
            repo.getMenu();
            fail("Should throw exception for corrupted JSON");
        } catch (RuntimeException e) {

        }
    }

    @Test
    public void testSaveAndLoad() {
        FileMenuRepository repo = new FileMenuRepository(TEST_FILE, mapper);
        Menu menu = new Menu(new java.util.HashMap<>());
        repo.saveMenu(menu);

        File f = new File(TEST_FILE);
        assertTrue(f.exists());

        Menu loaded = repo.getMenu();
        assertNotNull(loaded);
        assertTrue(loaded.getCategories().isEmpty());
    }
}
