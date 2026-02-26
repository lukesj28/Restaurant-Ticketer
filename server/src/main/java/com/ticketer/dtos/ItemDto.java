package com.ticketer.dtos;

import java.util.List;
import java.util.UUID;

public record ItemDto(
        UUID baseItemId,
        String name,
        long price,
        boolean available,
        boolean kitchen,
        List<String> sideSources,
        List<BaseItemDto> sideOptions) {
}
