package com.ticketer.dtos;

import java.util.List;
import java.util.UUID;

public class Requests {

    public record ItemCreateRequest(String category, String name, long price, Boolean kitchen,
            List<String> sideSources) {
    }

    public record ItemPriceUpdateRequest(long newPrice) {
    }

    public record ItemAvailabilityUpdateRequest(boolean available) {
    }

    public record ItemKitchenUpdateRequest(boolean kitchen) {
    }

    public record ItemRenameRequest(String newName) {
    }

    public record CategoryRenameRequest(String newCategory) {
    }

    public record CategoryVisibleRequest(boolean visible) {
    }

    public record ItemCategoryUpdateRequest(String newCategory) {
    }

    public record SetSideSourcesRequest(List<String> sideSources) {
    }

    public record CategoryReorderRequest(List<String> order) {
    }
    public record ItemReorderRequest(List<UUID> order) {
    }

    public record ComboSlotRequest(UUID id, String name, List<UUID> optionIds, Boolean required) {
    }

    public record CreateComboRequest(String name, String category,
            List<UUID> componentIds, List<ComboSlotRequest> slots,
            Long price, Boolean kitchen) {
    }

    public record UpdateComboRequest(String name, Long price, Boolean available, Boolean kitchen) {
    }

    public record TaxUpdateRequest(int tax) {
    }

    public record OpeningHoursUpdateRequest(String hours) {
    }

    public record CreateTicketRequest(String tableNumber, String comment) {
    }

    public record AddOrderRequest(String comment) {
    }

    public record UpdateCommentRequest(String comment) {
    }

    public record AddItemOrderRequest(UUID menuItemId, UUID selectedSideId, String comment) {
    }

    public record SlotSelectionRequest(UUID slotId, UUID selectedBaseItemId) {
    }

    public record AddComboOrderRequest(UUID comboId,
            List<SlotSelectionRequest> slotSelections, String comment) {
    }

    public record MoveItemRequest(int targetOrderIndex) {
    }

    public record MergeOrdersRequest(int targetOrderIndex) {
    }

    public record PrintOrdersRequest(List<Integer> orderIndices) {
    }

    public record AnalysisRequest(String startDate, String endDate) {
    }
}
