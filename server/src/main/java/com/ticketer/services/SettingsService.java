package com.ticketer.services;

import com.ticketer.models.Settings;
import com.ticketer.repositories.SettingsRepository;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.ValidationException;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;

    @Autowired
    public SettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public Settings getSettings() {
        Settings settings = settingsRepository.getSettings();
        if (settings == null) {
            throw new EntityNotFoundException("Settings not found");
        }
        return settings;
    }

    public double getTax() {
        return getSettings().getTax();
    }

    public String getOpeningHours(String day) {
        Settings settings = getSettings();
        if (settings.getHours() == null) {
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

    public Map<String, String> getAllOpeningHours() {
        Settings settings = getSettings();
        return settings.getHours();
    }

    public void setTax(double tax) {
        if (tax < 0) {
            throw new ValidationException("Tax cannot be negative");
        }
        Settings settings = getSettings();
        Settings newSettings = new Settings(tax, settings.getHours());
        settingsRepository.saveSettings(newSettings);
    }

    public void setOpeningHours(String day, String hours) {
        day = day.toLowerCase();
        Settings settings = getSettings();
        java.util.Map<String, String> currentHours = settings.getHours();

        java.util.Map<String, String> newHours = new java.util.HashMap<>();
        if (currentHours != null) {
            newHours.putAll(currentHours);
        }
        newHours.put(day, hours);

        Settings newSettings = new Settings(settings.getTax(), newHours);
        settingsRepository.saveSettings(newSettings);
    }
}
