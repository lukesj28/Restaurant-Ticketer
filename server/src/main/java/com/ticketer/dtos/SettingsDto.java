package com.ticketer.dtos;

import java.util.Map;

public record SettingsDto(
                int tax,
                Map<String, String> openingHours) {
}
