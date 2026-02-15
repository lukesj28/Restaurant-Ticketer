package com.ticketer.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

public record OrderItemDto(
        String name,
        String selectedSide,
        long mainPrice,
        long sidePrice,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String comment) {
}
