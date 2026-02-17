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
        items.add(new MenuItem("Burger", 100, true, new HashMap<>(), null, null, null));
        data.put("entrees", items);

        when(menuRepository.getMenu()).thenReturn(new Menu(data, null));
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
        MenuItem burger = new MenuItem("Burger", 100, true, new HashMap<>(), null, null, null);
        items.add(burger);
        data.put("entrees", items);

        when(menuRepository.getMenu()).thenReturn(new Menu(data, null));
        menuService = new MenuService(menuRepository);

        MenuItem found = menuService.getItem("entrees", "Burger");
        assertNotNull(found);
        assertEquals("Burger", found.name);
    }

    @Test
    public void testGetItemNotFound() {
        when(menuRepository.getMenu()).thenReturn(new Menu(new HashMap<>(), null));
        menuService = new MenuService(menuRepository);
        assertThrows(EntityNotFoundException.class, () -> menuService.getItem("entrees", "Burger"));
    }

    @Test
    public void testGetAllItems() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>(), null, null, null));
        data.put("entrees", items);

        when(menuRepository.getMenu()).thenReturn(new Menu(data, null));
        menuService = new MenuService(menuRepository);

        List<MenuItemView> allItems = menuService.getAllItems();
        assertEquals(1, allItems.size());
        assertEquals("Burger", allItems.get(0).name);
    }

    @Test
    public void testRemoveItem() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>(), null, null, null));
        data.put("entrees", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.removeItem("entrees", "Burger");

        assertFalse(data.containsKey("entrees"));
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testRemoveItemNotFound() {
        when(menuRepository.getMenu()).thenReturn(new Menu(new HashMap<>(), null));
        menuService = new MenuService(menuRepository);
        assertThrows(EntityNotFoundException.class, () -> menuService.removeItem("entrees", "Burger"));
    }

    @Test
    public void testAddItem() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        Menu menu = new Menu(data, new ArrayList<>());
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.addItem("Entrees", "Burger", 100, new HashMap<>());

        assertTrue(data.containsKey("entrees"));
        assertEquals(1, data.get("entrees").size());
        assertEquals("Burger", data.get("entrees").get(0).name);
        assertTrue(menu.getCategoryOrder().contains("entrees"));
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testEditItemPrice() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>(), null, null, null));
        data.put("entrees", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.editItemPrice("entrees", "Burger", 200);

        assertEquals(200, items.get(0).price);
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testEditItemAvailability() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>(), null, null, null));
        data.put("entrees", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.editItemAvailability("entrees", "Burger", false);

        assertFalse(items.get(0).available);
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testRenameItem() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>(), null, null, null));
        data.put("entrees", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.renameItem("entrees", "Burger", "Cheeseburger");

        assertEquals("Cheeseburger", items.get(0).name);
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testRenameCategory() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        data.put("entrees", new ArrayList<>());

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.renameCategory("Entrees", "Mains");

        assertFalse(data.containsKey("entrees"));
        assertTrue(data.containsKey("mains"));
        assertTrue(menu.getCategoryOrder().contains("mains"));
        assertFalse(menu.getCategoryOrder().contains("entrees"));
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testRenameCategoryNotFound() {
        when(menuRepository.getMenu()).thenReturn(new Menu(new HashMap<>(), null));
        menuService = new MenuService(menuRepository);
        assertThrows(EntityNotFoundException.class, () -> menuService.renameCategory("Entrees", "Mains"));
    }

    @Test
    public void testEditItemKitchen() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>(), null, null, null));
        data.put("entrees", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        assertFalse(items.get(0).kitchen);

        menuService.editItemKitchen("entrees", "Burger", true);
        assertTrue(items.get(0).kitchen);
        verify(menuRepository, times(1)).saveMenu(menu);

        menuService.editItemKitchen("entrees", "Burger", false);
        assertFalse(items.get(0).kitchen);
        verify(menuRepository, times(2)).saveMenu(menu);
    }

    @Test
    public void testIsKitchenRelevant() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        Map<String, com.ticketer.models.Side> sides = new HashMap<>();
        com.ticketer.models.Side chipsSide = new com.ticketer.models.Side();
        chipsSide.price = 299;
        chipsSide.available = true;
        chipsSide.kitchen = true;
        sides.put("chips", chipsSide);
        MenuItem burger = new MenuItem("Burger", 100, true, sides, null, null, null);
        burger.kitchen = false;
        items.add(burger);
        data.put("entrees", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        com.ticketer.models.OrderItem orderItem = new com.ticketer.models.OrderItem("test-category", "Burger", "chips", null, 100, 299, 0, null);
        assertTrue(menuService.isKitchenRelevant(orderItem));

        com.ticketer.models.OrderItem orderItem2 = new com.ticketer.models.OrderItem("test-category", "Burger", null, null, 100, 0, 0, null);
        assertFalse(menuService.isKitchenRelevant(orderItem2));

        burger.kitchen = true;
        assertTrue(menuService.isKitchenRelevant(orderItem2));
    }

    @Test
    public void testGetKitchenTally() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        Map<String, com.ticketer.models.Side> sides = new HashMap<>();
        com.ticketer.models.Side chipsSide = new com.ticketer.models.Side();
        chipsSide.price = 299;
        chipsSide.available = true;
        chipsSide.kitchen = true;
        sides.put("chips", chipsSide);
        MenuItem burger = new MenuItem("Burger", 100, true, sides, null, null, null);
        burger.kitchen = true;
        items.add(burger);
        data.put("entrees", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        com.ticketer.models.Ticket ticket = new com.ticketer.models.Ticket(1);
        com.ticketer.models.Order order = new com.ticketer.models.Order();
        order.addItem(new com.ticketer.models.OrderItem("test-category", "Burger", "chips", null, 100, 299, 0, null));
        order.addItem(new com.ticketer.models.OrderItem("test-category", "Burger", null, null, 100, 0, 0, null));
        ticket.addOrder(order);

        java.util.Map<String, Integer> tally = menuService.getKitchenTally(ticket);
        assertEquals(2, tally.get("Burger"));
        assertEquals(1, tally.get("chips"));
    }

    @Test
    public void testRemoveItemEmptyCategory() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>(), null, null, null));
        data.put("entrees", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.removeItem("entrees", "Burger");

        assertFalse(data.containsKey("entrees"));
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testChangeCategoryEmptyOldCategory() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>(), null, null, null));
        data.put("entrees", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.changeCategory("entrees", "Burger", "Mains");

        assertFalse(data.containsKey("entrees"));
        assertTrue(data.containsKey("mains"));
        assertEquals(1, data.get("mains").size());
        assertTrue(menu.getCategoryOrder().contains("mains"));
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testDeleteCategory() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, new HashMap<>(), null, null, null));
        data.put("entrees", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.deleteCategory("Entrees");

        assertFalse(data.containsKey("entrees"));
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testDeleteCategoryNotFound() {
        when(menuRepository.getMenu()).thenReturn(new Menu(new HashMap<>(), null));
        menuService = new MenuService(menuRepository);
        assertThrows(EntityNotFoundException.class, () -> menuService.deleteCategory("Entrees"));
    }

    @Test
    public void testAddSideAutoAddsNone() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, null, null, null, null));
        data.put("entrees", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.addSide("entrees", "Burger", "chips", 299);

        MenuItem burger = menuService.getItem("entrees", "Burger");
        assertNotNull(burger.sideOptions);
        assertTrue(burger.sideOptions.containsKey("chips"));
        assertTrue(burger.sideOptions.containsKey("none"));
        assertEquals(0, burger.sideOptions.get("none").price);
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testRemoveSideRemovesSidesWhenOnlyNoneRemains() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        Map<String, com.ticketer.models.Side> sides = new HashMap<>();
        com.ticketer.models.Side chipsSide = new com.ticketer.models.Side();
        chipsSide.price = 299;
        chipsSide.available = true;
        sides.put("chips", chipsSide);
        com.ticketer.models.Side noneSide = new com.ticketer.models.Side();
        noneSide.price = 0;
        noneSide.available = true;
        sides.put("none", noneSide);
        items.add(new MenuItem("Burger", 100, true, sides, null, null, null));
        data.put("entrees", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.removeSide("entrees", "Burger", "chips");

        MenuItem burger = menuService.getItem("entrees", "Burger");
        assertNull(burger.sideOptions);
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testRemoveSideNoneNotAllowed() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        Map<String, com.ticketer.models.Side> sides = new HashMap<>();
        com.ticketer.models.Side noneSide = new com.ticketer.models.Side();
        noneSide.price = 0;
        noneSide.available = true;
        sides.put("none", noneSide);
        items.add(new MenuItem("Burger", 100, true, sides, null, null, null));
        data.put("entrees", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        assertThrows(com.ticketer.exceptions.InvalidInputException.class,
                () -> menuService.removeSide("entrees", "Burger", "none"));
    }

    @Test
    public void testAddSideNoneNotAllowed() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Burger", 100, true, null, null, null, null));
        data.put("entrees", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        assertThrows(com.ticketer.exceptions.InvalidInputException.class,
                () -> menuService.addSide("entrees", "Burger", "none", 0));
    }

    @Test
    public void testReorderCategories() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        data.put("mains", new ArrayList<>());
        data.put("starters", new ArrayList<>());

        Menu menu = new Menu(data, new ArrayList<>());
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        List<String> order = java.util.Arrays.asList("mains", "starters");
        menuService.reorderCategories(order);

        assertEquals(order, menu.getCategoryOrder());
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testReorderCategoriesMissingCategory() {
        when(menuRepository.getMenu()).thenReturn(new Menu(new HashMap<>(), null));
        menuService = new MenuService(menuRepository);

        List<String> order = java.util.Arrays.asList("missing");
        assertThrows(EntityNotFoundException.class, () -> menuService.reorderCategories(order));
    }

    @Test
    public void testReorderItemsInCategory() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        MenuItem b1 = new MenuItem("Burger", 100, true, null, null, null, null);
        MenuItem b2 = new MenuItem("CheeseBurger", 120, true, null, null, null, null);
        items.add(b1);
        items.add(b2);
        data.put("mains", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        List<String> order = java.util.Arrays.asList("CheeseBurger", "Burger");
        menuService.reorderItemsInCategory("mains", order);

        List<MenuItem> reordered = menuService.getCategory("mains");
        assertEquals("CheeseBurger", reordered.get(0).name);
        assertEquals("Burger", reordered.get(1).name);
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testReorderSidesInItem() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        Map<String, com.ticketer.models.Side> sides = new HashMap<>();
        sides.put("Fries", new com.ticketer.models.Side());
        sides.put("Salad", new com.ticketer.models.Side());

        MenuItem burger = new MenuItem("Burger", 100, true, sides, null, null, null);
        items.add(burger);
        data.put("mains", items);

        Menu menu = new Menu(data, null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        List<String> order = java.util.Arrays.asList("Salad", "Fries");
        menuService.reorderSidesInItem("mains", "Burger", order);

        MenuItem updated = menuService.getItem("mains", "Burger");
        assertEquals(order, updated.sideOrder);
        verify(menuRepository).saveMenu(menu);
    }
}
