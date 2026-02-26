package com.ticketer.services;

import com.ticketer.models.BaseItem;
import com.ticketer.models.CategoryEntry;
import com.ticketer.models.Menu;
import com.ticketer.models.MenuItem;
import com.ticketer.repositories.MenuRepository;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    private MenuService menuService;

    private Menu buildMenuWithItem(String category, String name, long price, boolean kitchen) {
        UUID id = UUID.randomUUID();
        BaseItem item = new BaseItem(id, name, price, true, kitchen);
        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(id, item);
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem(id, Collections.emptyList()));
        CategoryEntry entry = new CategoryEntry(true, items);
        Map<String, CategoryEntry> categories = new LinkedHashMap<>();
        categories.put(category, entry);
        return new Menu(baseItems, categories, new LinkedHashMap<>(), new ArrayList<>(categories.keySet()));
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetMenu() {
        Menu menu = buildMenuWithItem("entrees", "Burger", 100, false);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);
        assertNotNull(menuService.getMenu());
    }

    @Test
    public void testGetBaseItem() {
        Menu menu = buildMenuWithItem("entrees", "Burger", 100, false);
        UUID id = menu.getBaseItems().keySet().iterator().next();
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);
        BaseItem found = menuService.getBaseItem(id);
        assertNotNull(found);
        assertEquals("Burger", found.getName());
    }

    @Test
    public void testGetBaseItemNotFound() {
        when(menuRepository.getMenu()).thenReturn(
                new Menu(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>()));
        menuService = new MenuService(menuRepository);
        assertThrows(EntityNotFoundException.class, () -> menuService.getBaseItem(UUID.randomUUID()));
    }

    @Test
    public void testCreateBaseItem() {
        Menu menu = new Menu(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>());
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        BaseItem created = menuService.createBaseItem("Burger", 1000, true);
        assertNotNull(created.getId());
        assertEquals("Burger", created.getName());
        assertEquals(1000, created.getPrice());
        assertTrue(created.isKitchen());
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testCreateBaseItemEmptyName() {
        when(menuRepository.getMenu()).thenReturn(
                new Menu(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>()));
        menuService = new MenuService(menuRepository);
        assertThrows(InvalidInputException.class, () -> menuService.createBaseItem("", 100, false));
    }

    @Test
    public void testUpdateBaseItemPrice() {
        Menu menu = buildMenuWithItem("entrees", "Burger", 100, false);
        UUID id = menu.getBaseItems().keySet().iterator().next();
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.updateBaseItemPrice(id, 200);
        assertEquals(200, menu.getBaseItem(id).getPrice());
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testUpdateBaseItemAvailability() {
        Menu menu = buildMenuWithItem("entrees", "Burger", 100, false);
        UUID id = menu.getBaseItems().keySet().iterator().next();
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        assertTrue(menu.getBaseItem(id).isAvailable());
        menuService.updateBaseItemAvailability(id, false);
        assertFalse(menu.getBaseItem(id).isAvailable());
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testUpdateBaseItemKitchen() {
        Menu menu = buildMenuWithItem("entrees", "Burger", 100, false);
        UUID id = menu.getBaseItems().keySet().iterator().next();
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        assertFalse(menu.getBaseItem(id).isKitchen());
        menuService.updateBaseItemKitchen(id, true);
        assertTrue(menu.getBaseItem(id).isKitchen());
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testRenameBaseItem() {
        Menu menu = buildMenuWithItem("entrees", "Burger", 100, false);
        UUID id = menu.getBaseItems().keySet().iterator().next();
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.renameBaseItem(id, "Cheeseburger");
        assertEquals("Cheeseburger", menu.getBaseItem(id).getName());
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testDeleteBaseItem() {
        Menu menu = buildMenuWithItem("entrees", "Burger", 100, false);
        UUID id = menu.getBaseItems().keySet().iterator().next();
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.deleteBaseItem(id);
        assertNull(menu.getBaseItem(id));
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testDeleteBaseItemNotFound() {
        when(menuRepository.getMenu()).thenReturn(
                new Menu(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>()));
        menuService = new MenuService(menuRepository);
        assertThrows(EntityNotFoundException.class, () -> menuService.deleteBaseItem(UUID.randomUUID()));
    }

    @Test
    public void testRenameCategory() {
        Menu menu = buildMenuWithItem("entrees", "Burger", 100, false);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.renameCategory("entrees", "mains");
        assertNull(menu.getCategory("entrees"));
        assertNotNull(menu.getCategory("mains"));
        assertTrue(menu.getCategoryOrder().contains("mains"));
        assertFalse(menu.getCategoryOrder().contains("entrees"));
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testRenameCategoryNotFound() {
        when(menuRepository.getMenu()).thenReturn(
                new Menu(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>()));
        menuService = new MenuService(menuRepository);
        assertThrows(EntityNotFoundException.class, () -> menuService.renameCategory("entrees", "mains"));
    }

    @Test
    public void testDeleteCategory() {
        Menu menu = buildMenuWithItem("entrees", "Burger", 100, false);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.deleteCategory("entrees");
        assertNull(menu.getCategory("entrees"));
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testDeleteCategoryNotFound() {
        when(menuRepository.getMenu()).thenReturn(
                new Menu(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>()));
        menuService = new MenuService(menuRepository);
        assertThrows(EntityNotFoundException.class, () -> menuService.deleteCategory("entrees"));
    }

    @Test
    public void testAddMenuItemToCategory() {
        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        UUID id = UUID.randomUUID();
        baseItems.put(id, new BaseItem(id, "Burger", 100, true, false));
        Menu menu = new Menu(baseItems, new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>());
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.addMenuItemToCategory("entrees", id, Collections.emptyList());
        assertNotNull(menu.getCategory("entrees"));
        assertEquals(1, menu.getCategory("entrees").getItems().size());
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testRemoveMenuItemFromCategory() {
        Menu menu = buildMenuWithItem("entrees", "Burger", 100, false);
        UUID id = menu.getCategory("entrees").getItems().get(0).getBaseItemId();
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.removeMenuItemFromCategory("entrees", id);
        assertNull(menu.getCategory("entrees"));
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testSetSideSources() {
        Menu menu = buildMenuWithItem("entrees", "Burger", 100, false);
        UUID id = menu.getCategory("entrees").getItems().get(0).getBaseItemId();
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.setSideSources("entrees", id, List.of("side options"));
        MenuItem mi = menu.findMenuItem("entrees", id);
        assertEquals(1, mi.getSideSources().size());
        assertEquals("side options", mi.getSideSources().get(0));
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testReorderCategories() {
        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        Map<String, CategoryEntry> categories = new LinkedHashMap<>();
        categories.put("mains", new CategoryEntry(true, new ArrayList<>()));
        categories.put("starters", new CategoryEntry(true, new ArrayList<>()));
        Menu menu = new Menu(baseItems, categories, new LinkedHashMap<>(), new ArrayList<>(categories.keySet()));
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        List<String> order = List.of("starters", "mains");
        menuService.reorderCategories(order);
        assertEquals(order, menu.getCategoryOrder());
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testReorderItemsInCategory() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(id1, new BaseItem(id1, "Burger", 100, true, false));
        baseItems.put(id2, new BaseItem(id2, "CheeseBurger", 120, true, false));
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem(id1, Collections.emptyList()));
        items.add(new MenuItem(id2, Collections.emptyList()));
        Map<String, CategoryEntry> categories = new LinkedHashMap<>();
        categories.put("mains", new CategoryEntry(true, items));
        Menu menu = new Menu(baseItems, categories, new LinkedHashMap<>(), new ArrayList<>(categories.keySet()));
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.reorderItemsInCategory("mains", List.of(id2, id1));
        List<MenuItem> reordered = menu.getCategory("mains").getItems();
        assertEquals(id2, reordered.get(0).getBaseItemId());
        assertEquals(id1, reordered.get(1).getBaseItemId());
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testMoveMenuItemToCategory() {
        Menu menu = buildMenuWithItem("entrees", "Burger", 100, false);
        UUID id = menu.getCategory("entrees").getItems().get(0).getBaseItemId();
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.moveMenuItemToCategory("entrees", id, "mains");
        assertNull(menu.getCategory("entrees"));
        assertNotNull(menu.getCategory("mains"));
        assertEquals(id, menu.getCategory("mains").getItems().get(0).getBaseItemId());
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testGetCategoryOrder() {
        Menu menu = buildMenuWithItem("entrees", "Burger", 100, false);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);
        assertTrue(menuService.getCategoryOrder().contains("entrees"));
    }

    @Test
    public void testIsKitchenRelevantItem() {
        UUID id = UUID.randomUUID();
        BaseItem item = new BaseItem(id, "Burger", 1000, true, true);
        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(id, item);
        Menu menu = new Menu(baseItems, new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>());
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        com.ticketer.models.OrderItem orderItem = com.ticketer.models.OrderItem.forItem(id, "Burger", null, null, 1000, 0);
        assertTrue(menuService.isKitchenRelevant(orderItem));
    }

    @Test
    public void testIsKitchenRelevantItemFalse() {
        UUID id = UUID.randomUUID();
        BaseItem item = new BaseItem(id, "Soda", 200, true, false);
        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(id, item);
        Menu menu = new Menu(baseItems, new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>());
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        com.ticketer.models.OrderItem orderItem = com.ticketer.models.OrderItem.forItem(id, "Soda", null, null, 200, 0);
        assertFalse(menuService.isKitchenRelevant(orderItem));
    }

    @Test
    public void testCreateItemOrderItem() {
        UUID id = UUID.randomUUID();
        BaseItem item = new BaseItem(id, "Burger", 1000, true, true);
        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(id, item);
        Menu menu = new Menu(baseItems, new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>());
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        com.ticketer.models.OrderItem orderItem = menuService.createItemOrderItem(id, null);
        assertEquals("Burger", orderItem.getName());
        assertEquals(1000, orderItem.getMainPrice());
        assertEquals(0, orderItem.getSidePrice());
        assertNull(orderItem.getSelectedSide());
    }

    @Test
    public void testCreateItemOrderItemWithSide() {
        UUID mainId = UUID.randomUUID();
        UUID sideId = UUID.randomUUID();
        BaseItem mainItem = new BaseItem(mainId, "Fish", 1200, true, true);
        BaseItem sideItem = new BaseItem(sideId, "Chips", 200, true, false);
        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(mainId, mainItem);
        baseItems.put(sideId, sideItem);
        Menu menu = new Menu(baseItems, new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>());
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        com.ticketer.models.OrderItem orderItem = menuService.createItemOrderItem(mainId, sideId);
        assertEquals("Fish", orderItem.getName());
        assertEquals("Chips", orderItem.getSelectedSide());
        assertEquals(1200, orderItem.getMainPrice());
        assertEquals(200, orderItem.getSidePrice());
        assertEquals(1400, orderItem.getPrice());
    }
}
