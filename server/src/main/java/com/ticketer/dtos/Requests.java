package com.ticketer.dtos;

import java.util.Map;

public class Requests {
    public record ItemCreateRequest(String category, String name, int price, Map<String, Integer> sides) {
    }

    public record ItemPriceUpdateRequest(int newPrice) {
    }

    public record ItemAvailabilityUpdateRequest(boolean available) {
    }

    public record ItemRenameRequest(String newName) {
    }

    public record CategoryRenameRequest(String newCategory) {
    }

    public record ItemCategoryUpdateRequest(String newCategory) {
    }

    public record SideUpdateRequest(Integer price, Boolean available) {
    }

    public record TaxUpdateRequest(int tax) {
    }

    public record OpeningHoursUpdateRequest(String hours) {
    }

    public record AddItemRequest(String name, String selectedSide) {
    }

    public record AnalysisRequest(String startDate, String endDate) {
    }

    public record CreateTicketRequest(String tableNumber) {
    }
}
