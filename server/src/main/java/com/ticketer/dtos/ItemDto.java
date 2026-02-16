package com.ticketer.dtos;

import java.util.Map;

public record ItemDto(
                String name,
                long price,
                boolean available,
                Map<String, SideDto> sides,
                java.util.List<String> sideOrder,
                Map<String, ExtraDto> extras,
                java.util.List<String> extraOrder) {
}
