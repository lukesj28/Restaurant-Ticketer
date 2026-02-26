package com.ticketer.dtos;

import java.util.UUID;

public record ComboSlotSelectionDto(
        UUID slotId,
        UUID selectedBaseItemId,
        String selectedName) {
}
