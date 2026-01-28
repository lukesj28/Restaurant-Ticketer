package com.ticketer.models;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Settings {
    private double tax;
    private Map<String, String> hours;

    @JsonCreator
    public Settings(@JsonProperty("tax") double tax, @JsonProperty("hours") Map<String, String> hours) {
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
