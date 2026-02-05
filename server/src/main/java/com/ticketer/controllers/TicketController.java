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

    @PostMapping("/{ticketId}/orders")
    public ApiResponse<TicketDto> addOrderToTicket(@PathVariable("ticketId") int ticketId) {
        logger.info("Received request to add order to ticket: {}", ticketId);
        ticketService.addOrderToTicket(ticketId, new Order(settingsService.getTax()));
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
        OrderItem item = menuService.createOrderItem(request.name(), request.selectedSide());
        ticketService.addItemToOrder(ticketId, orderIndex, item);
        return getTicket(ticketId);
    }

    @DeleteMapping("/{ticketId}/orders/{orderIndex}/items")
    public ApiResponse<TicketDto> removeItemFromOrder(@PathVariable("ticketId") int ticketId,
            @PathVariable("orderIndex") int orderIndex,
            @RequestBody Requests.AddItemRequest request) {
        OrderItem item = new OrderItem(request.name(), request.selectedSide(), 0, 0);
        ticketService.removeItemFromOrder(ticketId, orderIndex, item);
        return getTicket(ticketId);
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
    public ApiResponse<Integer> getClosedTicketsSubtotal() {
        return ApiResponse.success(ticketService.getClosedTicketsSubtotal());
    }

    @GetMapping("/closed/total")
    public ApiResponse<Integer> getClosedTicketsTotal() {
        return ApiResponse.success(ticketService.getClosedTicketsTotal());
    }

    @GetMapping("/active-completed/subtotal")
    public ApiResponse<Integer> getActiveAndCompletedTicketsSubtotal() {
        return ApiResponse.success(ticketService.getActiveAndCompletedTicketsSubtotal());
    }

    @GetMapping("/active-completed/total")
    public ApiResponse<Integer> getActiveAndCompletedTicketsTotal() {
        return ApiResponse.success(ticketService.getActiveAndCompletedTicketsTotal());
    }
}
