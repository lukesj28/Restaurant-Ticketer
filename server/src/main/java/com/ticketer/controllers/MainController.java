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

import com.ticketer.models.Item;
import com.ticketer.models.Ticket;
import com.ticketer.models.Order;
import com.ticketer.utils.menu.dto.ComplexItem;
import com.ticketer.utils.menu.dto.MenuItemView;
import com.ticketer.api.ApiResponse;
import com.ticketer.dtos.*;
import com.ticketer.exceptions.TicketerException;

import java.util.stream.Collectors;

public class MainController {

    private final MenuController menuController;
    private final SettingsController settingsController;
    private final TicketController ticketController;

    private final ScheduledExecutorService scheduler;
    private boolean isOpen;

    public MainController() {
        this.menuController = new MenuController();
        this.settingsController = new SettingsController();
        this.ticketController = new TicketController();

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.isOpen = false;

        checkAndScheduleState();
    }

    private void checkAndScheduleState() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        String dayName = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toLowerCase();

        String openTimeStr = settingsController.getOpenTime(dayName);
        String closeTimeStr = settingsController.getCloseTime(dayName);

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
            if (ticketController.areAllTicketsClosed()) {
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
        ticketController.moveAllToClosed();
        ticketController.serializeClosedTickets();
        ticketController.clearAllTickets();
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

    private OrderItemDto mapToOrderItemDto(Item item) {
        return new OrderItemDto(item.getName(), item.getSelectedSide(), item.getPrice());
    }

    private Item mapFromOrderItemDto(OrderItemDto dto) {
        return new Item(dto.name(), dto.selectedSide(), dto.price());
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

    private ItemDto mapToItemDto(ComplexItem item, String category) {
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
            menuController.refreshMenu();
            return ApiResponse.success(java.util.Collections.emptyList());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<ItemDto> getItem(String name) {
        try {
            ComplexItem item = menuController.getItem(name);
            if (item == null) {
                return ApiResponse.error("Item not found");
            }
            String category = menuController.getCategoryOfItem(name);
            return ApiResponse.success(mapToItemDto(item, category));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error retrieving item: " + e.getMessage());
        }
    }

    public Item getItem(ComplexItem item, String sideName) {
        return menuController.getItem(item, sideName);
    }

    public ApiResponse<List<ItemDto>> getCategory(String categoryName) {
        try {
            List<ComplexItem> items = menuController.getCategory(categoryName);
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
            List<MenuItemView> views = menuController.getAllItems();
            List<ItemDto> dtos = views.stream()
                    .map(v -> {
                        ComplexItem full = menuController.getItem(v.name);
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
            Map<String, List<ComplexItem>> categories = menuController.getCategories();
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
            menuController.addItem(category, name, price, sides);
            return getItem(name); // Re-use getItem to return the DTO
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error adding item: " + e.getMessage());
        }
    }

    public ApiResponse<ItemDto> editItemPrice(String itemName, int newPrice) {
        try {
            menuController.editItemPrice(itemName, newPrice);
            return getItem(itemName);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error editing item price: " + e.getMessage());
        }
    }

    public ApiResponse<ItemDto> editItemAvailability(String itemName, boolean available) {
        try {
            menuController.editItemAvailability(itemName, available);
            return getItem(itemName);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error editing item availability: " + e.getMessage());
        }
    }

    public ApiResponse<ItemDto> renameItem(String oldName, String newName) {
        try {
            menuController.renameItem(oldName, newName);
            return getItem(newName);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error renaming item: " + e.getMessage());
        }
    }

    public ApiResponse<List<String>> removeItem(String itemName) {
        try {
            menuController.removeItem(itemName);
            return ApiResponse.success(java.util.Collections.emptyList());
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error removing item: " + e.getMessage());
        }
    }

    public ApiResponse<List<ItemDto>> renameCategory(String oldCategory, String newCategory) {
        try {
            menuController.renameCategory(oldCategory, newCategory);
            return getCategory(newCategory);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error renaming category: " + e.getMessage());
        }
    }

    public ApiResponse<ItemDto> changeCategory(String itemName, String newCategory) {
        try {
            menuController.changeCategory(itemName, newCategory);
            return getItem(itemName);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error changing category: " + e.getMessage());
        }
    }

    public ApiResponse<ItemDto> updateSide(String itemName, String sideName, int newPrice) {
        try {
            menuController.updateSide(itemName, sideName, newPrice);
            return getItem(itemName);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error updating side: " + e.getMessage());
        }
    }

    public ApiResponse<List<String>> refreshSettings() {
        try {
            settingsController.refreshSettings();
            return ApiResponse.success(java.util.Collections.emptyList());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<Double> getTax() {
        try {
            return ApiResponse.success(settingsController.getTax());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<Map<String, String>> getOpeningHours() {
        try {
            return ApiResponse.success(settingsController.getOpeningHours());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<String> getOpeningHours(String day) {
        try {
            return ApiResponse.success(settingsController.getOpeningHours(day));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<String> getOpenTime(String day) {
        try {
            return ApiResponse.success(settingsController.getOpenTime(day));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<String> getCloseTime(String day) {
        try {
            return ApiResponse.success(settingsController.getCloseTime(day));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<SettingsDto> setTax(double tax) {
        try {
            settingsController.setTax(tax);
            return ApiResponse.success(mapToSettingsDto(settingsController.getSettings()));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error setting tax: " + e.getMessage());
        }
    }

    public ApiResponse<SettingsDto> setOpeningHours(String day, String hours) {
        try {
            settingsController.setOpeningHours(day, hours);
            return ApiResponse.success(mapToSettingsDto(settingsController.getSettings()));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error setting opening hours: " + e.getMessage());
        }
    }

    public ApiResponse<TicketDto> createTicket(String tableNumber) {
        try {
            Ticket ticket = ticketController.createTicket(tableNumber);
            return ApiResponse.success(mapToTicketDto(ticket));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<TicketDto> getTicket(int ticketId) {
        try {
            Ticket ticket = ticketController.getTicket(ticketId);
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
            ticketController.addOrderToTicket(ticketId, order);
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
            ticketController.removeMatchingOrder(ticketId, target);
            return getTicket(ticketId);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error removing order: " + e.getMessage());
        }
    }

    public ApiResponse<OrderDto> createOrder(double taxRate) {
        try {
            Order order = ticketController.createOrder(taxRate);
            return ApiResponse.success(mapToOrderDto(order));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<OrderDto> addItemToOrder(OrderDto orderDto,
            OrderItemDto itemDto) {
        try {
            Order order = mapFromOrderDto(orderDto);
            Item item = mapFromOrderItemDto(itemDto);
            ticketController.addItemToOrder(order, item);
            return ApiResponse.success(mapToOrderDto(order));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<OrderDto> removeItemFromOrder(OrderDto orderDto,
            OrderItemDto itemDto) {
        try {
            Order order = mapFromOrderDto(orderDto);
            Item target = mapFromOrderItemDto(itemDto);
            ticketController.removeMatchingItem(order, target);
            return ApiResponse.success(mapToOrderDto(order));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<List<TicketDto>> getActiveTickets() {
        try {
            return ApiResponse.success(
                    ticketController.getActiveTickets().stream().map(this::mapToTicketDto)
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<List<TicketDto>> getCompletedTickets() {
        try {
            return ApiResponse.success(ticketController.getCompletedTickets().stream().map(this::mapToTicketDto)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<List<TicketDto>> getClosedTickets() {
        try {
            return ApiResponse.success(
                    ticketController.getClosedTickets().stream().map(this::mapToTicketDto)
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<TicketDto> moveToCompleted(int ticketId) {
        try {
            ticketController.moveToCompleted(ticketId);
            Ticket t = ticketController.getCompletedTickets().stream().filter(x -> x.getId() == ticketId).findFirst()
                    .orElse(null);
            return ApiResponse.success(mapToTicketDto(t));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<TicketDto> moveToClosed(int ticketId) {
        try {
            ticketController.moveToClosed(ticketId);
            Ticket t = ticketController.getClosedTickets().stream().filter(x -> x.getId() == ticketId).findFirst()
                    .orElse(null);
            return ApiResponse.success(mapToTicketDto(t));
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<TicketDto> moveToActive(int ticketId) {
        try {
            ticketController.moveToActive(ticketId);
            return getTicket(ticketId);
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<List<String>> removeTicket(int ticketId) {
        try {
            ticketController.removeTicket(ticketId);
            return ApiResponse.success(java.util.Collections.emptyList());
        } catch (TicketerException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
