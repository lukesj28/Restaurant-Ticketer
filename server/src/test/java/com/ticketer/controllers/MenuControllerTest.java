package com.ticketer.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.ticketer.exceptions.InvalidInputException;
import com.ticketer.exceptions.GlobalExceptionHandler;
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
                                .setControllerAdvice(new GlobalExceptionHandler())
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
                MenuItem item = new MenuItem("TestItem", 1000, true, new HashMap<>(), null);
                when(menuService.getItem("TestItem")).thenReturn(item);

                mockMvc.perform(get("/api/menu/items/TestItem"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.name").value("TestItem"));
        }

        @Test
        public void testGetCategory() throws Exception {
                List<MenuItem> items = new ArrayList<>();
                items.add(new MenuItem("TestItem", 1000, true, new HashMap<>(), null));
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
                when(menuService.getItem("TestItem"))
                                .thenReturn(new MenuItem("TestItem", 1000, true, new HashMap<>(), null));

                mockMvc.perform(get("/api/menu/items"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.length()").value(1));
        }

        @Test
        public void testGetCategories() throws Exception {
                Map<String, List<MenuItem>> map = new HashMap<>();
                List<MenuItem> items = new ArrayList<>();
                items.add(new MenuItem("TestItem", 1000, true, new HashMap<>(), null));
                map.put("Entrees", items);
                when(menuService.getCategories()).thenReturn(map);

                mockMvc.perform(get("/api/menu/categories"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.Entrees").exists());
        }

        @Test
        public void testAddItem() throws Exception {
                MenuItem item = new MenuItem("NewItem", 1000, true, new HashMap<>(), null);
                when(menuService.getItem("NewItem")).thenReturn(item);

                String json = "{\"category\":\"Entrees\",\"name\":\"NewItem\",\"price\":1000,\"sides\":{}}";
                mockMvc.perform(post("/api/menu/items")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk());

                verify(menuService).addItem(eq("Entrees"), eq("NewItem"), eq(1000L), any());
        }

        @Test
        public void testEditItemPrice() throws Exception {
                MenuItem item = new MenuItem("TestItem", 1200, true, new HashMap<>(), null);
                when(menuService.getItem("TestItem")).thenReturn(item);

                String json = "{\"newPrice\":1200}";
                mockMvc.perform(put("/api/menu/items/TestItem/price")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk());

                verify(menuService).editItemPrice("TestItem", 1200L);
        }

        @Test
        public void testEditItemAvailability() throws Exception {
                MenuItem item = new MenuItem("TestItem", 1000, false, new HashMap<>(), null);
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
                MenuItem item = new MenuItem("NewName", 1000, true, new HashMap<>(), null);
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
                MenuItem item = new MenuItem("Item", 1000, true, new HashMap<>(), null);
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
                MenuItem item = new MenuItem("Item", 1000, true, new HashMap<>(), null);
                when(menuService.getItem("Item")).thenReturn(item);

                String json = "{\"price\":500}";
                mockMvc.perform(put("/api/menu/items/Item/sides/Side")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk());

                verify(menuService).updateSide("Item", "Side", 500L, null);
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

        @Test
        public void testAddItemInvalid() throws Exception {
                doThrow(new InvalidInputException("Item name cannot be empty"))
                                .when(menuService).addItem(any(), any(), anyLong(), any());

                String json = "{\"category\":\"\",\"name\":\"\",\"price\":-100,\"sides\":{}}";
                mockMvc.perform(post("/api/menu/items")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest());
        }

        @Test
        public void testEditItemPriceInvalid() throws Exception {
                doThrow(new InvalidInputException("Price cannot be negative"))
                                .when(menuService).editItemPrice(any(), anyLong());

                String json = "{\"newPrice\":-100}";
                mockMvc.perform(put("/api/menu/items/Item/price")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest());
        }

        @Test
        public void testRenameItemInvalid() throws Exception {
                doThrow(new InvalidInputException("Item name cannot be empty"))
                                .when(menuService).renameItem(any(), any());

                String json = "{\"newName\":\"\"}";
                mockMvc.perform(put("/api/menu/items/Item/rename")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest());
        }

        @Test
        public void testRenameCategoryInvalid() throws Exception {
                doThrow(new InvalidInputException("Category cannot be empty"))
                                .when(menuService).renameCategory(any(), any());

                String json = "{\"newCategory\":\"\"}";
                mockMvc.perform(put("/api/menu/categories/Cat/rename")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest());
        }

        @Test
        public void testChangeCategoryInvalid() throws Exception {
                doThrow(new InvalidInputException("Category cannot be empty"))
                                .when(menuService).changeCategory(any(), any());

                String json = "{\"newCategory\":\"\"}";
                mockMvc.perform(put("/api/menu/items/Item/category")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest());
        }

        @Test
        public void testAddSide() throws Exception {
                MenuItem item = new MenuItem("Item", 1000, true, new HashMap<>(), null);
                when(menuService.getItem("Item")).thenReturn(item);

                String json = "{\"name\":\"chips\",\"price\":299}";
                mockMvc.perform(post("/api/menu/items/Item/sides")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk());

                verify(menuService).addSide("Item", "chips", 299);
        }

        @Test
        public void testRemoveSide() throws Exception {
                MenuItem item = new MenuItem("Item", 1000, true, new HashMap<>(), null);
                when(menuService.getItem("Item")).thenReturn(item);

                mockMvc.perform(delete("/api/menu/items/Item/sides/chips"))
                                .andExpect(status().isOk());

                verify(menuService).removeSide("Item", "chips");
        }

        @Test
        public void testReorderCategories() throws Exception {
                java.util.List<String> order = java.util.Arrays.asList("Cat1", "Cat2");
                when(menuService.getCategoryOrder()).thenReturn(order);

                String json = "{\"order\":[\"Cat1\",\"Cat2\"]}";
                mockMvc.perform(put("/api/menu/categories/reorder")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk());

                verify(menuService).reorderCategories(order);
        }

        @Test
        public void testReorderItemsInCategory() throws Exception {
                java.util.List<String> order = java.util.Arrays.asList("Item1", "Item2");
                when(menuService.getCategory("Cat1")).thenReturn(new ArrayList<>());

                String json = "{\"order\":[\"Item1\",\"Item2\"]}";
                mockMvc.perform(put("/api/menu/categories/Cat1/items/reorder")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk());

                verify(menuService).reorderItemsInCategory("Cat1", order);
        }

        @Test
        public void testReorderSidesInItem() throws Exception {
                java.util.List<String> order = java.util.Arrays.asList("Side1", "Side2");
                MenuItem item = new MenuItem("Item", 100, true, new HashMap<>(), null);
                when(menuService.getItem("Item")).thenReturn(item);

                String json = "{\"order\":[\"Side1\",\"Side2\"]}";
                mockMvc.perform(put("/api/menu/items/Item/sides/reorder")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk());

                verify(menuService).reorderSidesInItem("Item", order);
        }
}
