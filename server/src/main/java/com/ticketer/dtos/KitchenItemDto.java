package com.ticketer.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record KitchenItemDto(
        String name,
        @JsonInclude(JsonInclude.Include.NON_NULL) String selectedSide,
        int quantity,
        @JsonInclude(JsonInclude.Include.NON_NULL) String comment,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<String> slotSelectionNames) {
}
