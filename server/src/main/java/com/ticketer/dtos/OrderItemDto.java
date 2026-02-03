package com.ticketer.dtos;

public record OrderItemDto(
                String name,
                String selectedSide,
                int mainPrice,
                int sidePrice) {
}
