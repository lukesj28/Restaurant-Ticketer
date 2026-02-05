package com.ticketer.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ticketer.exceptions.TicketerException;
import com.ticketer.models.MenuItem;
import com.ticketer.models.MenuItemView;
import com.ticketer.services.MenuService;

public class MenuControllerTest {

    @Mock
    private MenuService menuService;

    @InjectMocks
    private MenuController menuController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(menuController)
                .setControllerAdvice(new com.ticketer.exceptions.GlobalExceptionHandler())
                .build();
    }

    @Test
    public void testInitialization() {
        assertNotNull(menuController);
    }

    @Test
    public void testRefreshMenu() throws Exception {
        mockMvc.perform(post("/api/menu/refresh"))
                .andExpect(status().isOk());
        verify(menuService).refreshMenu();
    }

    @Test
    public void testGetItem() throws Exception {
        MenuItem item = new MenuItem("TestItem", 1000, true, new HashMap<>());
        when(menuService.getItem("TestItem")).thenReturn(item);

        mockMvc.perform(get("/api/menu/items/TestItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.name").value("TestItem"));
    }

    @Test
    public void testGetCategory() throws Exception {
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("TestItem", 1000, true, new HashMap<>()));
        when(menuService.getCategory("Entrees")).thenReturn(items);

        mockMvc.perform(get("/api/menu/categories/Entrees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[0].name").value("TestItem"));
    }

    @Test
    public void testGetAllItems() throws Exception {
        List<MenuItemView> list = new ArrayList<>();
        list.add(new MenuItemView("TestItem", 1000, true));
        when(menuService.getAllItems()).thenReturn(list);
        when(menuService.getItem("TestItem")).thenReturn(new MenuItem("TestItem", 1000, true, new HashMap<>()));

        mockMvc.perform(get("/api/menu/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(1));
    }

    @Test
    public void testGetCategories() throws Exception {
        Map<String, List<MenuItem>> map = new HashMap<>();
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("TestItem", 1000, true, new HashMap<>()));
        map.put("Entrees", items);
        when(menuService.getCategories()).thenReturn(map);

        mockMvc.perform(get("/api/menu/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.Entrees").exists());
    }

    @Test
    public void testAddItem() throws Exception {
        MenuItem item = new MenuItem("NewItem", 1000, true, new HashMap<>());
        when(menuService.getItem("NewItem")).thenReturn(item);

        String json = "{\"category\":\"Entrees\",\"name\":\"NewItem\",\"price\":1000,\"sides\":{}}";
        mockMvc.perform(post("/api/menu/items")
                .contentType("application/json")
                .content(json))
                .andExpect(status().isOk());

        verify(menuService).addItem(eq("Entrees"), eq("NewItem"), eq(1000), any());
    }

    @Test
    public void testEditItemPrice() throws Exception {
        MenuItem item = new MenuItem("TestItem", 1200, true, new HashMap<>());
        when(menuService.getItem("TestItem")).thenReturn(item);

        String json = "{\"newPrice\":1200}";
        mockMvc.perform(put("/api/menu/items/TestItem/price")
                .contentType("application/json")
                .content(json))
                .andExpect(status().isOk());

        verify(menuService).editItemPrice("TestItem", 1200);
    }

    @Test
    public void testEditItemAvailability() throws Exception {
        MenuItem item = new MenuItem("TestItem", 1000, false, new HashMap<>());
        when(menuService.getItem("TestItem")).thenReturn(item);

        String json = "{\"available\":false}";
        mockMvc.perform(put("/api/menu/items/TestItem/availability")
                .contentType("application/json")
                .content(json))
                .andExpect(status().isOk());

        verify(menuService).editItemAvailability("TestItem", false);
    }

    @Test
    public void testRenameItem() throws Exception {
        MenuItem item = new MenuItem("NewName", 1000, true, new HashMap<>());
        when(menuService.getItem("NewName")).thenReturn(item);

        String json = "{\"newName\":\"NewName\"}";
        mockMvc.perform(put("/api/menu/items/OldName/rename")
                .contentType("application/json")
                .content(json))
                .andExpect(status().isOk());

        verify(menuService).renameItem("OldName", "NewName");
    }

    @Test
    public void testRemoveItem() throws Exception {
        mockMvc.perform(delete("/api/menu/items/TestItem"))
                .andExpect(status().isOk());
        verify(menuService).removeItem("TestItem");
    }

    @Test
    public void testRenameCategory() throws Exception {
        when(menuService.getCategory("NewCat")).thenReturn(new ArrayList<>());

        String json = "{\"newCategory\":\"NewCat\"}";
        mockMvc.perform(put("/api/menu/categories/OldCat/rename")
                .contentType("application/json")
                .content(json))
                .andExpect(status().isOk());

        verify(menuService).renameCategory("OldCat", "NewCat");
    }

    @Test
    public void testChangeCategory() throws Exception {
        MenuItem item = new MenuItem("Item", 1000, true, new HashMap<>());
        when(menuService.getItem("Item")).thenReturn(item);

        String json = "{\"newCategory\":\"NewCat\"}";
        mockMvc.perform(put("/api/menu/items/Item/category")
                .contentType("application/json")
                .content(json))
                .andExpect(status().isOk());

        verify(menuService).changeCategory("Item", "NewCat");
    }

    @Test
    public void testUpdateSide() throws Exception {
        MenuItem item = new MenuItem("Item", 1000, true, new HashMap<>());
        when(menuService.getItem("Item")).thenReturn(item);

        String json = "{\"price\":500}";
        mockMvc.perform(put("/api/menu/items/Item/sides/Side")
                .contentType("application/json")
                .content(json))
                .andExpect(status().isOk());

        verify(menuService).updateSide("Item", "Side", 500, null);
    }

    @Test
    public void testNullHandling() throws Exception {
        when(menuService.getItem("Missing")).thenReturn(null);

        mockMvc.perform(get("/api/menu/items/Missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.payload").doesNotExist());
    }

    @Test
    public void testExceptions() throws Exception {
        doThrow(new RuntimeException("Generic Error")).when(menuService).refreshMenu();

        mockMvc.perform(post("/api/menu/refresh"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"));

        doThrow(new TicketerException("Ticketer Error", 400)).when(menuService).getItem("Item");

        mockMvc.perform(get("/api/menu/items/Item"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Ticketer Error"));
    }
}
