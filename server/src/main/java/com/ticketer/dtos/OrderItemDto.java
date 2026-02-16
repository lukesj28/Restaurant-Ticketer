package com.ticketer.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

public record OrderItemDto(
        String name,
        String selectedSide,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String selectedExtra,
        long mainPrice,
        long sidePrice,
        long extraPrice,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String comment) {
}
