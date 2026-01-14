package com.ticketer.controllers;

import com.ticketer.models.Settings;
import com.ticketer.utils.settings.SettingsEditor;
import com.ticketer.utils.settings.SettingsReader;
import java.io.IOException;

public class SettingsController {

    private Settings settings;

    public SettingsController() throws IOException {
        refreshSettings();
    }

    public void refreshSettings() throws IOException {
        this.settings = SettingsReader.readSettings();
    }

    public Settings getSettings() {
        return settings;
    }

    public double getTax() {
        return settings != null ? settings.getTax() : 0.0;
    }

    public java.util.Map<String, String> getOpeningHours() {
        return settings != null ? settings.getHours() : java.util.Collections.emptyMap();
    }

    public String getOpeningHours(String day) {
        if (settings == null || settings.getHours() == null) {
            return "closed";
        }
        return settings.getHours().getOrDefault(day.toLowerCase(), "closed");
    }

    public String getOpenTime(String day) {
        String hours = getOpeningHours(day);
        if ("closed".equalsIgnoreCase(hours)) {
            return null;
        }
        String[] parts = hours.split(" - ");
        return parts.length > 0 ? parts[0] : null;
    }

    public String getCloseTime(String day) {
        String hours = getOpeningHours(day);
        if ("closed".equalsIgnoreCase(hours)) {
            return null;
        }
        String[] parts = hours.split(" - ");
        return parts.length > 1 ? parts[1] : null;
    }

    public void setTax(double tax) throws IOException {
        SettingsEditor.setTax(tax);
        refreshSettings();
    }

    public void setOpeningHours(String day, String hours) throws IOException {
        SettingsEditor.setOpeningHours(day, hours);
        refreshSettings();
    }
}
