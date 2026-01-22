package com.ticketer.dtos;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ticketer.models.MenuItem;
import com.ticketer.models.Order;
import com.ticketer.models.OrderItem;
import com.ticketer.models.Settings;
import com.ticketer.models.Ticket;

public class DtoMapper {

    public static SettingsDto toSettingsDto(Settings settings) {
        if (settings == null)
            return null;
        return new SettingsDto(settings.getTax(), settings.getHours());
    }

    public static OrderItemDto toOrderItemDto(OrderItem item) {
        return new OrderItemDto(item.getName(), item.getSelectedSide(), item.getPrice());
    }

    public static OrderDto toOrderDto(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(DtoMapper::toOrderItemDto)
                .collect(Collectors.toList());
        return new OrderDto(items, order.getSubtotal(), order.getTotal(), order.getTaxRate());
    }

    public static TicketDto toTicketDto(Ticket ticket) {
        if (ticket == null)
            return null;
        List<OrderDto> orders = ticket.getOrders().stream()
                .map(DtoMapper::toOrderDto)
                .collect(Collectors.toList());
        return new TicketDto(
                ticket.getId(),
                ticket.getTableNumber(),
                orders,
                ticket.getSubtotal(),
                ticket.getTotal(),
                ticket.getCreatedAt() != null ? ticket.getCreatedAt().toString() : null,
                ticket.getClosedAt() != null ? ticket.getClosedAt().toString() : null);
    }

    public static ItemDto toItemDto(MenuItem item) {
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
}
