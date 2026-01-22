package com.ticketer.controllers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.ticketer.models.OrderItem;
import com.ticketer.models.Menu;
import com.ticketer.models.Settings;
import com.ticketer.models.Ticket;
import com.ticketer.models.Order;
import com.ticketer.models.MenuItem;
import com.ticketer.models.MenuItemView;

import com.ticketer.api.ApiResponse;
import com.ticketer.dtos.*;

import com.ticketer.dtos.Requests;
import com.ticketer.services.MenuService;
import com.ticketer.services.SettingsService;
import com.ticketer.services.TicketService;
import com.ticketer.services.RestaurantStateService;

@RestController
@RequestMapping("/api")
public class MainController {

    private final MenuService menuService;
    private final SettingsService settingsService;
    private final TicketService ticketService;
    private final RestaurantStateService restaurantStateService;

    @Autowired
    public MainController(MenuService menuService,
            SettingsService settingsService,
            TicketService ticketService,
            RestaurantStateService restaurantStateService) {
        this.menuService = menuService;
        this.settingsService = settingsService;
        this.ticketService = ticketService;
        this.restaurantStateService = restaurantStateService;
    }

    @GetMapping("/status")
    public boolean isOpen() {
        return restaurantStateService.isOpen();
    }

    private SettingsDto mapToSettingsDto(com.ticketer.models.Settings settings) {
        if (settings == null)
            return null;
        return new SettingsDto(settings.getTax(), settings.getHours());
    }

    private OrderItemDto mapToOrderItemDto(OrderItem item) {
        return new OrderItemDto(item.getName(), item.getSelectedSide(), item.getPrice());
    }

    private OrderDto mapToOrderDto(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(this::mapToOrderItemDto)
                .collect(Collectors.toList());
        return new OrderDto(items, order.getSubtotal(), order.getTotal(), order.getTaxRate());
    }

    private TicketDto mapToTicketDto(Ticket ticket) {
        if (ticket == null)
            return null;
        List<OrderDto> orders = ticket.getOrders().stream()
                .map(this::mapToOrderDto)
                .collect(Collectors.toList());
        return new TicketDto(
                ticket.getId(),
                ticket.getTableNumber(),
                orders,
                ticket.getSubtotal(),
                ticket.getTotal(),
                ticket.getCreatedAt().toString(),
                ticket.getClosedAt() != null ? ticket.getClosedAt().toString() : null);
    }

    private ItemDto mapToItemDto(MenuItem item) {
        if (item == null)
            return null;
        Map<String, SideDto> sides = null;
        if (item.sideOptions != null) {
            sides = item.sideOptions.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> new SideDto(e.getValue().price, e.getValue().available)));
        }
        return new ItemDto(
                item.name,
                item.basePrice,
                item.available,
                sides);
    }

    @PostMapping("/menu/refresh")
    public ApiResponse<List<String>> refreshMenu() {
        menuService.refreshMenu();
        return ApiResponse.success(java.util.Collections.emptyList());
    }

    @GetMapping("/menu/items/{name}")
    public ApiResponse<ItemDto> getItem(@PathVariable String name) {
        MenuItem item = menuService.getItem(name);
        return ApiResponse.success(mapToItemDto(item));
    }

    public static OrderItem getItem(MenuItem item, String sideName) {
        return Menu.getItem(item, sideName);
    }

    @GetMapping("/menu/categories/{categoryName}")
    public ApiResponse<List<ItemDto>> getCategory(@PathVariable String categoryName) {
        List<MenuItem> items = menuService.getCategory(categoryName);
        List<ItemDto> dtos = items.stream()
                .map(this::mapToItemDto)
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @GetMapping("/menu/items")
    public ApiResponse<List<ItemDto>> getAllItems() {
        List<MenuItemView> views = menuService.getAllItems();
        List<ItemDto> dtos = views.stream()
                .map(v -> {
                    MenuItem full = menuService.getItem(v.name);
                    return mapToItemDto(full);
                })
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @GetMapping("/menu/categories")
    public ApiResponse<Map<String, List<ItemDto>>> getCategories() {
        Map<String, List<MenuItem>> categories = menuService.getCategories();
        Map<String, List<ItemDto>> dtos = categories.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(this::mapToItemDto)
                                .collect(Collectors.toList())));
        return ApiResponse.success(dtos);
    }

    @PostMapping("/menu/items")
    public ApiResponse<ItemDto> addItem(@RequestBody Requests.ItemCreateRequest request) {
        menuService.addItem(request.category(), request.name(), request.price(), request.sides());
        MenuItem item = menuService.getItem(request.name());
        return ApiResponse.success(mapToItemDto(item));
    }

    @PutMapping("/menu/items/{itemName}/price")
    public ApiResponse<ItemDto> editItemPrice(@PathVariable String itemName,
            @RequestBody Requests.ItemPriceUpdateRequest request) {
        menuService.editItemPrice(itemName, request.newPrice());
        MenuItem item = menuService.getItem(itemName);
        return ApiResponse.success(mapToItemDto(item));
    }

    @PutMapping("/menu/items/{itemName}/availability")
    public ApiResponse<ItemDto> editItemAvailability(@PathVariable String itemName,
            @RequestBody Requests.ItemAvailabilityUpdateRequest request) {
        menuService.editItemAvailability(itemName, request.available());
        MenuItem item = menuService.getItem(itemName);
        return ApiResponse.success(mapToItemDto(item));
    }

    @PutMapping("/menu/items/{oldName}/rename")
    public ApiResponse<ItemDto> renameItem(@PathVariable String oldName,
            @RequestBody Requests.ItemRenameRequest request) {
        menuService.renameItem(oldName, request.newName());
        MenuItem item = menuService.getItem(request.newName());
        return ApiResponse.success(mapToItemDto(item));
    }

    @DeleteMapping("/menu/items/{itemName}")
    public ApiResponse<List<String>> removeItem(@PathVariable String itemName) {
        menuService.removeItem(itemName);
        return ApiResponse.success(java.util.Collections.emptyList());
    }

    @PutMapping("/menu/categories/{oldCategory}/rename")
    public ApiResponse<List<ItemDto>> renameCategory(@PathVariable String oldCategory,
            @RequestBody Requests.CategoryRenameRequest request) {
        menuService.renameCategory(oldCategory, request.newCategory());
        List<MenuItem> items = menuService.getCategory(request.newCategory());
        List<ItemDto> dtos = items.stream()
                .map(this::mapToItemDto)
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @PutMapping("/menu/items/{itemName}/category")
    public ApiResponse<ItemDto> changeCategory(@PathVariable String itemName,
            @RequestBody Requests.ItemCategoryUpdateRequest request) {
        menuService.changeCategory(itemName, request.newCategory());
        MenuItem item = menuService.getItem(itemName);
        return ApiResponse.success(mapToItemDto(item));
    }

    @PutMapping("/menu/items/{itemName}/sides/{sideName}")
    public ApiResponse<ItemDto> updateSide(@PathVariable String itemName, @PathVariable String sideName,
            @RequestBody Requests.SideUpdateRequest request) {
        menuService.updateSide(itemName, sideName, request.newPrice());
        MenuItem item = menuService.getItem(itemName);
        return ApiResponse.success(mapToItemDto(item));
    }

    @PostMapping("/settings/refresh")
    public ApiResponse<Settings> refreshSettings() {
        return ApiResponse.success(settingsService.getSettings());
    }

    @GetMapping("/settings/tax")
    public ApiResponse<Double> getTax() {
        return ApiResponse.success(settingsService.getTax());
    }

    @GetMapping("/settings/hours")
    public ApiResponse<Map<String, String>> getOpeningHours() {
        return ApiResponse.success(settingsService.getAllOpeningHours());
    }

    @GetMapping("/settings/hours/{day}")
    public ApiResponse<String> getOpeningHours(@PathVariable String day) {
        return ApiResponse.success(settingsService.getOpeningHours(day));
    }

    @GetMapping("/settings/hours/{day}/open")
    public ApiResponse<String> getOpenTime(@PathVariable String day) {
        return ApiResponse.success(settingsService.getOpenTime(day));
    }

    @GetMapping("/settings/hours/{day}/close")
    public ApiResponse<String> getCloseTime(@PathVariable("day") String day) {
        return ApiResponse.success(settingsService.getCloseTime(day));
    }

    @PutMapping("/settings/tax")
    public ApiResponse<SettingsDto> setTax(@RequestBody Requests.TaxUpdateRequest request) {
        settingsService.setTax(request.tax());
        return ApiResponse.success(mapToSettingsDto(settingsService.getSettings()));
    }

    @PutMapping("/settings/hours/{day}")
    public ApiResponse<SettingsDto> setOpeningHours(@PathVariable("day") String day,
            @RequestBody Requests.OpeningHoursUpdateRequest request) {
        settingsService.setOpeningHours(day, request.hours());
        restaurantStateService.checkAndScheduleState();
        return ApiResponse.success(mapToSettingsDto(settingsService.getSettings()));
    }

    @PostMapping("/tickets")
    public ApiResponse<TicketDto> createTicket(@RequestParam("tableNumber") String tableNumber) {
        Ticket ticket = ticketService.createTicket(tableNumber);
        return ApiResponse.success(mapToTicketDto(ticket));
    }

    @GetMapping("/tickets/{ticketId}")
    public ApiResponse<TicketDto> getTicket(@PathVariable("ticketId") int ticketId) {
        Ticket ticket = ticketService.getTicket(ticketId);
        if (ticket == null)
            throw new com.ticketer.exceptions.EntityNotFoundException("Ticket not found");
        return ApiResponse.success(mapToTicketDto(ticket));
    }

    @PostMapping("/tickets/{ticketId}/orders")
    public ApiResponse<TicketDto> addOrderToTicket(@PathVariable("ticketId") int ticketId) {
        ticketService.addOrderToTicket(ticketId, new Order(settingsService.getTax()));
        return getTicket(ticketId);
    }

    @DeleteMapping("/tickets/{ticketId}/orders/{orderIndex}")
    public ApiResponse<TicketDto> removeOrderFromTicket(@PathVariable("ticketId") int ticketId,
            @PathVariable("orderIndex") int orderIndex) {
        ticketService.removeOrder(ticketId, orderIndex);
        return getTicket(ticketId);
    }

    @PostMapping("/tickets/{ticketId}/orders/{orderIndex}/items")
    public ApiResponse<TicketDto> addItemToOrder(@PathVariable("ticketId") int ticketId,
            @PathVariable("orderIndex") int orderIndex,
            @RequestBody Requests.AddItemRequest request) {
        MenuItem menuItem = menuService.getItem(request.name());
        if (menuItem == null) {
            throw new com.ticketer.exceptions.EntityNotFoundException("Item not found: " + request.name());
        }
        int price = menuItem.basePrice;
        if (request.selectedSide() != null && menuItem.sideOptions != null) {
            var side = menuItem.sideOptions.get(request.selectedSide());
            if (side != null) {
                price += side.price;
            }
        }
        OrderItem item = new OrderItem(request.name(), request.selectedSide(), price);
        ticketService.addItemToOrder(ticketId, orderIndex, item);
        return getTicket(ticketId);
    }

    @DeleteMapping("/tickets/{ticketId}/orders/{orderIndex}/items")
    public ApiResponse<TicketDto> removeItemFromOrder(@PathVariable("ticketId") int ticketId,
            @PathVariable("orderIndex") int orderIndex,
            @RequestBody Requests.AddItemRequest request) {
        OrderItem item = new OrderItem(request.name(), request.selectedSide(), 0);
        ticketService.removeItemFromOrder(ticketId, orderIndex, item);
        return getTicket(ticketId);
    }

    @GetMapping("/tickets/active")
    public ApiResponse<List<TicketDto>> getActiveTickets() {
        return ApiResponse.success(
                ticketService.getActiveTickets().stream().map(this::mapToTicketDto)
                        .collect(Collectors.toList()));
    }

    @GetMapping("/tickets/completed")
    public ApiResponse<List<TicketDto>> getCompletedTickets() {
        return ApiResponse.success(ticketService.getCompletedTickets().stream().map(this::mapToTicketDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/tickets/closed")
    public ApiResponse<List<TicketDto>> getClosedTickets() {
        return ApiResponse.success(
                ticketService.getClosedTickets().stream().map(this::mapToTicketDto)
                        .collect(Collectors.toList()));
    }

    @PutMapping("/tickets/{ticketId}/completed")
    public ApiResponse<TicketDto> moveToCompleted(@PathVariable("ticketId") int ticketId) {
        ticketService.moveToCompleted(ticketId);
        Ticket t = ticketService.getCompletedTickets().stream().filter(x -> x.getId() == ticketId).findFirst()
                .orElse(null);
        return ApiResponse.success(mapToTicketDto(t));
    }

    @PutMapping("/tickets/{ticketId}/closed")
    public ApiResponse<TicketDto> moveToClosed(@PathVariable("ticketId") int ticketId) {
        ticketService.moveToClosed(ticketId);
        Ticket t = ticketService.getClosedTickets().stream().filter(x -> x.getId() == ticketId).findFirst()
                .orElse(null);
        return ApiResponse.success(mapToTicketDto(t));
    }

    @PutMapping("/tickets/{ticketId}/active")
    public ApiResponse<TicketDto> moveToActive(@PathVariable("ticketId") int ticketId) {
        ticketService.moveToActive(ticketId);
        return getTicket(ticketId);
    }

    @DeleteMapping("/tickets/{ticketId}")
    public ApiResponse<List<String>> removeTicket(@PathVariable("ticketId") int ticketId) {
        ticketService.removeTicket(ticketId);
        return ApiResponse.success(java.util.Collections.emptyList());
    }

    @PostMapping("/shutdown")
    public void shutdown() {
        restaurantStateService.shutdown();
    }
}
