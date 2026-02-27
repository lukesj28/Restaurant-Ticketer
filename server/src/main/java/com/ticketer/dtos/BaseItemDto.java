package com.ticketer.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

public record BaseItemDto(
        UUID id,
        String name,
        long price,
        boolean available,
        boolean kitchen,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<CompositeComponentDto> components) {

    public BaseItemDto(UUID id, String name, long price, boolean available, boolean kitchen) {
        this(id, name, price, available, kitchen, null);
    }
}
