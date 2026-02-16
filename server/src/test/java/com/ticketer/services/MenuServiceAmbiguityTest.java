package com.ticketer.services;

import com.ticketer.models.Menu;
import com.ticketer.models.MenuItem;
import com.ticketer.repositories.MenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public void testAmbiguousItemEditing() {
        Map<String, List<MenuItem>> data = new LinkedHashMap<>();
        
        List<MenuItem> drinks = new ArrayList<>();
        MenuItem drinksCoke = new MenuItem("Coke", 100, true, null, null, null, null);
        drinks.add(drinksCoke);
        data.put("drinks", drinks);

        List<MenuItem> mixers = new ArrayList<>();
        MenuItem mixersCoke = new MenuItem("Coke", 150, true, null, null, null, null);
        mixers.add(mixersCoke);
        data.put("mixers", mixers);

        Menu menu = new Menu(data, new ArrayList<>(), new ArrayList<>(data.keySet()));
        when(menuRepository.getMenu()).thenReturn(menu);
        menuService = new MenuService(menuRepository);

        menuService.editItemPrice("drinks", "Coke", 200);

        assertEquals(200, drinksCoke.price, "Price in Drinks category should be updated to 200");
        
        assertEquals(150, mixersCoke.price, "Price in Mixers category should remain 150");
    }
}
