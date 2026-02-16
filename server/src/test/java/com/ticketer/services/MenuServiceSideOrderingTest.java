package com.ticketer.services;

import com.ticketer.models.Menu;
import com.ticketer.models.MenuItem;
import com.ticketer.models.Side;
import com.ticketer.repositories.MenuRepository;
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

public class MenuServiceSideOrderingTest {

    @Mock
    private MenuRepository menuRepository;

    private MenuService menuService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAddSideEnforcesNoneAtBottom() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        
        Map<String, Side> sides = new HashMap<>();
        sides.put("none", new Side());
        MenuItem burger = new MenuItem("Burger", 100, true, sides, null, null, null);
        if (burger.sideOrder == null) burger.sideOrder = new ArrayList<>(sides.keySet());
        
        items.add(burger);
        data.put("mains", items);

        Menu menu = new Menu(data, new ArrayList<>(), null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.addSide("mains", "Burger", "Chips", 200);

        MenuItem updated = menuService.getItem("mains", "Burger");
        List<String> order = updated.sideOrder;
        
        assertEquals(2, order.size());
        assertEquals("none", order.get(order.size() - 1));
        assertTrue(order.contains("Chips"));
    }

    @Test
    public void testReorderSidesEnforcesNoneAtBottom() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        
        Map<String, Side> sides = new HashMap<>();
        sides.put("none", new Side());
        sides.put("Chips", new Side());
        sides.put("Salad", new Side());
        
        MenuItem burger = new MenuItem("Burger", 100, true, sides, null, null, null);
        if (burger.sideOrder == null) burger.sideOrder = new ArrayList<>(sides.keySet());
        
        items.add(burger);
        data.put("mains", items);

        Menu menu = new Menu(data, new ArrayList<>(), null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        List<String> requestedOrder = new ArrayList<>();
        requestedOrder.add("none");
        requestedOrder.add("Salad");
        requestedOrder.add("Chips");

        menuService.reorderSidesInItem("mains", "Burger", requestedOrder);

        MenuItem updated = menuService.getItem("mains", "Burger");
        List<String> order = updated.sideOrder;
        
        assertEquals(3, order.size());
        assertEquals("none", order.get(order.size() - 1));
        assertEquals("Salad", order.get(0));
        assertEquals("Chips", order.get(1));
    }
    
    @Test
    public void testReorderSidesAppendsNoneIfMissing() {
        Map<String, List<MenuItem>> data = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        
        Map<String, Side> sides = new HashMap<>();
        sides.put("none", new Side());
        sides.put("Chips", new Side());
        sides.put("Salad", new Side());
        
        MenuItem burger = new MenuItem("Burger", 100, true, sides, null, null, null);
        if (burger.sideOrder == null) burger.sideOrder = new ArrayList<>(sides.keySet());
        
        items.add(burger);
        data.put("mains", items);

        Menu menu = new Menu(data, new ArrayList<>(), null);
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        List<String> requestedOrder = new ArrayList<>();
        requestedOrder.add("Salad");
        requestedOrder.add("Chips");

        menuService.reorderSidesInItem("mains", "Burger", requestedOrder);

        MenuItem updated = menuService.getItem("mains", "Burger");
        List<String> order = updated.sideOrder;
        
        assertEquals(3, order.size());
        assertEquals("none", order.get(order.size() - 1));
        assertEquals("Salad", order.get(0));
        assertEquals("Chips", order.get(1));
    }
}
