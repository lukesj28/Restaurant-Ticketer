package com.ticketer.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketer.dtos.Requests;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.GlobalExceptionHandler;
import com.ticketer.exceptions.InvalidInputException;
import com.ticketer.models.BaseItem;
import com.ticketer.models.Menu;
import com.ticketer.services.MenuService;

public class MenuControllerTest {

    @Mock
    private MenuService menuService;

    @InjectMocks
    private MenuController menuController;

    private final ObjectMapper mapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(menuController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        when(menuService.getMenu()).thenReturn(
                new Menu(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>()));
    }

    @Test
    public void testInitialization() {
        assertNotNull(menuController);
    }

    @Test
    public void testGetMenu() throws Exception {
        mockMvc.perform(get("/api/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
        verify(menuService, atLeastOnce()).getMenu();
    }

    @Test
    public void testRefreshMenu() throws Exception {
        mockMvc.perform(post("/api/menu/refresh"))
                .andExpect(status().isOk());
        verify(menuService).refreshMenu();
    }

    @Test
    public void testGetCategoryOrder() throws Exception {
        when(menuService.getCategoryOrder()).thenReturn(Arrays.asList("Mains", "Starters"));

        mockMvc.perform(get("/api/menu/category-order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[0]").value("Mains"))
                .andExpect(jsonPath("$.payload[1]").value("Starters"));
    }

    @Test
    public void testCreateItem() throws Exception {
        UUID id = UUID.randomUUID();
        BaseItem created = new BaseItem(id, "Burger", 1000, true, false);
        when(menuService.createBaseItem(eq("Burger"), eq(1000L), eq(false), eq(false), isNull())).thenReturn(created);

        String json = "{\"category\":\"Mains\",\"name\":\"Burger\",\"price\":1000,\"kitchen\":false,\"sideSources\":[]}";
        mockMvc.perform(post("/api/menu/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());

        verify(menuService).createBaseItem("Burger", 1000L, false, false, null);
        verify(menuService).addMenuItemToCategory(eq("Mains"), eq(id), any());
    }

    @Test
    public void testCreateItemInvalidName() throws Exception {
        when(menuService.createBaseItem(eq(""), anyLong(), anyBoolean(), anyBoolean(), any()))
                .thenThrow(new InvalidInputException("Item name cannot be empty"));

        String json = "{\"category\":\"Mains\",\"name\":\"\",\"price\":100}";
        mockMvc.perform(post("/api/menu/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    public void testGetItem() throws Exception {
        UUID id = UUID.randomUUID();
        BaseItem item = new BaseItem(id, "Fish", 1200, true, true);
        when(menuService.getBaseItem(id)).thenReturn(item);

        mockMvc.perform(get("/api/menu/items/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.name").value("Fish"))
                .andExpect(jsonPath("$.payload.price").value(1200));
    }

    @Test
    public void testGetItemNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(menuService.getBaseItem(id)).thenThrow(new EntityNotFoundException("Base item not found"));

        mockMvc.perform(get("/api/menu/items/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    public void testUpdateItemPrice() throws Exception {
        UUID id = UUID.randomUUID();
        Requests.ItemPriceUpdateRequest request = new Requests.ItemPriceUpdateRequest(1500);

        mockMvc.perform(put("/api/menu/items/" + id + "/price")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(menuService).updateBaseItemPrice(id, 1500L);
    }

    @Test
    public void testUpdateItemAvailability() throws Exception {
        UUID id = UUID.randomUUID();
        Requests.ItemAvailabilityUpdateRequest request = new Requests.ItemAvailabilityUpdateRequest(false);

        mockMvc.perform(put("/api/menu/items/" + id + "/availability")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(menuService).updateBaseItemAvailability(id, false);
    }

    @Test
    public void testUpdateItemKitchen() throws Exception {
        UUID id = UUID.randomUUID();
        Requests.ItemKitchenUpdateRequest request = new Requests.ItemKitchenUpdateRequest(true);

        mockMvc.perform(put("/api/menu/items/" + id + "/kitchen")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(menuService).updateBaseItemKitchen(id, true);
    }

    @Test
    public void testRenameItem() throws Exception {
        UUID id = UUID.randomUUID();
        Requests.ItemRenameRequest request = new Requests.ItemRenameRequest("Cheeseburger");

        mockMvc.perform(put("/api/menu/items/" + id + "/rename")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(menuService).renameBaseItem(id, "Cheeseburger");
    }

    @Test
    public void testUpdateItemComponents() throws Exception {
        UUID id = UUID.randomUUID();
        UUID subId = UUID.randomUUID();
        String json = "{\"components\":[{\"baseItemId\":\"" + subId + "\",\"quantity\":2.0}]}";

        mockMvc.perform(put("/api/menu/items/" + id + "/components")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());

        verify(menuService).updateBaseItemComponents(eq(id), argThat(list ->
                list != null && list.size() == 1 && subId.equals(list.get(0).getBaseItemId())));
    }

    @Test
    public void testDeleteItem() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/menu/items/" + id))
                .andExpect(status().isOk());

        verify(menuService).deleteBaseItem(id);
    }

    @Test
    public void testDeleteItemNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Item not found")).when(menuService).deleteBaseItem(id);

        mockMvc.perform(delete("/api/menu/items/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    public void testSetCategoryVisible() throws Exception {
        Requests.CategoryVisibleRequest request = new Requests.CategoryVisibleRequest(false);

        mockMvc.perform(put("/api/menu/categories/Mains/visible")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(menuService).setCategoryVisible("Mains", false);
    }

    @Test
    public void testRenameCategory() throws Exception {
        Requests.CategoryRenameRequest request = new Requests.CategoryRenameRequest("NewCat");

        mockMvc.perform(put("/api/menu/categories/OldCat/rename")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(menuService).renameCategory("OldCat", "NewCat");
    }

    @Test
    public void testRenameCategoryNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Category not found"))
                .when(menuService).renameCategory(any(), any());

        String json = "{\"newCategory\":\"NewCat\"}";
        mockMvc.perform(put("/api/menu/categories/Missing/rename")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    public void testDeleteCategory() throws Exception {
        mockMvc.perform(delete("/api/menu/categories/Mains"))
                .andExpect(status().isOk());

        verify(menuService).deleteCategory("Mains");
    }

    @Test
    public void testDeleteCategoryNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Category not found"))
                .when(menuService).deleteCategory("Missing");

        mockMvc.perform(delete("/api/menu/categories/Missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    public void testReorderCategories() throws Exception {
        List<String> order = Arrays.asList("Starters", "Mains");
        when(menuService.getCategoryOrder()).thenReturn(order);

        String json = "{\"order\":[\"Starters\",\"Mains\"]}";
        mockMvc.perform(put("/api/menu/categories/reorder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());

        verify(menuService).reorderCategories(order);
    }

    @Test
    public void testAddItemToCategory() throws Exception {
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(post("/api/menu/categories/Mains/items/" + itemId))
                .andExpect(status().isOk());

        verify(menuService).addMenuItemToCategory(eq("Mains"), eq(itemId), any());
    }

    @Test
    public void testRemoveItemFromCategory() throws Exception {
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(delete("/api/menu/categories/Mains/items/" + itemId))
                .andExpect(status().isOk());

        verify(menuService).removeMenuItemFromCategory("Mains", itemId);
    }

    @Test
    public void testSetSideSources() throws Exception {
        UUID itemId = UUID.randomUUID();
        String json = "{\"sideSources\":[\"side options\"]}";

        mockMvc.perform(put("/api/menu/categories/Mains/items/" + itemId + "/side-sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());

        verify(menuService).setSideSources(eq("Mains"), eq(itemId), any());
    }

    @Test
    public void testMoveItemToCategory() throws Exception {
        UUID itemId = UUID.randomUUID();
        Requests.ItemCategoryUpdateRequest request = new Requests.ItemCategoryUpdateRequest("Starters");

        mockMvc.perform(put("/api/menu/categories/Mains/items/" + itemId + "/category")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(menuService).moveMenuItemToCategory("Mains", itemId, "Starters");
    }

    @Test
    public void testReorderItemsInCategory() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        String json = "{\"order\":[\"" + id1 + "\",\"" + id2 + "\"]}";

        mockMvc.perform(put("/api/menu/categories/Mains/items/reorder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());

        verify(menuService).reorderItemsInCategory(eq("Mains"), any());
    }

    @Test
    public void testExceptions() throws Exception {
        doThrow(new RuntimeException("Generic Error")).when(menuService).refreshMenu();

        mockMvc.perform(post("/api/menu/refresh"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }
}
