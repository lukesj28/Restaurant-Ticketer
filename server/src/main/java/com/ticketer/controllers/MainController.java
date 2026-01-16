package com.ticketer.controllers;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.ticketer.services.MenuService;
import com.ticketer.services.SettingsService;
import com.ticketer.services.TicketService;
import com.ticketer.repositories.FileMenuRepository;
import com.ticketer.repositories.FileSettingsRepository;
import com.ticketer.repositories.FileTicketRepository;

public class MainController {

    private final MenuService menuService;
    private final SettingsService settingsService;
    private final TicketService ticketService;

    private final ScheduledExecutorService scheduler;
    private boolean isOpen;

    public MainController() {
        this(new MenuService(new FileMenuRepository()),
                new SettingsService(new FileSettingsRepository()),
                new TicketService(new FileTicketRepository()));
    }

    public MainController(MenuService menuService, SettingsService settingsService, TicketService ticketService) {
        this.menuService = menuService;
        this.settingsService = settingsService;
        this.ticketService = ticketService;

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.isOpen = false;

        checkAndScheduleState();
    }

    private void checkAndScheduleState() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        String dayName = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toLowerCase();

        String openTimeStr = settingsService.getOpenTime(dayName);
        String closeTimeStr = settingsService.getCloseTime(dayName);

        if (openTimeStr == null || closeTimeStr == null) {
            setClosedState();
            scheduleNextDayCheck();
            return;
        }

        try {
            LocalTime openTime = LocalTime.parse(openTimeStr);
            LocalTime closeTime = LocalTime.parse(closeTimeStr);
            LocalTime now = LocalTime.now();

            if (now.isBefore(openTime)) {
                setClosedState();
                long delay = java.time.Duration.between(now, openTime).toMillis();
                scheduler.schedule(this::handleOpening, delay, TimeUnit.MILLISECONDS);
            } else if (now.isAfter(openTime) && now.isBefore(closeTime)) {
                setOpenState();
                long delay = java.time.Duration.between(now, closeTime).toMillis();
                scheduler.schedule(this::handleClosing, delay, TimeUnit.MILLISECONDS);
            } else {
                if (isOpen) {
                    handleClosing();
                } else {
                    setClosedState();
                    scheduleNextDayCheck();
                    runClosingSequence();
                }
            }

        } catch (DateTimeParseException e) {
            System.err.println("Error parsing time settings: " + e.getMessage());
            setClosedState();
            scheduleNextDayCheck();
        }
    }

    private void scheduleNextDayCheck() {
        LocalTime now = LocalTime.now();
        LocalTime midnight = LocalTime.MAX;
        long delay = java.time.Duration.between(now, midnight).toMillis() + 1000;
        scheduler.schedule(this::checkAndScheduleState, delay, TimeUnit.MILLISECONDS);
    }

    private void setOpenState() {
        this.isOpen = true;
        System.out.println("Restaurant is now OPEN.");
    }

    private void setClosedState() {
        this.isOpen = false;
        System.out.println("Restaurant is now CLOSED.");
    }

    private void handleOpening() {
        setOpenState();
        checkAndScheduleState();
    }

    private void handleClosing() {
        setClosedState();
        scheduler.execute(this::runClosingSequence);
        scheduleNextDayCheck();
    }

    private void runClosingSequence() {
        System.out.println("Starting closing sequence...");

        long startTime = System.currentTimeMillis();
        long maxWaitTime = 3600000;
        long checkInterval = 300000;

        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            if (ticketService.areAllTicketsClosed()) {
                break;
            }
            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        System.out.println("Finalizing closing sequence. Moving remaining tickets to closed.");
        ticketService.moveAllToClosed();
        ticketService.serializeClosedTickets();
        ticketService.clearAllTickets();
        System.out.println("Closing sequence completed.");
    }

    public boolean isOpen() {
        return isOpen;
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

    private ItemDto mapToItemDto(MenuItem item, String category) {
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
                category,
                item.basePrice,
                item.available,
                sides);
    }

    public ApiResponse<List<String>> refreshMenu() {
        try {
            menuService.refreshMenu();
            return ApiResponse.success(java.util.Collections.emptyList());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<ItemDto> getItem(String name) {
        try {
            MenuItem item = menuService.getItem(name);
            String category = menuService.getCategoryOfItem(name);
            return ApiResponse.success(mapToItemDto(item, category));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error retrieving item: " + e.getMessage());
        }
    }

    public static OrderItem getItem(MenuItem item, String sideName) {
        return Menu.getItem(item, sideName);
    }

    public ApiResponse<List<ItemDto>> getCategory(String categoryName) {
        try {
            List<MenuItem> items = menuService.getCategory(categoryName);
            List<ItemDto> dtos = items.stream()
                    .map(i -> mapToItemDto(i, categoryName))
                    .collect(Collectors.toList());
            return ApiResponse.success(dtos);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error retrieving category: " + e.getMessage());
        }
    }

    public ApiResponse<List<ItemDto>> getAllItems() {
        try {
            List<MenuItemView> views = menuService.getAllItems();
            List<ItemDto> dtos = views.stream()
                    .map(v -> {
                        MenuItem full = menuService.getItem(v.name);
                        return mapToItemDto(full, v.category);
                    })
                    .collect(Collectors.toList());
            return ApiResponse.success(dtos);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<Map<String, List<ItemDto>>> getCategories() {
        try {
            Map<String, List<MenuItem>> categories = menuService.getCategories();
            Map<String, List<ItemDto>> dtos = categories.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().stream()
                                    .map(i -> mapToItemDto(i, entry.getKey()))
                                    .collect(Collectors.toList())));
            return ApiResponse.success(dtos);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<ItemDto> addItem(String category, String name, int price, Map<String, Integer> sides) {
        try {
            menuService.addItem(category, name, price, sides);
            return getItem(name);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error adding item: " + e.getMessage());
        }
    }

    public ApiResponse<ItemDto> editItemPrice(String itemName, int newPrice) {
        try {
            menuService.editItemPrice(itemName, newPrice);
            return getItem(itemName);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error editing item price: " + e.getMessage());
        }
    }

    public ApiResponse<ItemDto> editItemAvailability(String itemName, boolean available) {
        try {
            menuService.editItemAvailability(itemName, available);
            return getItem(itemName);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error editing item availability: " + e.getMessage());
        }
    }

    public ApiResponse<ItemDto> renameItem(String oldName, String newName) {
        try {
            menuService.renameItem(oldName, newName);
            return getItem(newName);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error renaming item: " + e.getMessage());
        }
    }

    public ApiResponse<List<String>> removeItem(String itemName) {
        try {
            menuService.removeItem(itemName);
            return ApiResponse.success(java.util.Collections.emptyList());
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error removing item: " + e.getMessage());
        }
    }

    public ApiResponse<List<ItemDto>> renameCategory(String oldCategory, String newCategory) {
        try {
            menuService.renameCategory(oldCategory, newCategory);
            return getCategory(newCategory);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error renaming category: " + e.getMessage());
        }
    }

    public ApiResponse<ItemDto> changeCategory(String itemName, String newCategory) {
        try {
            menuService.changeCategory(itemName, newCategory);
            return getItem(itemName);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error changing category: " + e.getMessage());
        }
    }

    public ApiResponse<ItemDto> updateSide(String itemName, String sideName, int newPrice) {
        try {
            menuService.updateSide(itemName, sideName, newPrice);
            return getItem(itemName);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error updating side: " + e.getMessage());
        }
    }

    public ApiResponse<Settings> refreshSettings() {
        try {
            return ApiResponse.success(settingsService.getSettings());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<Double> getTax() {
        try {
            return ApiResponse.success(settingsService.getTax());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<Map<String, String>> getOpeningHours() {
        try {
            return ApiResponse.success(settingsService.getAllOpeningHours());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<String> getOpeningHours(String day) {
        try {
            return ApiResponse.success(settingsService.getOpeningHours(day));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<String> getOpenTime(String day) {
        try {
            return ApiResponse.success(settingsService.getOpenTime(day));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<String> getCloseTime(String day) {
        try {
            return ApiResponse.success(settingsService.getCloseTime(day));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<SettingsDto> setTax(double tax) {
        try {
            settingsService.setTax(tax);
            return ApiResponse.success(mapToSettingsDto(settingsService.getSettings()));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error setting tax: " + e.getMessage());
        }
    }

    public ApiResponse<SettingsDto> setOpeningHours(String day, String hours) {
        return ApiResponse.error("Operation not supported in this version (Refactoring in progress)");
    }

    public ApiResponse<TicketDto> createTicket(String tableNumber) {
        try {
            Ticket ticket = ticketService.createTicket(tableNumber);
            return ApiResponse.success(mapToTicketDto(ticket));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<TicketDto> getTicket(int ticketId) {
        try {
            Ticket ticket = ticketService.getTicket(ticketId);
            if (ticket == null)
                return ApiResponse.error("Ticket not found");
            return ApiResponse.success(mapToTicketDto(ticket));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<TicketDto> addOrderToTicket(int ticketId,
            OrderDto orderDto) {
        try {
            Order order = mapFromOrderDto(orderDto);
            ticketService.addOrderToTicket(ticketId, order);
            return getTicket(ticketId);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error adding order: " + e.getMessage());
        }
    }

    public ApiResponse<TicketDto> removeOrderFromTicket(int ticketId,
            OrderDto orderDto) {
        try {
            Order target = mapFromOrderDto(orderDto);
            ticketService.removeMatchingOrder(ticketId, target);
            return getTicket(ticketId);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error removing order: " + e.getMessage());
        }
    }

    public ApiResponse<OrderDto> createOrder(double taxRate) {
        try {
            Order order = new Order(taxRate);
            return ApiResponse.success(mapToOrderDto(order));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<OrderDto> addItemToOrder(OrderDto orderDto,
            OrderItemDto itemDto) {
        try {
            Order order = mapFromOrderDto(orderDto);
            OrderItem item = mapFromOrderItemDto(itemDto);
            order.addItem(item);
            return ApiResponse.success(mapToOrderDto(order));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<OrderDto> removeItemFromOrder(OrderDto orderDto,
            OrderItemDto itemDto) {
        try {
            Order order = mapFromOrderDto(orderDto);
            OrderItem target = mapFromOrderItemDto(itemDto);

            OrderItem toRemove = null;
            for (OrderItem i : order.getItems()) {
                if (i.getName().equals(target.getName()) &&
                        ((i.getSelectedSide() == null && target.getSelectedSide() == null)
                                || (i.getSelectedSide() != null
                                        && i.getSelectedSide().equals(target.getSelectedSide())))) {
                    toRemove = i;
                    break;
                }
            }

            if (toRemove != null) {
                order.removeItem(toRemove);
                return ApiResponse.success(mapToOrderDto(order));
            } else {
                return ApiResponse.error("Item not found in order");
            }
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<List<TicketDto>> getActiveTickets() {
        try {
            return ApiResponse.success(
                    ticketService.getActiveTickets().stream().map(this::mapToTicketDto)
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<List<TicketDto>> getCompletedTickets() {
        try {
            return ApiResponse.success(ticketService.getCompletedTickets().stream().map(this::mapToTicketDto)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<List<TicketDto>> getClosedTickets() {
        try {
            return ApiResponse.success(
                    ticketService.getClosedTickets().stream().map(this::mapToTicketDto)
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<TicketDto> moveToCompleted(int ticketId) {
        try {
            ticketService.moveToCompleted(ticketId);
            Ticket t = ticketService.getCompletedTickets().stream().filter(x -> x.getId() == ticketId).findFirst()
                    .orElse(null);
            return ApiResponse.success(mapToTicketDto(t));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<TicketDto> moveToClosed(int ticketId) {
        try {
            ticketService.moveToClosed(ticketId);
            Ticket t = ticketService.getClosedTickets().stream().filter(x -> x.getId() == ticketId).findFirst()
                    .orElse(null);
            return ApiResponse.success(mapToTicketDto(t));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<TicketDto> moveToActive(int ticketId) {
        try {
            ticketService.moveToActive(ticketId);
            return getTicket(ticketId);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<List<String>> removeTicket(int ticketId) {
        try {
            ticketService.removeTicket(ticketId);
            return ApiResponse.success(java.util.Collections.emptyList());
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
