package com.ticketer.dtos;

import java.util.List;
import java.util.Map;

public record KitchenTicketDto(
                int id,
                String tableNumber,
                Map<String, Integer> kitchenTally,
                List<KitchenOrderGroupDto> kitchenOrders,
                String createdAt,
                String comment) {
}
