package com.ticketer.dtos;

public record OrderItemDto(
        String name,
        String selectedSide,
        long mainPrice,
        long sidePrice) {
}
