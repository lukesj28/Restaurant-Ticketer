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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class MenuServiceAmbiguityTest {

    @Mock
    private MenuRepository menuRepository;

    private MenuService menuService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testUpdatePriceTargetsCorrectBaseItem() {
        UUID drinksCokeId = UUID.randomUUID();
        UUID mixersCokeId = UUID.randomUUID();

        BaseItem drinksCoke = new BaseItem(drinksCokeId, "Coke", 100, true, false);
        BaseItem mixersCoke = new BaseItem(mixersCokeId, "Coke", 150, true, false);

        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(drinksCokeId, drinksCoke);
        baseItems.put(mixersCokeId, mixersCoke);

        List<MenuItem> drinks = new ArrayList<>();
        drinks.add(new MenuItem(drinksCokeId, Collections.emptyList()));

        List<MenuItem> mixers = new ArrayList<>();
        mixers.add(new MenuItem(mixersCokeId, Collections.emptyList()));

        Map<String, CategoryEntry> categories = new LinkedHashMap<>();
        categories.put("drinks", new CategoryEntry(true, drinks));
        categories.put("mixers", new CategoryEntry(true, mixers));

        Menu menu = new Menu(baseItems, categories, new LinkedHashMap<>(), new ArrayList<>(categories.keySet()));
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.updateBaseItemPrice(drinksCokeId, 200);

        assertEquals(200, drinksCoke.getPrice(), "Price of drinks Coke should be updated to 200");
        assertEquals(150, mixersCoke.getPrice(), "Price of mixers Coke should remain 150");
        verify(menuRepository).saveMenu(menu);
    }

    @Test
    public void testRenameTargetsCorrectBaseItem() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        BaseItem item1 = new BaseItem(id1, "Coke", 100, true, false);
        BaseItem item2 = new BaseItem(id2, "Coke", 150, true, false);

        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(id1, item1);
        baseItems.put(id2, item2);

        Map<String, CategoryEntry> categories = new LinkedHashMap<>();
        categories.put("drinks", new CategoryEntry(true, List.of(new MenuItem(id1, Collections.emptyList()))));
        categories.put("mixers", new CategoryEntry(true, List.of(new MenuItem(id2, Collections.emptyList()))));

        Menu menu = new Menu(baseItems, categories, new LinkedHashMap<>(), new ArrayList<>(categories.keySet()));
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.renameBaseItem(id1, "Diet Coke");

        assertEquals("Diet Coke", item1.getName());
        assertEquals("Coke", item2.getName(), "Other item should not be renamed");
    }
}
