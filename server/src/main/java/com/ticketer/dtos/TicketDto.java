package com.ticketer.dtos;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

public record TicketDto(
        int id,
        String tableNumber,
        List<OrderDto> orders,
        long subtotal,
        long total,
        long tax,
        String status,
        String createdAt,
        String closedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String comment) {
}
