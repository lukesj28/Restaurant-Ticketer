package com.ticketer.controllers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.ticketer.api.ApiResponse;
import com.ticketer.dtos.*;
import com.ticketer.models.MenuItem;
import com.ticketer.models.MenuItemView;
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

    @PostMapping("/refresh")
    public ApiResponse<List<String>> refreshMenu() {
        logger.info("Received request to refresh menu");
        menuService.refreshMenu();
        return ApiResponse.success(java.util.Collections.emptyList());
    }

    @GetMapping("/items/{name}")
    public ApiResponse<ItemDto> getItem(@PathVariable String name) {
        MenuItem item = menuService.getItem(name);
        return ApiResponse.success(DtoMapper.toItemDto(item));
    }

    @GetMapping("/categories/{categoryName}")
    public ApiResponse<List<ItemDto>> getCategory(@PathVariable String categoryName) {
        List<MenuItem> items = menuService.getCategory(categoryName);
        List<ItemDto> dtos = items.stream()
                .map(DtoMapper::toItemDto)
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @GetMapping("/items")
    public ApiResponse<List<ItemDto>> getAllItems() {
        List<MenuItemView> views = menuService.getAllItems();
        List<ItemDto> dtos = views.stream()
                .map(v -> {
                    MenuItem full = menuService.getItem(v.name);
                    return DtoMapper.toItemDto(full);
                })
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @GetMapping("/categories")
    public ApiResponse<Map<String, List<ItemDto>>> getCategories() {
        Map<String, List<MenuItem>> categories = menuService.getCategories();
        Map<String, List<ItemDto>> dtos = categories.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(DtoMapper::toItemDto)
                                .collect(Collectors.toList())));
        return ApiResponse.success(dtos);
    }

    @PostMapping("/items")
    public ApiResponse<ItemDto> addItem(@RequestBody Requests.ItemCreateRequest request) {
        logger.info("Received request to add item: {} to category: {}", request.name(), request.category());
        menuService.addItem(request.category(), request.name(), request.price(), request.sides());
        MenuItem item = menuService.getItem(request.name());
        return ApiResponse.success(DtoMapper.toItemDto(item));
    }

    @PutMapping("/items/{itemName}/price")
    public ApiResponse<ItemDto> editItemPrice(@PathVariable String itemName,
            @RequestBody Requests.ItemPriceUpdateRequest request) {
        menuService.editItemPrice(itemName, request.newPrice());
        MenuItem item = menuService.getItem(itemName);
        return ApiResponse.success(DtoMapper.toItemDto(item));
    }

    @PutMapping("/items/{itemName}/availability")
    public ApiResponse<ItemDto> editItemAvailability(@PathVariable String itemName,
            @RequestBody Requests.ItemAvailabilityUpdateRequest request) {
        menuService.editItemAvailability(itemName, request.available());
        MenuItem item = menuService.getItem(itemName);
        return ApiResponse.success(DtoMapper.toItemDto(item));
    }

    @PutMapping("/items/{oldName}/rename")
    public ApiResponse<ItemDto> renameItem(@PathVariable String oldName,
            @RequestBody Requests.ItemRenameRequest request) {
        menuService.renameItem(oldName, request.newName());
        MenuItem item = menuService.getItem(request.newName());
        return ApiResponse.success(DtoMapper.toItemDto(item));
    }

    @DeleteMapping("/items/{itemName}")
    public ApiResponse<List<String>> removeItem(@PathVariable String itemName) {
        logger.info("Received request to remove item: {}", itemName);
        menuService.removeItem(itemName);
        return ApiResponse.success(java.util.Collections.emptyList());
    }

    @PutMapping("/categories/{oldCategory}/rename")
    public ApiResponse<List<ItemDto>> renameCategory(@PathVariable String oldCategory,
            @RequestBody Requests.CategoryRenameRequest request) {
        menuService.renameCategory(oldCategory, request.newCategory());
        List<MenuItem> items = menuService.getCategory(request.newCategory());
        List<ItemDto> dtos = items.stream()
                .map(DtoMapper::toItemDto)
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @PutMapping("/items/{itemName}/category")
    public ApiResponse<ItemDto> changeCategory(@PathVariable String itemName,
            @RequestBody Requests.ItemCategoryUpdateRequest request) {
        menuService.changeCategory(itemName, request.newCategory());
        MenuItem item = menuService.getItem(itemName);
        return ApiResponse.success(DtoMapper.toItemDto(item));
    }

    @PutMapping("/items/{itemName}/sides/{sideName}")
    public ApiResponse<ItemDto> updateSide(@PathVariable String itemName, @PathVariable String sideName,
            @RequestBody Requests.SideUpdateRequest request) {
        menuService.updateSide(itemName, sideName, request.price(), request.available());
        MenuItem item = menuService.getItem(itemName);
        return ApiResponse.success(DtoMapper.toItemDto(item));
    }

    @PostMapping("/items/{itemName}/sides")
    public ApiResponse<ItemDto> addSide(@PathVariable String itemName,
            @RequestBody Requests.SideCreateRequest request) {
        logger.info("Received request to add side {} to item {}", request.name(), itemName);
        menuService.addSide(itemName, request.name(), request.price());
        MenuItem item = menuService.getItem(itemName);
        return ApiResponse.success(DtoMapper.toItemDto(item));
    }

    @DeleteMapping("/items/{itemName}/sides/{sideName}")
    public ApiResponse<ItemDto> removeSide(@PathVariable String itemName, @PathVariable String sideName) {
        logger.info("Received request to remove side {} from item {}", sideName, itemName);
        menuService.removeSide(itemName, sideName);
        MenuItem item = menuService.getItem(itemName);
        return ApiResponse.success(DtoMapper.toItemDto(item));
    }

    @DeleteMapping("/categories/{categoryName}")
    public ApiResponse<List<String>> deleteCategory(@PathVariable String categoryName) {
        menuService.deleteCategory(categoryName);
        return ApiResponse.success(java.util.Collections.emptyList());
    }

    @GetMapping("/kitchen-items")
    public ApiResponse<List<String>> getKitchenItems() {
        return ApiResponse.success(menuService.getKitchenItems());
    }

    @PostMapping("/kitchen-items/{itemName}")
    public ApiResponse<List<String>> addKitchenItem(@PathVariable String itemName) {
        menuService.addKitchenItem(itemName);
        return ApiResponse.success(menuService.getKitchenItems());
    }

    @DeleteMapping("/kitchen-items/{itemName}")
    public ApiResponse<List<String>> removeKitchenItem(@PathVariable String itemName) {
        menuService.removeKitchenItem(itemName);
        return ApiResponse.success(menuService.getKitchenItems());
    }

    @PutMapping("/categories/reorder")
    public ApiResponse<List<String>> reorderCategories(@RequestBody Requests.CategoryReorderRequest request) {
        menuService.reorderCategories(request.order());
        return ApiResponse.success(menuService.getCategoryOrder());
    }

    @PutMapping("/categories/{categoryName}/items/reorder")
    public ApiResponse<List<ItemDto>> reorderItemsInCategory(@PathVariable String categoryName,
            @RequestBody Requests.ItemReorderRequest request) {
        menuService.reorderItemsInCategory(categoryName, request.order());
        List<MenuItem> items = menuService.getCategory(categoryName);
        List<ItemDto> dtos = items.stream()
                .map(DtoMapper::toItemDto)
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @PutMapping("/items/{itemName}/sides/reorder")
    public ApiResponse<ItemDto> reorderSidesInItem(@PathVariable String itemName,
            @RequestBody Requests.SideReorderRequest request) {
        menuService.reorderSidesInItem(itemName, request.order());
        MenuItem item = menuService.getItem(itemName);
        return ApiResponse.success(DtoMapper.toItemDto(item));
    }

    @GetMapping("/category-order")
    public ApiResponse<List<String>> getCategoryOrder() {
        return ApiResponse.success(menuService.getCategoryOrder());
    }
}
