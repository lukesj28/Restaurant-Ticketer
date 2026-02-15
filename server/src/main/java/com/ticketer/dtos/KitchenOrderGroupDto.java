package com.ticketer.dtos;

import java.util.List;

public record KitchenOrderGroupDto(
        String comment,
        List<KitchenItemDto> items) {
}
