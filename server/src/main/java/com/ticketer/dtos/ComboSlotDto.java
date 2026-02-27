package com.ticketer.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

public record ComboSlotDto(
        UUID id,
        String name,
        List<BaseItemDto> options,
        List<UUID> optionOrder,
        boolean required,
        @JsonInclude(JsonInclude.Include.NON_NULL) String categorySource) {

    public ComboSlotDto(UUID id, String name, List<BaseItemDto> options, List<UUID> optionOrder, boolean required) {
        this(id, name, options, optionOrder, required, null);
    }
}
