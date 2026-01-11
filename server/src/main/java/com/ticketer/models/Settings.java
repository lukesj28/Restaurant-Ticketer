package com.ticketer.models;

import java.util.Map;

public class Settings {
    public double tax;
    public Map<String, String> hours;

    public Settings(double tax, Map<String, String> hours) {
        this.tax = tax;
        this.hours = hours;
    }
}
