package com.ticketer.controllers;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.ticketer.api.ApiResponse;
import com.ticketer.dtos.*;
import com.ticketer.models.BaseItem;
import com.ticketer.models.ComboItem;
import com.ticketer.models.CompositeComponent;
import com.ticketer.services.MenuService;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MenuController.class);

    private final MenuService menuService;

    @Autowired
    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("")
    public ApiResponse<MenuDto> getMenu() {
        return ApiResponse.success(DtoMapper.toMenuDto(menuService.getMenu()));
    }

    @PostMapping("/refresh")
    public ApiResponse<List<String>> refreshMenu() {
        logger.info("Received request to refresh menu");
        menuService.refreshMenu();
        return ApiResponse.success(Collections.emptyList());
    }

    @GetMapping("/category-order")
    public ApiResponse<List<String>> getCategoryOrder() {
        return ApiResponse.success(menuService.getCategoryOrder());
    }

    @PostMapping("/items")
    public ApiResponse<MenuDto> createItem(@RequestBody Requests.ItemCreateRequest request) {
        logger.info("Received request to create item: {} in category: {}", request.name(), request.category());
        boolean kitchen = request.kitchen() != null && request.kitchen();
        boolean alcohol = request.alcohol() != null && request.alcohol();
        List<CompositeComponent> components = (request.components() != null && !request.components().isEmpty())
                ? request.components().stream()
                        .filter(c -> c.baseItemId() != null)
                        .map(c -> new CompositeComponent(c.baseItemId(), c.quantity()))
                        .collect(Collectors.toList())
                : null;
        BaseItem item = menuService.createBaseItem(request.name(), request.price(), kitchen, alcohol, components);
        menuService.addMenuItemToCategory(request.category(), item.getId(), request.sideSources());
        return getMenu();
    }

    @GetMapping("/items/{id}")
    public ApiResponse<BaseItemDto> getItem(@PathVariable UUID id) {
        return ApiResponse.success(DtoMapper.toBaseItemDto(menuService.getBaseItem(id)));
    }

    @PutMapping("/items/{id}/price")
    public ApiResponse<MenuDto> updateItemPrice(@PathVariable UUID id,
            @RequestBody Requests.ItemPriceUpdateRequest request) {
        menuService.updateBaseItemPrice(id, request.newPrice());
        return getMenu();
    }

    @PutMapping("/items/{id}/availability")
    public ApiResponse<MenuDto> updateItemAvailability(@PathVariable UUID id,
            @RequestBody Requests.ItemAvailabilityUpdateRequest request) {
        menuService.updateBaseItemAvailability(id, request.available());
        return getMenu();
    }

    @PutMapping("/items/{id}/kitchen")
    public ApiResponse<MenuDto> updateItemKitchen(@PathVariable UUID id,
            @RequestBody Requests.ItemKitchenUpdateRequest request) {
        menuService.updateBaseItemKitchen(id, request.kitchen());
        return getMenu();
    }

    @PutMapping("/items/{id}/alcohol")
    public ApiResponse<MenuDto> updateItemAlcohol(@PathVariable UUID id,
            @RequestBody Requests.ItemAlcoholUpdateRequest request) {
        menuService.updateBaseItemAlcohol(id, request.alcohol());
        return getMenu();
    }

    @PutMapping("/items/{id}/rename")
    public ApiResponse<MenuDto> renameItem(@PathVariable UUID id,
            @RequestBody Requests.ItemRenameRequest request) {
        menuService.renameBaseItem(id, request.newName());
        return getMenu();
    }

    @PutMapping("/items/{id}/components")
    public ApiResponse<MenuDto> updateItemComponents(@PathVariable UUID id,
            @RequestBody Requests.ItemComponentsUpdateRequest request) {
        List<CompositeComponent> components = request.components() == null ? null :
            request.components().stream()
                .filter(c -> c.baseItemId() != null)
                .map(c -> new CompositeComponent(c.baseItemId(), c.quantity()))
                .collect(Collectors.toList());
        menuService.updateBaseItemComponents(id, components);
        return getMenu();
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<MenuDto> deleteItem(@PathVariable UUID id) {
        logger.info("Received request to delete base item: {}", id);
        menuService.deleteBaseItem(id);
        return getMenu();
    }

    @PutMapping("/categories/{categoryName}/visible")
    public ApiResponse<MenuDto> setCategoryVisible(@PathVariable String categoryName,
            @RequestBody Requests.CategoryVisibleRequest request) {
        menuService.setCategoryVisible(categoryName, request.visible());
        return getMenu();
    }

    @PutMapping("/categories/{categoryName}/rename")
    public ApiResponse<MenuDto> renameCategory(@PathVariable String categoryName,
            @RequestBody Requests.CategoryRenameRequest request) {
        menuService.renameCategory(categoryName, request.newCategory());
        return getMenu();
    }

    @DeleteMapping("/categories/{categoryName}")
    public ApiResponse<List<String>> deleteCategory(@PathVariable String categoryName) {
        menuService.deleteCategory(categoryName);
        return ApiResponse.success(Collections.emptyList());
    }

    @PutMapping("/categories/reorder")
    public ApiResponse<List<String>> reorderCategories(@RequestBody Requests.CategoryReorderRequest request) {
        menuService.reorderCategories(request.order());
        return ApiResponse.success(menuService.getCategoryOrder());
    }

    @PostMapping("/categories/{categoryName}/items/{itemId}")
    public ApiResponse<MenuDto> addItemToCategory(@PathVariable String categoryName,
            @PathVariable UUID itemId,
            @RequestBody(required = false) Requests.SetSideSourcesRequest request) {
        List<String> sideSources = request != null ? request.sideSources() : null;
        menuService.addMenuItemToCategory(categoryName, itemId, sideSources);
        return getMenu();
    }

    @DeleteMapping("/categories/{categoryName}/items/{itemId}")
    public ApiResponse<MenuDto> removeItemFromCategory(@PathVariable String categoryName,
            @PathVariable UUID itemId) {
        menuService.removeMenuItemFromCategory(categoryName, itemId);
        return getMenu();
    }

    @PutMapping("/categories/{categoryName}/items/{itemId}/side-sources")
    public ApiResponse<MenuDto> setSideSources(@PathVariable String categoryName,
            @PathVariable UUID itemId,
            @RequestBody Requests.SetSideSourcesRequest request) {
        menuService.setSideSources(categoryName, itemId, request.sideSources());
        return getMenu();
    }

    @PutMapping("/categories/{categoryName}/items/{itemId}/category")
    public ApiResponse<MenuDto> moveItemToCategory(@PathVariable String categoryName,
            @PathVariable UUID itemId,
            @RequestBody Requests.ItemCategoryUpdateRequest request) {
        menuService.moveMenuItemToCategory(categoryName, itemId, request.newCategory());
        return getMenu();
    }

    @PutMapping("/categories/{categoryName}/items/reorder")
    public ApiResponse<MenuDto> reorderItemsInCategory(@PathVariable String categoryName,
            @RequestBody Requests.ItemReorderRequest request) {
        menuService.reorderItemsInCategory(categoryName, request.order());
        return getMenu();
    }

    @PostMapping("/combos")
    public ApiResponse<MenuDto> createCombo(@RequestBody Requests.CreateComboRequest request) {
        logger.info("Received request to create combo: {}", request.name());
        boolean kitchen = request.kitchen() != null && request.kitchen();
        menuService.createCombo(request.name(), request.category(), request.componentIds(),
                request.slots(), request.price(), kitchen);
        return getMenu();
    }

    @GetMapping("/combos/{id}")
    public ApiResponse<ComboItemDto> getCombo(@PathVariable UUID id) {
        ComboItem combo = menuService.getCombo(id);
        return ApiResponse.success(DtoMapper.toComboItemDto(combo, menuService.getMenu()));
    }

    @PutMapping("/combos/{id}")
    public ApiResponse<MenuDto> updateCombo(@PathVariable UUID id,
            @RequestBody Requests.UpdateComboRequest request) {
        menuService.updateCombo(id, request.name(), request.price(), request.available(), request.kitchen());
        return getMenu();
    }

    @DeleteMapping("/combos/{id}")
    public ApiResponse<MenuDto> deleteCombo(@PathVariable UUID id) {
        logger.info("Received request to delete combo: {}", id);
        menuService.deleteCombo(id);
        return getMenu();
    }
}
