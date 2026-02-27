package com.ticketer.dtos;

import java.util.UUID;

public record CompositeComponentDto(UUID baseItemId, String name, double quantity) {}
