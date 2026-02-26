package com.ticketer.services;

import com.ticketer.models.BaseItem;
import com.ticketer.models.CategoryEntry;
import com.ticketer.models.Menu;
import com.ticketer.models.MenuItem;
import com.ticketer.repositories.MenuRepository;
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

public class MenuServiceSideOrderingTest {

    @Mock
    private MenuRepository menuRepository;

    private MenuService menuService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Menu buildMenuWithItemAndSideCategory() {
        UUID mainId = UUID.randomUUID();
        UUID sideId = UUID.randomUUID();

        BaseItem mainItem = new BaseItem(mainId, "Burger", 1000, true, false);
        BaseItem sideItem = new BaseItem(sideId, "Chips", 200, true, false);

        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(mainId, mainItem);
        baseItems.put(sideId, sideItem);

        List<MenuItem> mainItems = new ArrayList<>();
        mainItems.add(new MenuItem(mainId, Collections.emptyList()));

        List<MenuItem> sideItems = new ArrayList<>();
        sideItems.add(new MenuItem(sideId, Collections.emptyList()));

        Map<String, CategoryEntry> categories = new LinkedHashMap<>();
        categories.put("mains", new CategoryEntry(true, mainItems));
        categories.put("side options", new CategoryEntry(false, sideItems));

        List<String> order = new ArrayList<>(categories.keySet());
        return new Menu(baseItems, categories, new LinkedHashMap<>(), order);
    }

    @Test
    public void testSetSideSourcesAssignsSideCategory() {
        Menu menu = buildMenuWithItemAndSideCategory();
        UUID mainId = menu.getCategory("mains").getItems().get(0).getBaseItemId();
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.setSideSources("mains", mainId, List.of("side options"));

        MenuItem mi = menu.findMenuItem("mains", mainId);
        assertEquals(1, mi.getSideSources().size());
        assertEquals("side options", mi.getSideSources().get(0));
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testSetSideSourcesEmpty() {
        Menu menu = buildMenuWithItemAndSideCategory();
        UUID mainId = menu.getCategory("mains").getItems().get(0).getBaseItemId();
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.setSideSources("mains", mainId, List.of("side options"));
        menuService.setSideSources("mains", mainId, Collections.emptyList());

        MenuItem mi = menu.findMenuItem("mains", mainId);
        assertTrue(mi.getSideSources().isEmpty());
    }

    @Test
    public void testSetSideSourcesMultiple() {
        UUID mainId = UUID.randomUUID();
        UUID side1Id = UUID.randomUUID();
        UUID side2Id = UUID.randomUUID();

        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(mainId, new BaseItem(mainId, "Platter", 2500, true, true));
        baseItems.put(side1Id, new BaseItem(side1Id, "Chips", 200, true, false));
        baseItems.put(side2Id, new BaseItem(side2Id, "Salad", 300, true, false));

        List<MenuItem> mains = new ArrayList<>();
        mains.add(new MenuItem(mainId, Collections.emptyList()));
        List<MenuItem> sides1 = new ArrayList<>();
        sides1.add(new MenuItem(side1Id, Collections.emptyList()));
        List<MenuItem> sides2 = new ArrayList<>();
        sides2.add(new MenuItem(side2Id, Collections.emptyList()));

        Map<String, CategoryEntry> categories = new LinkedHashMap<>();
        categories.put("platters", new CategoryEntry(true, mains));
        categories.put("standard sides", new CategoryEntry(false, sides1));
        categories.put("premium sides", new CategoryEntry(false, sides2));

        Menu menu = new Menu(baseItems, categories, new LinkedHashMap<>(), new ArrayList<>(categories.keySet()));
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.setSideSources("platters", mainId, List.of("standard sides", "premium sides"));

        MenuItem mi = menu.findMenuItem("platters", mainId);
        assertEquals(2, mi.getSideSources().size());
        assertTrue(mi.getSideSources().contains("standard sides"));
        assertTrue(mi.getSideSources().contains("premium sides"));
    }

    @Test
    public void testGetSideOptionsFromMenu() {
        Menu menu = buildMenuWithItemAndSideCategory();
        UUID mainId = menu.getCategory("mains").getItems().get(0).getBaseItemId();
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.setSideSources("mains", mainId, List.of("side options"));
        List<BaseItem> sideOptions = menu.getSideOptions(List.of("side options"));
        assertEquals(1, sideOptions.size());
        assertEquals("Chips", sideOptions.get(0).getName());
    }
}
