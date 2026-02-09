package com.ticketer.dtos;

import java.util.List;

public record OrderDto(
                List<OrderItemDto> items,
                long subtotal,
                long total,
                long tax,
                int taxRate) {
}
