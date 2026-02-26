package com.ticketer.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

public record OrderItemDto(
        String type,
        String name,
        @JsonInclude(JsonInclude.Include.NON_NULL) String selectedSide,
        long mainPrice,
        long sidePrice,
        @JsonInclude(JsonInclude.Include.NON_NULL) String comment,
        @JsonInclude(JsonInclude.Include.NON_NULL) UUID comboId,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<ComboSlotSelectionDto> slotSelections) {
}
