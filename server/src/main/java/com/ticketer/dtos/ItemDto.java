package com.ticketer.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

public record ItemDto(
        UUID baseItemId,
        String name,
        long price,
        boolean available,
        boolean kitchen,
        boolean alcohol,
        List<String> sideSources,
        List<BaseItemDto> sideOptions,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<CompositeComponentDto> components) {

    public ItemDto(UUID baseItemId, String name, long price, boolean available, boolean kitchen,
            List<String> sideSources, List<BaseItemDto> sideOptions) {
        this(baseItemId, name, price, available, kitchen, false, sideSources, sideOptions, null);
    }
}
