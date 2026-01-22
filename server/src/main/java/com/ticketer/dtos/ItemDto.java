package com.ticketer.dtos;

import java.util.Map;

public record ItemDto(
                String name,
                int price,
                boolean available,
                Map<String, SideDto> sides) {
}
