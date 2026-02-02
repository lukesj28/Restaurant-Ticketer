package com.ticketer.models;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Settings {
    private int taxBasisPoints;
    private Map<String, String> hours;

    @JsonCreator
    public Settings(@JsonProperty("tax") int taxBasisPoints, @JsonProperty("hours") Map<String, String> hours) {
        this.taxBasisPoints = taxBasisPoints;
        this.hours = hours;
    }

    public int getTax() {
        return taxBasisPoints;
    }

    public Map<String, String> getHours() {
        return hours;
    }
}
