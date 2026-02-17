package com.ticketer.dtos;

import java.util.List;
import java.util.Map;

public class Requests {
    public record ItemCreateRequest(String category, String name, long price, Map<String, Long> sides, Boolean kitchen) {
    }

    public record ItemPriceUpdateRequest(long newPrice) {
    }

    public record ItemAvailabilityUpdateRequest(boolean available) {
    }

    public record ItemRenameRequest(String newName) {
    }

    public record CategoryRenameRequest(String newCategory) {
    }

    public record ItemCategoryUpdateRequest(String newCategory) {
    }

    public record SideUpdateRequest(Long price, Boolean available, Boolean kitchen) {
    }

    public record SideCreateRequest(String name, long price) {
    }

    public record TaxUpdateRequest(int tax) {
    }

    public record OpeningHoursUpdateRequest(String hours) {
    }

    public record AddItemRequest(String category, String name, String selectedSide, String selectedExtra, String comment) {
    }

    public record AddOrderRequest(String comment) {
    }

    public record AnalysisRequest(String startDate, String endDate) {
    }

    public record CreateTicketRequest(String tableNumber, String comment) {
    }

    public record UpdateCommentRequest(String comment) {
    }

    public record CategoryReorderRequest(List<String> order) {
    }

    public record ItemReorderRequest(List<String> order) {
    }

    public record SideReorderRequest(List<String> order) {
    }

    public record ExtraCreateRequest(String name, long price) {
    }

    public record ExtraUpdateRequest(Long price, Boolean available, Boolean kitchen) {
    }

    public record ExtraReorderRequest(List<String> order) {
    }
}
