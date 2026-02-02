package com.ticketer.services;

import com.ticketer.models.Menu;
import com.ticketer.models.MenuItem;
import com.ticketer.models.MenuItemView;
import com.ticketer.repositories.MenuRepository;
import com.ticketer.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    private MenuService menuService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetCategories() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>()));
        data.put("entrees", items);

        when(menuRepository.getMenu()).thenReturn(new Menu(data));
        menuService = new MenuService(menuRepository);

        Map<String, List<MenuItem>> categories = menuService.getCategories();
        assertNotNull(categories);
        assertTrue(categories.containsKey("entrees"));
        assertEquals(1, categories.get("entrees").size());
        assertEquals("Burger", categories.get("entrees").get(0).name);
    }

    @Test
    public void testGetItem() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        MenuItem burger = new MenuItem("Burger", 100, true, new HashMap<>());
        items.add(burger);
        data.put("entrees", items);

        when(menuRepository.getMenu()).thenReturn(new Menu(data));
        menuService = new MenuService(menuRepository);

        MenuItem found = menuService.getItem("Burger");
        assertNotNull(found);
        assertEquals("Burger", found.name);
    }

    @Test
    public void testGetItemNotFound() {
        when(menuRepository.getMenu()).thenReturn(new Menu(new HashMap<>()));
        menuService = new MenuService(menuRepository);
        assertThrows(EntityNotFoundException.class, () -> menuService.getItem("Burger"));
    }

    @Test
    public void testGetAllItems() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>()));
        data.put("entrees", items);

        when(menuRepository.getMenu()).thenReturn(new Menu(data));
        menuService = new MenuService(menuRepository);

        List<MenuItemView> allItems = menuService.getAllItems();
        assertEquals(1, allItems.size());
        assertEquals("Burger", allItems.get(0).name);
    }

    @Test
    public void testRemoveItem() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>()));
        data.put("entrees", items);

        Menu menu = new Menu(data);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.removeItem("Burger");

        assertTrue(data.get("entrees").isEmpty());
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testRemoveItemNotFound() {
        when(menuRepository.getMenu()).thenReturn(new Menu(new HashMap<>()));
        menuService = new MenuService(menuRepository);
        assertThrows(EntityNotFoundException.class, () -> menuService.removeItem("Burger"));
    }

    @Test
    public void testAddItem() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        Menu menu = new Menu(data);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.addItem("Entrees", "Burger", 100, new HashMap<>());

        assertTrue(data.containsKey("entrees"));
        assertEquals(1, data.get("entrees").size());
        assertEquals("Burger", data.get("entrees").get(0).name);
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testEditItemPrice() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>()));
        data.put("entrees", items);

        Menu menu = new Menu(data);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.editItemPrice("Burger", 200);

        assertEquals(200, items.get(0).price);
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testEditItemAvailability() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>()));
        data.put("entrees", items);

        Menu menu = new Menu(data);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.editItemAvailability("Burger", false);

        assertFalse(items.get(0).available);
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testRenameItem() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>()));
        data.put("entrees", items);

        Menu menu = new Menu(data);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.renameItem("Burger", "Cheeseburger");

        assertEquals("Cheeseburger", items.get(0).name);
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testRenameCategory() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        data.put("entrees", new ArrayList<>());

        Menu menu = new Menu(data);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.renameCategory("Entrees", "Mains");

        assertFalse(data.containsKey("entrees"));
        assertTrue(data.containsKey("mains"));
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testRenameCategoryNotFound() {
        when(menuRepository.getMenu()).thenReturn(new Menu(new HashMap<>()));
        menuService = new MenuService(menuRepository);
        assertThrows(EntityNotFoundException.class, () -> menuService.renameCategory("Entrees", "Mains"));
    }
}
