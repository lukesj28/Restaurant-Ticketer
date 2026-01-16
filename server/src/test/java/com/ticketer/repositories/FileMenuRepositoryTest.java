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

    @Before
    public void setUp() {
        new File(TEST_FILE).delete();
    }

    @After
    public void tearDown() {
        new File(TEST_FILE).delete();
    }

    @Test(expected = RuntimeException.class)
    public void testGetMenuFileNotFound() {
        FileMenuRepository repo = new FileMenuRepository(TEST_FILE);
        repo.getMenu();
    }

    @Test
    public void testGetMenuCorruptedJson() throws IOException {
        Files.write(new File(TEST_FILE).toPath(), "{ invalid json ".getBytes());
        FileMenuRepository repo = new FileMenuRepository(TEST_FILE);
        try {
            repo.getMenu();
            fail("Should throw exception for corrupted JSON");
        } catch (com.google.gson.JsonSyntaxException e) {

        } catch (RuntimeException e) {

        }
    }

    @Test
    public void testSaveAndLoad() {
        FileMenuRepository repo = new FileMenuRepository(TEST_FILE);
        Menu menu = new Menu(new java.util.HashMap<>());
        repo.saveMenu(menu);

        File f = new File(TEST_FILE);
        assertTrue(f.exists());

        Menu loaded = repo.getMenu();
        assertNotNull(loaded);
        assertTrue(loaded.getCategories().isEmpty());
    }
}
