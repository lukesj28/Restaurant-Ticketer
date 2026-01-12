package com.ticketer.models;

import java.util.Map;

public class Settings {
    private double tax;
    private Map<String, String> hours;

    public Settings(double tax, Map<String, String> hours) {
        this.tax = tax;
        this.hours = hours;
    }

    public double getTax() {
        return tax;
    }

    public Map<String, String> getHours() {
        return hours;
    }
}
