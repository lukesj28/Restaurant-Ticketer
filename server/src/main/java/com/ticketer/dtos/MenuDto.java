package com.ticketer.dtos;

import java.util.List;
import java.util.Map;

public record MenuDto(
        List<BaseItemDto> baseItems,
        Map<String, CategoryDto> categories,
        List<ComboItemDto> combos,
        List<String> categoryOrder) {
}
