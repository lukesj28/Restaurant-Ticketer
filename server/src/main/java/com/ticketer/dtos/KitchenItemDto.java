package com.ticketer.dtos;

public record KitchenItemDto(
        String name,
        String selectedSide,
        String selectedExtra,
        int quantity,
        String comment) {
}
