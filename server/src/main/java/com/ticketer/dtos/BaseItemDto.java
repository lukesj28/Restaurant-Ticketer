package com.ticketer.dtos;

import java.util.UUID;

public record BaseItemDto(
        UUID id,
        String name,
        long price,
        boolean available,
        boolean kitchen) {
}
