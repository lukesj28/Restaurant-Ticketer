package com.ticketer.dtos;

import java.util.List;

public record TicketDto(
                int id,
                String tableNumber,
                List<OrderDto> orders,
                long subtotal,
                long total,
                String status,
                String createdAt,
                String closedAt) {
}
