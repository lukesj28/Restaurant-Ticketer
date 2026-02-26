package com.ticketer.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

public record ComboItemDto(
        UUID id,
        String name,
        String category,
        List<BaseItemDto> components,
        List<ComboSlotDto> slots,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long price,
        boolean available,
        boolean kitchen) {
}
