package com.ticketer.models;

import java.util.Map;

public class MenuItem {
    public String name;
    public int basePrice;
    public boolean available;
    public Map<String, Side> sideOptions;

    public MenuItem(String name, int basePrice, boolean available, Map<String, Side> sideOptions) {
        this.name = name;
        this.basePrice = basePrice;
        this.available = available;
        this.sideOptions = sideOptions;
    }

    public boolean hasSides() {
        return sideOptions != null && !sideOptions.isEmpty();
    }
}
