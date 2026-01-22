package com.ticketer.controllers;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ticketer.api.ApiResponse;
import com.ticketer.api.ApiStatus;
import com.ticketer.dtos.*;
import com.ticketer.exceptions.TicketerException;
import com.ticketer.models.Menu;
import com.ticketer.models.MenuItem;
import com.ticketer.models.MenuItemView;
import com.ticketer.models.Side;
import com.ticketer.repositories.MenuRepository;
import com.ticketer.services.MenuService;

public class MenuControllerTest {

    private MockMenuService menuService;
    private MenuController menuController;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        menuService = new MockMenuService();
        menuController = new MenuController(menuService);
        mockMvc = MockMvcBuilders.standaloneSetup(menuController)
                .setControllerAdvice(new com.ticketer.exceptions.GlobalExceptionHandler())
                .build();
    }

    @Test
    public void testInitialization() {
        assertNotNull(menuController);
    }

    @Test
    public void testRefreshMenu() {
        menuController.refreshMenu();
        assertTrue(menuService.refreshMenuCalled);
    }

    @Test
    public void testGetItem() throws Exception {
        ApiResponse<ItemDto> response = menuController.getItem("TestItem");
        assertNotNull(response.payload());
        assertEquals("TestItem", response.payload().name());
    }

    @Test
    public void testGetCategory() {
        ApiResponse<List<ItemDto>> response = menuController.getCategory("Entrees");
        assertNotNull(response.payload());
        assertEquals(1, response.payload().size());
        assertEquals("TestItem", response.payload().get(0).name());
    }

    @Test
    public void testGetAllItems() {
        ApiResponse<List<ItemDto>> response = menuController.getAllItems();
        assertNotNull(response.payload());
        assertEquals(1, response.payload().size());
    }

    @Test
    public void testGetCategories() {
        ApiResponse<Map<String, List<ItemDto>>> response = menuController.getCategories();
        assertNotNull(response.payload());
        assertTrue(response.payload().containsKey("Entrees"));
    }

    @Test
    public void testAddItem() {
        menuController.addItem(new Requests.ItemCreateRequest("Entrees", "NewItem", 1000, Collections.emptyMap()));
        assertTrue(menuService.addItemCalled);
    }

    @Test
    public void testEditItemPrice() {
        menuController.editItemPrice("TestItem", new Requests.ItemPriceUpdateRequest(1200));
        assertTrue(menuService.editItemPriceCalled);
    }

    @Test
    public void testEditItemAvailability() {
        menuController.editItemAvailability("TestItem", new Requests.ItemAvailabilityUpdateRequest(false));
        assertTrue(menuService.editItemAvailabilityCalled);
    }

    @Test
    public void testRenameItem() {
        menuController.renameItem("OldName", new Requests.ItemRenameRequest("NewName"));
    }

    @Test
    public void testRemoveItem() {
        menuController.removeItem("TestItem");
        assertTrue(menuService.removeItemCalled);
    }

    @Test
    public void testRenameCategory() {
        menuController.renameCategory("OldCat", new Requests.CategoryRenameRequest("NewCat"));
    }

    @Test
    public void testChangeCategory() {
        menuController.changeCategory("Item", new Requests.ItemCategoryUpdateRequest("NewCat"));
    }

    @Test
    public void testUpdateSide() {
        menuController.updateSide("Item", "Side", new Requests.SideUpdateRequest(500));
    }

    @Test
    public void testNullHandling() throws Exception {
        menuService.returnNull = true;
        ApiResponse<ItemDto> itemResponse = menuController.getItem("Missing");
        assertEquals(ApiStatus.SUCCESS, itemResponse.status());
        assertNull(itemResponse.payload());
    }

    @Test
    public void testExceptions() throws Exception {
        menuService.throwGenericException = true;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/menu/refresh"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value("ERROR"));

        menuService.throwGenericException = false;
        menuService.throwTicketerException = true;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/menu/items/Item"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value("ERROR"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value("Ticketer Error"));
    }

    private static class FakeMenuRepository implements MenuRepository {
        @Override
        public Menu getMenu() {
            return new Menu(new HashMap<>());
        }

        @Override
        public void saveMenu(Menu menu) {
        }
    }

    private static class MockMenuService extends MenuService {
        boolean refreshMenuCalled = false;
        boolean addItemCalled = false;
        boolean editItemPriceCalled = false;
        boolean editItemAvailabilityCalled = false;
        boolean removeItemCalled = false;

        boolean throwGenericException = false;
        boolean throwTicketerException = false;
        boolean returnNull = false;

        public MockMenuService() {
            super(new FakeMenuRepository());
        }

        private void checkExceptions() {
            if (throwTicketerException)
                throw new TicketerException("Ticketer Error", 400);
            if (throwGenericException)
                throw new RuntimeException("Generic Error");
        }

        @Override
        public void refreshMenu() {
            checkExceptions();
            refreshMenuCalled = true;
        }

        @Override
        public List<MenuItemView> getAllItems() {
            checkExceptions();
            if (returnNull)
                return null;
            List<MenuItemView> list = new ArrayList<>();
            list.add(new MenuItemView("TestItem", 1000, true));
            return list;
        }

        @Override
        public Map<String, List<MenuItem>> getCategories() {
            checkExceptions();
            if (returnNull)
                return null;
            Map<String, List<MenuItem>> map = new HashMap<>();
            List<MenuItem> items = new ArrayList<>();
            items.add(getItem("TestItem"));
            map.put("Entrees", items);
            return map;
        }

        @Override
        public MenuItem getItem(String name) {
            checkExceptions();
            if (returnNull || "MissingItem".equals(name))
                return null;
            Map<String, Side> sides = new HashMap<>();
            Side s = new Side();
            s.price = 200;
            s.available = true;
            sides.put("Fries", s);
            return new MenuItem(name, 1000, true, sides);
        }

        @Override
        public String getCategoryOfItem(String name) {
            checkExceptions();
            return "Entrees";
        }

        @Override
        public void addItem(String c, String n, int p, Map<String, Integer> s) {
            checkExceptions();
            addItemCalled = true;
        }

        @Override
        public void editItemPrice(String n, int p) {
            checkExceptions();
            editItemPriceCalled = true;
        }

        @Override
        public void editItemAvailability(String n, boolean a) {
            checkExceptions();
            editItemAvailabilityCalled = true;
        }

        @Override
        public void removeItem(String n) {
            checkExceptions();
            removeItemCalled = true;
        }

        @Override
        public List<MenuItem> getCategory(String categoryName) {
            checkExceptions();
            List<MenuItem> items = new ArrayList<>();
            items.add(getItem("TestItem"));
            return items;
        }

        @Override
        public void renameItem(String o, String n) {
            checkExceptions();
        }

        @Override
        public void renameCategory(String o, String n) {
            checkExceptions();
        }

        @Override
        public void changeCategory(String n, String c) {
            checkExceptions();
        }

        @Override
        public void updateSide(String i, String s, int p) {
            checkExceptions();
        }
    }
}
