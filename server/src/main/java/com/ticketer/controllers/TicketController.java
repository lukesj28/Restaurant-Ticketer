package com.ticketer.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.ticketer.api.ApiResponse;
import com.ticketer.dtos.*;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.models.Order;
import com.ticketer.models.OrderItem;
import com.ticketer.models.Ticket;
import com.ticketer.services.MenuService;
import com.ticketer.services.SettingsService;
import com.ticketer.services.TicketService;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TicketController.class);

    private final TicketService ticketService;
    private final MenuService menuService;
    private final SettingsService settingsService;

    @Autowired
    public TicketController(TicketService ticketService, MenuService menuService, SettingsService settingsService) {
        this.ticketService = ticketService;
        this.menuService = menuService;
        this.settingsService = settingsService;
    }

    @PostMapping("/counter/reset")
    public ApiResponse<String> resetTicketCounter() {
        logger.info("Received request to reset ticket counter");
        ticketService.clearAllTickets();
        return ApiResponse.success("Ticket counter reset and all tickets cleared.");
    }

    @PostMapping("")
    public ApiResponse<TicketDto> createTicket(@RequestBody Requests.CreateTicketRequest request) {
        logger.info("Received request to create ticket for table: {}", request.tableNumber());
        Ticket ticket = ticketService.createTicket(request.tableNumber());
        if (request.comment() != null && !request.comment().trim().isEmpty()) {
            ticket.setComment(request.comment());
        }
        return ApiResponse.success(DtoMapper.toTicketDto(ticket));
    }

    @GetMapping("/{ticketId}")
    public ApiResponse<TicketDto> getTicket(@PathVariable("ticketId") int ticketId) {
        Ticket ticket = ticketService.getTicket(ticketId);
        if (ticket == null)
            throw new EntityNotFoundException("Ticket not found");
        return ApiResponse.success(DtoMapper.toTicketDto(ticket));
    }

    @GetMapping("/{ticketId}/tally")
    public ApiResponse<java.util.Map<String, Integer>> getTicketTally(@PathVariable("ticketId") int ticketId) {
        Ticket ticket = ticketService.getTicket(ticketId);
        if (ticket == null) {
            throw new EntityNotFoundException("Ticket not found");
        }
        return ApiResponse.success(ticket.getTally());
    }

    @GetMapping("/{ticketId}/tally/kitchen")
    public ApiResponse<java.util.Map<String, Integer>> getTicketKitchenTally(@PathVariable("ticketId") int ticketId) {
        Ticket ticket = ticketService.getTicket(ticketId);
        if (ticket == null) {
            throw new EntityNotFoundException("Ticket not found");
        }

        java.util.Map<String, Integer> fullTally = ticket.getTally();
        List<String> kitchenItems = menuService.getKitchenItems();

        java.util.Map<String, Integer> kitchenTally = new java.util.HashMap<>();
        for (String item : kitchenItems) {
            if (fullTally.containsKey(item)) {
                kitchenTally.put(item, fullTally.get(item));
            }
        }

        return ApiResponse.success(kitchenTally);
    }

    @PostMapping("/{ticketId}/orders")
    public ApiResponse<TicketDto> addOrderToTicket(@PathVariable("ticketId") int ticketId,
            @RequestBody(required = false) Requests.AddOrderRequest request) {
        logger.info("Received request to add order to ticket: {}", ticketId);
        Order order = new Order(settingsService.getTax());
        if (request != null && request.comment() != null && !request.comment().trim().isEmpty()) {
            order.setComment(request.comment());
        }
        ticketService.addOrderToTicket(ticketId, order);
        return getTicket(ticketId);
    }

    @GetMapping("/{ticketId}/orders/{orderIndex}")
    public ApiResponse<OrderDto> getOrder(@PathVariable("ticketId") int ticketId,
            @PathVariable("orderIndex") int orderIndex) {
        Order order = ticketService.getOrder(ticketId, orderIndex);
        return ApiResponse.success(DtoMapper.toOrderDto(order));
    }

    @DeleteMapping("/{ticketId}/orders/{orderIndex}")
    public ApiResponse<TicketDto> removeOrderFromTicket(@PathVariable("ticketId") int ticketId,
            @PathVariable("orderIndex") int orderIndex) {
        ticketService.removeOrder(ticketId, orderIndex);
        return getTicket(ticketId);
    }

    @PostMapping("/{ticketId}/orders/{orderIndex}/items")
    public ApiResponse<TicketDto> addItemToOrder(@PathVariable("ticketId") int ticketId,
            @PathVariable("orderIndex") int orderIndex,
            @RequestBody Requests.AddItemRequest request) {
        logger.info("Received request to add item {} to ticket {} order {}", request.name(), ticketId, orderIndex);
        OrderItem item = menuService.createOrderItem(request.category(), request.name(), request.selectedSide(), request.selectedExtra());
        ticketService.addItemToOrder(ticketId, orderIndex, item, request.comment());
        return getTicket(ticketId);
    }

    @DeleteMapping("/{ticketId}/orders/{orderIndex}/items")
    public ApiResponse<TicketDto> removeItemFromOrder(@PathVariable("ticketId") int ticketId,
            @PathVariable("orderIndex") int orderIndex,
            @RequestBody Requests.AddItemRequest request) {
        OrderItem item = new OrderItem(request.name(), request.selectedSide(), request.selectedExtra(), 0, 0, 0, null);
        ticketService.removeItemFromOrder(ticketId, orderIndex, item);
        return getTicket(ticketId);
    }

    @PutMapping("/{ticketId}/comment")
    public ApiResponse<TicketDto> updateTicketComment(@PathVariable("ticketId") int ticketId,
            @RequestBody Requests.UpdateCommentRequest request) {
        ticketService.updateTicketComment(ticketId, request.comment());
        return getTicket(ticketId);
    }

    @PutMapping("/{ticketId}/orders/{orderIndex}/comment")
    public ApiResponse<TicketDto> updateOrderComment(@PathVariable("ticketId") int ticketId,
            @PathVariable("orderIndex") int orderIndex,
            @RequestBody Requests.UpdateCommentRequest request) {
        ticketService.updateOrderComment(ticketId, orderIndex, request.comment());
        return getTicket(ticketId);
    }

    @PutMapping("/{ticketId}/orders/{orderIndex}/items/{itemIndex}/comment")
    public ApiResponse<TicketDto> updateItemComment(@PathVariable("ticketId") int ticketId,
            @PathVariable("orderIndex") int orderIndex,
            @PathVariable("itemIndex") int itemIndex,
            @RequestBody Requests.UpdateCommentRequest request) {
        ticketService.updateItemComment(ticketId, orderIndex, itemIndex, request.comment());
        return getTicket(ticketId);
    }

    @GetMapping("/active/kitchen")
    public ApiResponse<List<KitchenTicketDto>> getActiveKitchenTickets() {
        List<String> kitchenItems = menuService.getKitchenItems();
        List<Ticket> kitchenTickets = ticketService.getKitchenTickets();

        List<KitchenTicketDto> result = kitchenTickets.stream().map(ticket -> {
            java.util.Map<String, Integer> fullTally = ticket.getTally();
            java.util.Map<String, Integer> kitchenTally = new java.util.LinkedHashMap<>();
            for (String item : kitchenItems) {
                if (fullTally.containsKey(item)) {
                    kitchenTally.put(item, fullTally.get(item));
                }
            }

            List<KitchenOrderGroupDto> kitchenOrders = new java.util.ArrayList<>();

            for (Order order : ticket.getOrders()) {
                List<KitchenItemDto> groupItems = groupKitchenItems(order, kitchenItems);
                if (!groupItems.isEmpty() || (order.getComment() != null && !order.getComment().trim().isEmpty())) {
                    kitchenOrders.add(new KitchenOrderGroupDto(order.getComment(), groupItems));
                }
            }

            return new KitchenTicketDto(
                    ticket.getId(),
                    ticket.getTableNumber(),
                    kitchenTally,
                    kitchenOrders,
                    ticket.getCreatedAt() != null ? ticket.getCreatedAt().toString() : null,
                    ticket.getComment());
        }).collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    private List<KitchenItemDto> groupKitchenItems(Order order, List<String> kitchenItems) {
        List<KitchenItemDto> result = new java.util.ArrayList<>();
        java.util.Map<String, java.util.Map<String, java.util.Map<String, Integer>>> grouped = new java.util.LinkedHashMap<>();

        for (OrderItem item : order.getItems()) {
            if (!kitchenItems.contains(item.getName())) {
                continue;
            }
            if (item.getComment() != null && !item.getComment().trim().isEmpty()) {
                result.add(new KitchenItemDto(item.getName(), item.getSelectedSide(), item.getSelectedExtra(), 1, item.getComment()));
            } else {
                String sideKey = item.getSelectedSide() != null ? item.getSelectedSide() : "";
                String extraKey = item.getSelectedExtra() != null ? item.getSelectedExtra() : "";
                grouped.computeIfAbsent(item.getName(), k -> new java.util.LinkedHashMap<>())
                       .computeIfAbsent(sideKey, k -> new java.util.LinkedHashMap<>())
                       .merge(extraKey, 1, Integer::sum);
            }
        }

        for (String kitchenItemName : kitchenItems) {
            if (grouped.containsKey(kitchenItemName)) {
                for (java.util.Map.Entry<String, java.util.Map<String, Integer>> sideEntry : grouped.get(kitchenItemName).entrySet()) {
                    String side = sideEntry.getKey().isEmpty() ? null : sideEntry.getKey();
                    for (java.util.Map.Entry<String, Integer> extraEntry : sideEntry.getValue().entrySet()) {
                        String extra = extraEntry.getKey().isEmpty() ? null : extraEntry.getKey();
                        result.add(new KitchenItemDto(kitchenItemName, side, extra, extraEntry.getValue(), null));
                    }
                }
            }
        }

        return result;
    }

    @PostMapping("/{ticketId}/kitchen")
    public ApiResponse<String> sendToKitchen(@PathVariable("ticketId") int ticketId) {
        logger.info("Received request to send ticket {} to kitchen", ticketId);
        ticketService.sendToKitchen(ticketId);
        return ApiResponse.success("Ticket sent to kitchen.");
    }

    @PostMapping("/{ticketId}/kitchen/complete")
    public ApiResponse<String> completeKitchenTicket(@PathVariable("ticketId") int ticketId) {
        logger.info("Received request to complete kitchen ticket {}", ticketId);
        ticketService.completeKitchenTicket(ticketId);
        return ApiResponse.success("Kitchen ticket completed.");
    }

    @GetMapping("/active")
    public ApiResponse<List<TicketDto>> getActiveTickets() {
        return ApiResponse.success(
                ticketService.getActiveTickets().stream().map(DtoMapper::toTicketDto)
                        .collect(Collectors.toList()));
    }

    @GetMapping("/completed")
    public ApiResponse<List<TicketDto>> getCompletedTickets() {
        return ApiResponse.success(ticketService.getCompletedTickets().stream().map(DtoMapper::toTicketDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/closed")
    public ApiResponse<List<TicketDto>> getClosedTickets() {
        return ApiResponse.success(
                ticketService.getClosedTickets().stream().map(DtoMapper::toTicketDto)
                        .collect(Collectors.toList()));
    }

    @PutMapping("/{ticketId}/completed")
    public ApiResponse<TicketDto> moveToCompleted(@PathVariable("ticketId") int ticketId) {
        logger.info("Received request to move ticket {} to completed", ticketId);
        ticketService.moveToCompleted(ticketId);
        Ticket t = ticketService.getCompletedTickets().stream().filter(x -> x.getId() == ticketId).findFirst()
                .orElse(null);
        return ApiResponse.success(DtoMapper.toTicketDto(t));
    }

    @PutMapping("/{ticketId}/closed")
    public ApiResponse<TicketDto> moveToClosed(@PathVariable("ticketId") int ticketId) {
        logger.info("Received request to move ticket {} to closed", ticketId);
        ticketService.moveToClosed(ticketId);
        Ticket t = ticketService.getClosedTickets().stream().filter(x -> x.getId() == ticketId).findFirst()
                .orElse(null);
        return ApiResponse.success(DtoMapper.toTicketDto(t));
    }

    @PutMapping("/{ticketId}/active")
    public ApiResponse<TicketDto> moveToActive(@PathVariable("ticketId") int ticketId) {
        logger.info("Received request to move ticket {} to active", ticketId);
        ticketService.moveToActive(ticketId);
        return getTicket(ticketId);
    }

    @DeleteMapping("/{ticketId}")
    public ApiResponse<List<String>> removeTicket(@PathVariable("ticketId") int ticketId) {
        logger.info("Received request to remove ticket: {}", ticketId);
        ticketService.removeTicket(ticketId);
        return ApiResponse.success(java.util.Collections.emptyList());
    }

    @GetMapping("/closed/subtotal")
    public ApiResponse<Long> getClosedTicketsSubtotal() {
        return ApiResponse.success(ticketService.getClosedTicketsSubtotal());
    }

    @GetMapping("/closed/total")
    public ApiResponse<Long> getClosedTicketsTotal() {
        return ApiResponse.success(ticketService.getClosedTicketsTotal());
    }

    @GetMapping("/active-completed/subtotal")
    public ApiResponse<Long> getActiveAndCompletedTicketsSubtotal() {
        return ApiResponse.success(ticketService.getActiveAndCompletedTicketsSubtotal());
    }

    @GetMapping("/active-completed/total")
    public ApiResponse<Long> getActiveAndCompletedTicketsTotal() {
        return ApiResponse.success(ticketService.getActiveAndCompletedTicketsTotal());
    }
}
