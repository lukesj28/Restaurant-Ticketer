package com.ticketer.dtos;

import java.util.Map;

public record SettingsDto(
        double tax,
        Map<String, String> openingHours) {
}
