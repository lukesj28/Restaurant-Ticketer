package com.ticketer.dtos;

import java.util.List;

public record TicketDto(
                int id,
                String tableNumber,
                List<OrderDto> orders,
                int subtotal,
                int total,
                String createdAt,
                String closedAt) {
}
