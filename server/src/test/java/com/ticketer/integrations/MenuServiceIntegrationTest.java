package com.ticketer.integrations;

import com.ticketer.models.BaseItem;
import com.ticketer.models.Menu;
import com.ticketer.repositories.FileMenuRepository;
import com.ticketer.services.MenuService;
import com.ticketer.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MenuServiceIntegrationTest {

    private MenuService service;

    @TempDir
    Path tempDir;

    private File testMenuFile;
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        testMenuFile = tempDir.resolve("test-menu-service.json").toFile();
        mapper = new com.ticketer.config.JacksonConfig().objectMapper();
        service = new MenuService(new FileMenuRepository(testMenuFile.getAbsolutePath(), mapper));
    }

    @Test
    public void testInitialization() {
        assertNotNull(service.getMenu());
        assertTrue(service.getMenu().getBaseItems().isEmpty());
    }

    @Test
    public void testCreateBaseItem() {
        BaseItem item = service.createBaseItem("Burger", 1000, false);
        assertNotNull(item.getId());
        assertEquals("Burger", item.getName());
        assertEquals(1000, item.getPrice());
        assertFalse(item.isKitchen());
    }

    @Test
    public void testCreateBaseItemAndAddToCategory() {
        BaseItem item = service.createBaseItem("Fish", 1200, true);
        service.addMenuItemToCategory("Mains", item.getId(), Collections.emptyList());

        Menu menu = service.getMenu();
        assertNotNull(menu.getCategory("Mains"));
        assertEquals(1, menu.getCategory("Mains").getItems().size());
        assertEquals(item.getId(), menu.getCategory("Mains").getItems().get(0).getBaseItemId());
    }

    @Test
    public void testUpdateBaseItemPrice() {
        BaseItem item = service.createBaseItem("Steak", 2000, false);
        service.updateBaseItemPrice(item.getId(), 2500);
        assertEquals(2500, service.getBaseItem(item.getId()).getPrice());
    }

    @Test
    public void testUpdateBaseItemAvailability() {
        BaseItem item = service.createBaseItem("Soup", 500, false);
        assertTrue(item.isAvailable());

        service.updateBaseItemAvailability(item.getId(), false);
        assertFalse(service.getBaseItem(item.getId()).isAvailable());

        service.updateBaseItemAvailability(item.getId(), true);
        assertTrue(service.getBaseItem(item.getId()).isAvailable());
    }

    @Test
    public void testRenameBaseItem() {
        BaseItem item = service.createBaseItem("OldName", 1000, false);
        service.renameBaseItem(item.getId(), "NewName");
        assertEquals("NewName", service.getBaseItem(item.getId()).getName());
    }

    @Test
    public void testDeleteBaseItem() {
        BaseItem item = service.createBaseItem("ToDelete", 1000, false);
        UUID id = item.getId();
        service.deleteBaseItem(id);
        assertThrows(EntityNotFoundException.class, () -> service.getBaseItem(id));
    }

    @Test
    public void testDeleteBaseItemNotFound() {
        assertThrows(EntityNotFoundException.class, () -> service.deleteBaseItem(UUID.randomUUID()));
    }

    @Test
    public void testRenameCategory() {
        BaseItem item = service.createBaseItem("ItemInCat", 1000, false);
        service.addMenuItemToCategory("OldCat", item.getId(), Collections.emptyList());

        service.renameCategory("OldCat", "NewCat");

        Menu menu = service.getMenu();
        assertNull(menu.getCategory("OldCat"));
        assertNotNull(menu.getCategory("NewCat"));
    }

    @Test
    public void testRenameCategoryNotFound() {
        assertThrows(EntityNotFoundException.class, () ->
                service.renameCategory("NonExistentCat", "NewCat"));
    }

    @Test
    public void testMoveMenuItemToCategory() {
        BaseItem item = service.createBaseItem("MoveMe", 1000, false);
        service.addMenuItemToCategory("Cat1", item.getId(), Collections.emptyList());

        service.moveMenuItemToCategory("Cat1", item.getId(), "Cat2");

        Menu menu = service.getMenu();
        assertNull(menu.getCategory("Cat1"));
        assertNotNull(menu.getCategory("Cat2"));
        assertEquals(item.getId(), menu.getCategory("Cat2").getItems().get(0).getBaseItemId());
    }

    @Test
    public void testDeleteCategory() {
        BaseItem item = service.createBaseItem("ItemForDel", 1000, false);
        service.addMenuItemToCategory("DelCat", item.getId(), Collections.emptyList());

        service.deleteCategory("DelCat");

        assertNull(service.getMenu().getCategory("DelCat"));
    }

    @Test
    public void testDeleteCategoryNotFound() {
        assertThrows(EntityNotFoundException.class, () -> service.deleteCategory("GhostCategory"));
    }

    @Test
    public void testGetBaseItemNotFound() {
        assertThrows(EntityNotFoundException.class, () -> service.getBaseItem(UUID.randomUUID()));
    }

    @Test
    public void testCreateItemOrderItem() {
        BaseItem main = service.createBaseItem("Burger", 1200, true);
        BaseItem side = service.createBaseItem("Fries", 200, false);

        com.ticketer.models.OrderItem item = service.createItemOrderItem(main.getId(), side.getId());
        assertNotNull(item);
        assertEquals("Burger", item.getName());
        assertEquals("Fries", item.getSelectedSide());
        assertEquals(1200, item.getMainPrice());
        assertEquals(200, item.getSidePrice());
        assertEquals(1400, item.getPrice());
    }

    @Test
    public void testCreateItemOrderItemNoSide() {
        BaseItem main = service.createBaseItem("Soda", 300, false);

        com.ticketer.models.OrderItem item = service.createItemOrderItem(main.getId(), null);
        assertNotNull(item);
        assertEquals("Soda", item.getName());
        assertNull(item.getSelectedSide());
        assertEquals(300, item.getMainPrice());
        assertEquals(0, item.getSidePrice());
    }

    @Test
    public void testCategoryOrderPreservedAfterSave() {
        BaseItem a = service.createBaseItem("A", 100, false);
        BaseItem b = service.createBaseItem("B", 200, false);
        service.addMenuItemToCategory("Starters", a.getId(), Collections.emptyList());
        service.addMenuItemToCategory("Mains", b.getId(), Collections.emptyList());

        service.reorderCategories(Arrays.asList("Mains", "Starters"));

        MenuService reloaded = new MenuService(new FileMenuRepository(testMenuFile.getAbsolutePath(), mapper));
        assertEquals(Arrays.asList("Mains", "Starters"), reloaded.getCategoryOrder());
    }
}
