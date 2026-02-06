package com.ticketer.dtos;

import java.util.List;

public record KitchenOrderDto(List<KitchenOrderItemDto> items) {
}
