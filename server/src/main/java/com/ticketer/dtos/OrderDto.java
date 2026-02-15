package com.ticketer.dtos;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

public record OrderDto(
                List<OrderItemDto> items,
                long subtotal,
                long total,
                long tax,
                int taxRate,
                @JsonInclude(JsonInclude.Include.NON_NULL)
                String comment) {
}
