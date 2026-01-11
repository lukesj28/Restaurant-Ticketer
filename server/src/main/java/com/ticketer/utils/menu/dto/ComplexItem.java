package com.ticketer.utils.menu.dto;

import java.util.Map;

public class ComplexItem {
    public String name;
    public double basePrice;
    public boolean available;
    public Map<String, Side> sideOptions;

    public ComplexItem(String name, double basePrice, boolean available, Map<String, Side> sideOptions) {
        this.name = name;
        this.basePrice = basePrice;
        this.available = available;
        this.sideOptions = sideOptions;
    }

    public boolean hasSides() {
        return sideOptions != null && !sideOptions.isEmpty();
    }
}
