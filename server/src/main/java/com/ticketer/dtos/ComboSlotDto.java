package com.ticketer.dtos;

import java.util.List;
import java.util.UUID;

public record ComboSlotDto(
        UUID id,
        String name,
        List<BaseItemDto> options,
        List<UUID> optionOrder,
        boolean required) {
}
