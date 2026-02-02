package com.ticketer.dtos;

import java.util.List;

public record OrderDto(
                List<OrderItemDto> items,
                int subtotal,
                int total,
                int taxRate) {
}
