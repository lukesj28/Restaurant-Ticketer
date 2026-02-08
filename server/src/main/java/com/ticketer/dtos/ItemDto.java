package com.ticketer.dtos;

import java.util.Map;

public record ItemDto(
                String name,
                long price,
                boolean available,
                Map<String, SideDto> sides,
                java.util.List<String> sideOrder) {
}
