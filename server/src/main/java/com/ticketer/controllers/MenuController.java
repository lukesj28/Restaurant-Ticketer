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

    @GetMapping("/items")
    public ApiResponse<List<ItemDto>> getAllItems() {
        Map<String, List<MenuItem>> categories = menuService.getCategories();
        List<ItemDto> dtos = categories.values().stream()
                .flatMap(List::stream)
                .map(DtoMapper::toItemDto)
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

    @GetMapping("/categories/{categoryName}")
    public ApiResponse<List<ItemDto>> getCategory(@PathVariable String categoryName) {
        List<MenuItem> items = menuService.getCategory(categoryName);
        List<ItemDto> dtos = items.stream()
                .map(DtoMapper::toItemDto)
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @GetMapping("/categories/{categoryName}/items/{itemName}")
    public ApiResponse<ItemDto> getItem(@PathVariable String categoryName, @PathVariable String itemName) {
        MenuItem item = menuService.getItem(categoryName, itemName);
        return ApiResponse.success(DtoMapper.toItemDto(item));
    }

    @PostMapping("/items")
    public ApiResponse<ItemDto> addItem(@RequestBody Requests.ItemCreateRequest request) {
        logger.info("Received request to add item: {} to category: {}", request.name(), request.category());
        menuService.addItem(request.category(), request.name(), request.price(), request.sides());
        MenuItem item = menuService.getItem(request.category(), request.name());
        return ApiResponse.success(DtoMapper.toItemDto(item));
    }

    @PutMapping("/categories/{categoryName}/items/{itemName}/price")
    public ApiResponse<List<ItemDto>> editItemPrice(@PathVariable("categoryName") String categoryName,
            @PathVariable("itemName") String itemName,
            @RequestBody Requests.ItemPriceUpdateRequest request) {
        menuService.editItemPrice(categoryName, itemName, request.newPrice());
        return getAllItems();
    }

    @PutMapping("/categories/{categoryName}/items/{itemName}/availability")
    public ApiResponse<List<ItemDto>> editItemAvailability(@PathVariable("categoryName") String categoryName,
            @PathVariable("itemName") String itemName,
            @RequestBody Requests.ItemAvailabilityUpdateRequest request) {
        menuService.editItemAvailability(categoryName, itemName, request.available());
        return getAllItems();
    }

    @PutMapping("/categories/{categoryName}/items/{oldName}/rename")
    public ApiResponse<List<ItemDto>> renameItem(@PathVariable("categoryName") String categoryName,
            @PathVariable("oldName") String oldName,
            @RequestBody Requests.ItemRenameRequest request) {
        menuService.renameItem(categoryName, oldName, request.newName());
        return getAllItems();
    }

    @DeleteMapping("/categories/{categoryName}/items/{itemName}")
    public ApiResponse<List<ItemDto>> removeItem(@PathVariable("categoryName") String categoryName,
            @PathVariable("itemName") String itemName) {
        logger.info("Received request to remove item: {} from category: {}", itemName, categoryName);
        menuService.removeItem(categoryName, itemName);
        return getAllItems();
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

    @PutMapping("/categories/{categoryName}/items/{itemName}/category")
    public ApiResponse<List<ItemDto>> changeCategory(@PathVariable("categoryName") String categoryName,
            @PathVariable("itemName") String itemName,
            @RequestBody Requests.ItemCategoryUpdateRequest request) {
        menuService.changeCategory(categoryName, itemName, request.newCategory());
        return getAllItems();
    }

    @PutMapping("/categories/{categoryName}/items/{itemName}/sides/{sideName}")
    public ApiResponse<List<ItemDto>> updateSide(@PathVariable("categoryName") String categoryName,
            @PathVariable("itemName") String itemName,
            @PathVariable("sideName") String sideName,
            @RequestBody Requests.SideUpdateRequest request) {
        menuService.updateSide(categoryName, itemName, sideName, request.price(), request.available());
        return getAllItems();
    }

    @DeleteMapping("/categories/{categoryName}/items/{itemName}/sides/{sideName}")
    public ApiResponse<List<ItemDto>> removeSide(@PathVariable("categoryName") String categoryName,
            @PathVariable("itemName") String itemName,
            @PathVariable("sideName") String sideName) {
        logger.info("Received request to remove side {} from item {} in category {}", sideName, itemName, categoryName);
        menuService.removeSide(categoryName, itemName, sideName);
        return getAllItems();
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

    @PutMapping("/categories/{categoryName}/items/{itemName}/sides/reorder")
    public ApiResponse<List<ItemDto>> reorderSidesInItem(@PathVariable("categoryName") String categoryName,
            @PathVariable("itemName") String itemName,
            @RequestBody Requests.SideReorderRequest request) {
        menuService.reorderSidesInItem(categoryName, itemName, request.order());
        return getAllItems();
    }

    @GetMapping("/category-order")
    public ApiResponse<List<String>> getCategoryOrder() {
        return ApiResponse.success(menuService.getCategoryOrder());
    }

    @PostMapping("/categories/{categoryName}/items/{itemName}/sides")
    public ApiResponse<List<ItemDto>> addSide(@PathVariable("categoryName") String categoryName,
            @PathVariable("itemName") String itemName,
            @RequestBody Requests.SideCreateRequest request) {
        logger.info("Received request to add side {} to item {} in category {}", request.name(), itemName, categoryName);
        menuService.addSide(categoryName, itemName, request.name(), request.price());
        return getAllItems();
    }

    @PostMapping("/categories/{categoryName}/items/{itemName}/extras")
    public ApiResponse<List<ItemDto>> addExtra(@PathVariable("categoryName") String categoryName,
            @PathVariable("itemName") String itemName,
            @RequestBody Requests.ExtraCreateRequest request) {
        logger.info("Received request to add extra {} to item {} in category {}", request.name(), itemName, categoryName);
        menuService.addExtra(categoryName, itemName, request.name(), request.price());
        return getAllItems();
    }

    @PutMapping("/categories/{categoryName}/items/{itemName}/extras/{extraName}")
    public ApiResponse<List<ItemDto>> updateExtra(@PathVariable("categoryName") String categoryName,
            @PathVariable("itemName") String itemName,
            @PathVariable("extraName") String extraName,
            @RequestBody Requests.ExtraUpdateRequest request) {
        menuService.updateExtra(categoryName, itemName, extraName, request.price(), request.available());
        return getAllItems();
    }

    @DeleteMapping("/categories/{categoryName}/items/{itemName}/extras/{extraName}")
    public ApiResponse<List<ItemDto>> removeExtra(@PathVariable("categoryName") String categoryName,
            @PathVariable("itemName") String itemName,
            @PathVariable("extraName") String extraName) {
        logger.info("Received request to remove extra {} from item {} in category {}", extraName, itemName, categoryName);
        menuService.removeExtra(categoryName, itemName, extraName);
        return getAllItems();
    }

    @PutMapping("/categories/{categoryName}/items/{itemName}/extras/reorder")
    public ApiResponse<List<ItemDto>> reorderExtrasInItem(@PathVariable("categoryName") String categoryName,
            @PathVariable("itemName") String itemName,
            @RequestBody Requests.ExtraReorderRequest request) {
        menuService.reorderExtrasInItem(categoryName, itemName, request.order());
        return getAllItems();
    }
}
