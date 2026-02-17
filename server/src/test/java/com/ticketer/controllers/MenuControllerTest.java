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
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.GlobalExceptionHandler;
import com.ticketer.models.MenuItem;
import com.ticketer.models.MenuItemView;
import com.ticketer.services.MenuService;
import com.ticketer.dtos.Requests;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MenuControllerTest {

        @Mock
        private MenuService menuService;

        @InjectMocks
        private MenuController menuController;

        private ObjectMapper mapper = new ObjectMapper();

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
        public void testGetCategory() throws Exception {
                List<MenuItem> items = new ArrayList<>();
                items.add(new MenuItem("TestItem", 1000, true, new HashMap<>(), null, null, null));
                when(menuService.getCategory("Entrees")).thenReturn(items);

                mockMvc.perform(get("/api/menu/categories/Entrees"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload[0].name").value("TestItem"));
        }

        @Test
        public void testGetAllItems() throws Exception {
                Map<String, List<MenuItem>> map = new HashMap<>();
                List<MenuItem> items = new ArrayList<>();
                items.add(new MenuItem("TestItem", 1000, true, new HashMap<>(), null, null, null));
                map.put("Entrees", items);
                when(menuService.getCategories()).thenReturn(map);

                mockMvc.perform(get("/api/menu/items"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.length()").value(1))
                                .andExpect(jsonPath("$.payload[0].name").value("TestItem"));
        }

        @Test
        public void testGetCategories() throws Exception {
                Map<String, List<MenuItem>> map = new HashMap<>();
                List<MenuItem> items = new ArrayList<>();
                items.add(new MenuItem("TestItem", 1000, true, new HashMap<>(), null, null, null));
                map.put("Entrees", items);
                when(menuService.getCategories()).thenReturn(map);

                mockMvc.perform(get("/api/menu/categories"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.Entrees").exists());
        }

        @Test
        public void testAddItem() throws Exception {
                MenuItem item = new MenuItem("NewItem", 1000, true, new HashMap<>(), null, null, null);
                when(menuService.getItem("Entrees", "NewItem")).thenReturn(item);

                String json = "{\"category\":\"Entrees\",\"name\":\"NewItem\",\"price\":1000,\"sides\":{}}";
                mockMvc.perform(post("/api/menu/items")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk());

                verify(menuService).addItem(eq("Entrees"), eq("NewItem"), eq(1000L), any(), eq(false));
        }

        @Test
        public void testEditItemPrice() throws Exception {
                MenuItem item = new MenuItem("TestItem", 1200, true, new HashMap<>(), null, null, null);
                when(menuService.getItem("Entrees", "TestItem")).thenReturn(item);
                when(menuService.getCategories()).thenReturn(new HashMap<>());

                Requests.ItemPriceUpdateRequest request = new Requests.ItemPriceUpdateRequest(1200);
                mockMvc.perform(put("/api/menu/categories/Entrees/items/Burger/price")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(menuService).editItemPrice("Entrees", "Burger", 1200);
        }

        @Test
        public void testEditItemAvailability() throws Exception {
                MenuItem item = new MenuItem("TestItem", 1000, false, new HashMap<>(), null, null, null);
                when(menuService.getItem("Entrees", "TestItem")).thenReturn(item);
                when(menuService.getCategories()).thenReturn(new HashMap<>());

                Requests.ItemAvailabilityUpdateRequest request = new Requests.ItemAvailabilityUpdateRequest(false);
                mockMvc.perform(put("/api/menu/categories/Entrees/items/Burger/availability")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(menuService).editItemAvailability("Entrees", "Burger", false);
        }

        @Test
        public void testRenameItem() throws Exception {
                MenuItem item = new MenuItem("NewName", 1000, true, new HashMap<>(), null, null, null);
                when(menuService.getItem("Entrees", "NewName")).thenReturn(item);
                when(menuService.getCategories()).thenReturn(new HashMap<>());

                Requests.ItemRenameRequest request = new Requests.ItemRenameRequest("Cheeseburger");
                mockMvc.perform(put("/api/menu/categories/Entrees/items/Burger/rename")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(menuService).renameItem("Entrees", "Burger", "Cheeseburger");
        }

        @Test
        public void testRemoveItem() throws Exception {
                when(menuService.getCategories()).thenReturn(new HashMap<>());
                mockMvc.perform(delete("/api/menu/categories/Entrees/items/Burger"))
                                .andExpect(status().isOk());

                verify(menuService).removeItem("Entrees", "Burger");
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
                MenuItem item = new MenuItem("Item", 1000, true, new HashMap<>(), null, null, null);
                when(menuService.getItem("Entrees", "Item")).thenReturn(item);
                when(menuService.getCategories()).thenReturn(new HashMap<>());

                Requests.ItemCategoryUpdateRequest request = new Requests.ItemCategoryUpdateRequest("Mains");
                mockMvc.perform(put("/api/menu/categories/Entrees/items/Burger/category")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(menuService).changeCategory("Entrees", "Burger", "Mains");
        }

        @Test
        public void testUpdateSide() throws Exception {
                MenuItem item = new MenuItem("Item", 1000, true, new HashMap<>(), null, null, null);
                when(menuService.getItem("Entrees", "Item")).thenReturn(item);
                when(menuService.getCategories()).thenReturn(new HashMap<>());

                Requests.SideUpdateRequest request = new Requests.SideUpdateRequest(200L, true, null);
                mockMvc.perform(put("/api/menu/categories/Entrees/items/Burger/sides/Chips")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(menuService).updateSide("Entrees", "Burger", "Chips", 200L, true, null);
        }

        @Test
        public void testNullHandling() throws Exception {
                doThrow(new EntityNotFoundException("Item not found")).when(menuService).getItem("Entrees", "Missing");

                mockMvc.perform(get("/api/menu/categories/Entrees/items/Missing"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value("ERROR"));
        }

        @Test
        public void testExceptions() throws Exception {
                doThrow(new RuntimeException("Generic Error")).when(menuService).refreshMenu();

                mockMvc.perform(post("/api/menu/refresh"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.status").value("ERROR"));

                doThrow(new TicketerException("Ticketer Error", 400)).when(menuService).getItem("Entrees", "Item");

                mockMvc.perform(get("/api/menu/categories/Entrees/items/Item"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("ERROR"))
                                .andExpect(jsonPath("$.message").value("Ticketer Error"));
        }

        @Test
        public void testAddItemInvalid() throws Exception {
                doThrow(new InvalidInputException("Item name cannot be empty"))
                                .when(menuService).addItem(any(), any(), anyLong(), any(), eq(false));

                String json = "{\"category\":\"\",\"name\":\"\",\"price\":-100,\"sides\":{}}";
                mockMvc.perform(post("/api/menu/items")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest());
        }

        @Test
        public void testEditItemPriceInvalid() throws Exception {
                doThrow(new EntityNotFoundException("Item not found")).when(menuService).editItemPrice("Entrees", "Burger", 200);

                Requests.ItemPriceUpdateRequest request = new Requests.ItemPriceUpdateRequest(200);

                mockMvc.perform(put("/api/menu/categories/Entrees/items/Burger/price")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        @Test
        public void testRenameItemInvalid() throws Exception {
                doThrow(new InvalidInputException("New name cannot be empty")).when(menuService).renameItem("Entrees", "Burger", "");

                Requests.ItemRenameRequest request = new Requests.ItemRenameRequest("");

                mockMvc.perform(put("/api/menu/categories/Entrees/items/Burger/rename")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request)))
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
                doThrow(new EntityNotFoundException("Category not found")).when(menuService).changeCategory("Entrees", "Burger", "Missing");

                Requests.ItemCategoryUpdateRequest request = new Requests.ItemCategoryUpdateRequest("Missing");

                mockMvc.perform(put("/api/menu/categories/Entrees/items/Burger/category")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        @Test
        public void testAddSide() throws Exception {
                MenuItem item = new MenuItem("Item", 1000, true, new HashMap<>(), null, null, null);
                when(menuService.getItem("Entrees", "Item")).thenReturn(item);
                when(menuService.getCategories()).thenReturn(new HashMap<>());

                Requests.SideCreateRequest request = new Requests.SideCreateRequest("Fries", 200);
                mockMvc.perform(post("/api/menu/categories/Entrees/items/Burger/sides")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(menuService).addSide("Entrees", "Burger", "Fries", 200);
        }

        @Test
        public void testRemoveSide() throws Exception {
                MenuItem item = new MenuItem("Item", 1000, true, new HashMap<>(), null, null, null);
                when(menuService.getItem("Entrees", "Item")).thenReturn(item);
                when(menuService.getCategories()).thenReturn(new HashMap<>());

                mockMvc.perform(delete("/api/menu/categories/Entrees/items/Burger/sides/Fries"))
                                .andExpect(status().isOk());

                verify(menuService).removeSide("Entrees", "Burger", "Fries");
        }

        @Test
        public void testReorderCategories() throws Exception {
                java.util.List<String> order = java.util.Arrays.asList("Cat1", "Cat2");
                when(menuService.getCategoryOrder()).thenReturn(order);
                when(menuService.getCategories()).thenReturn(new HashMap<>());

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
                when(menuService.getCategories()).thenReturn(new HashMap<>());

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
                MenuItem item = new MenuItem("Item", 100, true, new HashMap<>(), null, null, null);
                when(menuService.getItem("Entrees", "Burger")).thenReturn(item);
                when(menuService.getCategories()).thenReturn(new HashMap<>());

                Requests.SideReorderRequest request = new Requests.SideReorderRequest(order);
                mockMvc.perform(put("/api/menu/categories/Entrees/items/Burger/sides/reorder")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(menuService).reorderSidesInItem("Entrees", "Burger", order);
        }
}
