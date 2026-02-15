package com.ticketer.dtos;

public record KitchenItemDto(
        String name,
        String selectedSide,
        int quantity,
        String comment) {
}
