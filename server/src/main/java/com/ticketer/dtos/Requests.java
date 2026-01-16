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

    public record SideUpdateRequest(int newPrice) {
    }

    public record TaxUpdateRequest(double tax) {
    }
}
