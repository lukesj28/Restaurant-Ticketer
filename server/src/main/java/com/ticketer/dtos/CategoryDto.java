package com.ticketer.dtos;

import java.util.List;

public record CategoryDto(
        boolean visible,
        List<ItemDto> items) {
}
