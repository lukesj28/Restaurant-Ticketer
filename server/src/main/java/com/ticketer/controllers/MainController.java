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
import com.ticketer.utils.menu.dto.MenuItem;
import com.ticketer.utils.menu.dto.MenuItemView;
import com.ticketer.api.ApiResponse;
import com.ticketer.dtos.*;
import com.ticketer.exceptions.TicketerException;

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

    private OrderItem mapFromOrderItemDto(OrderItemDto dto) {
        return new OrderItem(dto.name(), dto.selectedSide(), dto.price());
    }

    private OrderDto mapToOrderDto(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(this::mapToOrderItemDto)
                .collect(Collectors.toList());
        return new OrderDto(items, order.getSubtotal(), order.getTotal(), order.getTaxRate());
    }

    private Order mapFromOrderDto(OrderDto dto) {
        Order order = new Order(dto.taxRate());
        if (dto.items() != null) {
            for (OrderItemDto itemDto : dto.items()) {
                order.addItem(mapFromOrderItemDto(itemDto));
            }
        }
        return order;
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
                ticket.getCreatedAt());
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
        try {
            menuService.refreshMenu();
            return ApiResponse.success(java.util.Collections.emptyList());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/menu/items/{name}")
    public ApiResponse<ItemDto> getItem(@PathVariable String name) {
        try {
            MenuItem item = menuService.getItem(name);
            return ApiResponse.success(mapToItemDto(item));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error retrieving item: " + e.getMessage());
        }
    }

    public static OrderItem getItem(MenuItem item, String sideName) {
        return Menu.getItem(item, sideName);
    }

    @GetMapping("/menu/categories/{categoryName}")
    public ApiResponse<List<ItemDto>> getCategory(@PathVariable String categoryName) {
        try {
            List<MenuItem> items = menuService.getCategory(categoryName);
            List<ItemDto> dtos = items.stream()
                    .map(this::mapToItemDto)
                    .collect(Collectors.toList());
            return ApiResponse.success(dtos);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error retrieving category: " + e.getMessage());
        }
    }

    @GetMapping("/menu/items")
    public ApiResponse<List<ItemDto>> getAllItems() {
        try {
            List<MenuItemView> views = menuService.getAllItems();
            List<ItemDto> dtos = views.stream()
                    .map(v -> {
                        MenuItem full = menuService.getItem(v.name);
                        return mapToItemDto(full);
                    })
                    .collect(Collectors.toList());
            return ApiResponse.success(dtos);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/menu/categories")
    public ApiResponse<Map<String, List<ItemDto>>> getCategories() {
        try {
            Map<String, List<MenuItem>> categories = menuService.getCategories();
            Map<String, List<ItemDto>> dtos = categories.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().stream()
                                    .map(this::mapToItemDto)
                                    .collect(Collectors.toList())));
            return ApiResponse.success(dtos);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/menu/items")
    public ApiResponse<ItemDto> addItem(@RequestBody Requests.ItemCreateRequest request) {
        try {
            menuService.addItem(request.category(), request.name(), request.price(), request.sides());
            MenuItem item = menuService.getItem(request.name());
            return ApiResponse.success(mapToItemDto(item));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error adding item: " + e.getMessage());
        }
    }

    @PutMapping("/menu/items/{itemName}/price")
    public ApiResponse<ItemDto> editItemPrice(@PathVariable String itemName,
            @RequestBody Requests.ItemPriceUpdateRequest request) {
        try {
            menuService.editItemPrice(itemName, request.newPrice());
            MenuItem item = menuService.getItem(itemName);
            return ApiResponse.success(mapToItemDto(item));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error updating price: " + e.getMessage());
        }
    }

    @PutMapping("/menu/items/{itemName}/availability")
    public ApiResponse<ItemDto> editItemAvailability(@PathVariable String itemName,
            @RequestBody Requests.ItemAvailabilityUpdateRequest request) {
        try {
            menuService.editItemAvailability(itemName, request.available());
            MenuItem item = menuService.getItem(itemName);
            return ApiResponse.success(mapToItemDto(item));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error updating availability: " + e.getMessage());
        }
    }

    @PutMapping("/menu/items/{oldName}/rename")
    public ApiResponse<ItemDto> renameItem(@PathVariable String oldName,
            @RequestBody Requests.ItemRenameRequest request) {
        try {
            menuService.renameItem(oldName, request.newName());
            MenuItem item = menuService.getItem(request.newName());
            return ApiResponse.success(mapToItemDto(item));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error renaming item: " + e.getMessage());
        }
    }

    @DeleteMapping("/menu/items/{itemName}")
    public ApiResponse<List<String>> removeItem(@PathVariable String itemName) {
        try {
            menuService.removeItem(itemName);
            return ApiResponse.success(java.util.Collections.emptyList());
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error removing item: " + e.getMessage());
        }
    }

    @PutMapping("/menu/categories/{oldCategory}/rename")
    public ApiResponse<List<ItemDto>> renameCategory(@PathVariable String oldCategory,
            @RequestBody Requests.CategoryRenameRequest request) {
        try {
            menuService.renameCategory(oldCategory, request.newCategory());
            List<MenuItem> items = menuService.getCategory(request.newCategory());
            List<ItemDto> dtos = items.stream()
                    .map(this::mapToItemDto)
                    .collect(Collectors.toList());
            return ApiResponse.success(dtos);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error renaming category: " + e.getMessage());
        }
    }

    @PutMapping("/menu/items/{itemName}/category")
    public ApiResponse<ItemDto> changeCategory(@PathVariable String itemName,
            @RequestBody Requests.ItemCategoryUpdateRequest request) {
        try {
            menuService.changeCategory(itemName, request.newCategory());
            MenuItem item = menuService.getItem(itemName);
            return ApiResponse.success(mapToItemDto(item));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error changing category: " + e.getMessage());
        }
    }

    @PutMapping("/menu/items/{itemName}/sides/{sideName}")
    public ApiResponse<ItemDto> updateSide(@PathVariable String itemName, @PathVariable String sideName,
            @RequestBody Requests.SideUpdateRequest request) {
        try {
            menuService.updateSide(itemName, sideName, request.newPrice());
            MenuItem item = menuService.getItem(itemName);
            return ApiResponse.success(mapToItemDto(item));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error updating side: " + e.getMessage());
        }
    }

    @PostMapping("/settings/refresh")
    public ApiResponse<Settings> refreshSettings() {
        try {
            return ApiResponse.success(settingsService.getSettings());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/settings/tax")
    public ApiResponse<Double> getTax() {
        try {
            return ApiResponse.success(settingsService.getTax());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/settings/hours")
    public ApiResponse<Map<String, String>> getOpeningHours() {
        try {
            return ApiResponse.success(settingsService.getAllOpeningHours());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/settings/hours/{day}")
    public ApiResponse<String> getOpeningHours(@PathVariable String day) {
        try {
            return ApiResponse.success(settingsService.getOpeningHours(day));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/settings/hours/{day}/open")
    public ApiResponse<String> getOpenTime(@PathVariable String day) {
        try {
            return ApiResponse.success(settingsService.getOpenTime(day));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/settings/hours/{day}/close")
    public ApiResponse<String> getCloseTime(@PathVariable("day") String day) {
        try {
            return ApiResponse.success(settingsService.getCloseTime(day));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/settings/tax")
    public ApiResponse<SettingsDto> setTax(@RequestBody Requests.TaxUpdateRequest request) {
        try {
            settingsService.setTax(request.tax());
            return ApiResponse.success(mapToSettingsDto(settingsService.getSettings()));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error setting tax: " + e.getMessage());
        }
    }

    @PutMapping("/settings/hours/{day}")
    public ApiResponse<SettingsDto> setOpeningHours(@PathVariable("day") String day,
            @RequestBody Requests.OpeningHoursUpdateRequest request) {
        try {
            settingsService.setOpeningHours(day, request.hours());
            restaurantStateService.checkAndScheduleState();
            return ApiResponse.success(mapToSettingsDto(settingsService.getSettings()));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error setting opening hours: " + e.getMessage());
        }
    }

    @PostMapping("/tickets")
    public ApiResponse<TicketDto> createTicket(@RequestParam("tableNumber") String tableNumber) {
        try {
            Ticket ticket = ticketService.createTicket(tableNumber);
            return ApiResponse.success(mapToTicketDto(ticket));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/tickets/{ticketId}")
    public ApiResponse<TicketDto> getTicket(@PathVariable("ticketId") int ticketId) {
        try {
            Ticket ticket = ticketService.getTicket(ticketId);
            if (ticket == null)
                return ApiResponse.error("Ticket not found");
            return ApiResponse.success(mapToTicketDto(ticket));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/tickets/{ticketId}/orders")
    public ApiResponse<TicketDto> addOrderToTicket(@PathVariable("ticketId") int ticketId) {
        try {
            ticketService.addOrderToTicket(ticketId, new Order(settingsService.getTax()));
            return getTicket(ticketId);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error adding order: " + e.getMessage());
        }
    }

    @DeleteMapping("/tickets/{ticketId}/orders/{orderIndex}")
    public ApiResponse<TicketDto> removeOrderFromTicket(@PathVariable("ticketId") int ticketId,
            @PathVariable("orderIndex") int orderIndex) {
        try {
            ticketService.removeOrder(ticketId, orderIndex);
            return getTicket(ticketId);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error removing order: " + e.getMessage());
        }
    }

    @PostMapping("/tickets/{ticketId}/orders/{orderIndex}/items")
    public ApiResponse<TicketDto> addItemToOrder(@PathVariable("ticketId") int ticketId,
            @PathVariable("orderIndex") int orderIndex,
            @RequestBody Requests.AddItemRequest request) {
        try {
            MenuItem menuItem = menuService.getItem(request.name());
            if (menuItem == null) {
                return ApiResponse.error("Item not found: " + request.name());
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
        } catch (Exception e) {
            return ApiResponse.error("Error adding item: " + e.getMessage());
        }
    }

    @DeleteMapping("/tickets/{ticketId}/orders/{orderIndex}/items")
    public ApiResponse<TicketDto> removeItemFromOrder(@PathVariable("ticketId") int ticketId,
            @PathVariable("orderIndex") int orderIndex,
            @RequestBody Requests.AddItemRequest request) {
        try {
            OrderItem item = new OrderItem(request.name(), request.selectedSide(), 0);
            ticketService.removeItemFromOrder(ticketId, orderIndex, item);
            return getTicket(ticketId);
        } catch (Exception e) {
            return ApiResponse.error("Error removing item: " + e.getMessage());
        }
    }

    @GetMapping("/tickets/active")
    public ApiResponse<List<TicketDto>> getActiveTickets() {
        try {
            return ApiResponse.success(
                    ticketService.getActiveTickets().stream().map(this::mapToTicketDto)
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/tickets/completed")
    public ApiResponse<List<TicketDto>> getCompletedTickets() {
        try {
            return ApiResponse.success(ticketService.getCompletedTickets().stream().map(this::mapToTicketDto)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/tickets/closed")
    public ApiResponse<List<TicketDto>> getClosedTickets() {
        try {
            return ApiResponse.success(
                    ticketService.getClosedTickets().stream().map(this::mapToTicketDto)
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/tickets/{ticketId}/completed")
    public ApiResponse<TicketDto> moveToCompleted(@PathVariable("ticketId") int ticketId) {
        try {
            ticketService.moveToCompleted(ticketId);
            Ticket t = ticketService.getCompletedTickets().stream().filter(x -> x.getId() == ticketId).findFirst()
                    .orElse(null);
            return ApiResponse.success(mapToTicketDto(t));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/tickets/{ticketId}/closed")
    public ApiResponse<TicketDto> moveToClosed(@PathVariable("ticketId") int ticketId) {
        try {
            ticketService.moveToClosed(ticketId);
            Ticket t = ticketService.getClosedTickets().stream().filter(x -> x.getId() == ticketId).findFirst()
                    .orElse(null);
            return ApiResponse.success(mapToTicketDto(t));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/tickets/{ticketId}/active")
    public ApiResponse<TicketDto> moveToActive(@PathVariable("ticketId") int ticketId) {
        try {
            ticketService.moveToActive(ticketId);
            return getTicket(ticketId);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/tickets/{ticketId}")
    public ApiResponse<List<String>> removeTicket(@PathVariable("ticketId") int ticketId) {
        try {
            ticketService.removeTicket(ticketId);
            return ApiResponse.success(java.util.Collections.emptyList());
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/shutdown")
    public void shutdown() {
        restaurantStateService.shutdown();
    }
}
