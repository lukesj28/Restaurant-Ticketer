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

    private final MenuService menuService;

    @Autowired
    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping("/refresh")
    public ApiResponse<List<String>> refreshMenu() {
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
        menuService.updateSide(itemName, sideName, request.newPrice());
        MenuItem item = menuService.getItem(itemName);
        return ApiResponse.success(DtoMapper.toItemDto(item));
    }
}
