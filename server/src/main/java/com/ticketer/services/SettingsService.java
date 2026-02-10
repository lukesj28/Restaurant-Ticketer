package com.ticketer.services;

import com.ticketer.models.Settings;
import com.ticketer.repositories.SettingsRepository;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.InvalidInputException;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SettingsService.class);

    private final SettingsRepository settingsRepository;
    private Settings currentSettings;

    @Autowired
    public SettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
        this.currentSettings = settingsRepository.getSettings();
    }

    public void refreshSettings() {
        this.currentSettings = settingsRepository.getSettings();
    }

    public Settings getSettings() {
        if (currentSettings == null) {
            throw new EntityNotFoundException("Settings not found");
        }
        return currentSettings;
    }

    public int getTax() {
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

    public synchronized void setTax(int taxBasisPoints) {
        logger.info("Setting tax to {}", taxBasisPoints);
        if (taxBasisPoints < 0) {
            throw new InvalidInputException("Tax cannot be negative");
        }
        Settings settings = getSettings();
        Settings newSettings = new Settings(taxBasisPoints, settings.getHours(), settings.getPrinter());
        this.currentSettings = newSettings;
        settingsRepository.saveSettings(newSettings);
    }

    public synchronized void setOpeningHours(String day, String hours) {
        logger.info("Setting opening hours for {}: {}", day, hours);
        String pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9] - ([0-1][0-9]|2[0-3]):[0-5][0-9]$|^closed$";

        if (!hours.matches(pattern)) {
            throw new InvalidInputException(
                    "Invalid hours format. Expected 'HH:MM - HH:MM' or 'closed'");
        }

        day = day.toLowerCase();
        Settings settings = getSettings();
        java.util.Map<String, String> currentHours = settings.getHours();

        java.util.Map<String, String> newHours = new java.util.HashMap<>();
        if (currentHours != null) {
            newHours.putAll(currentHours);
        }
        newHours.put(day, hours);

        Settings newSettings = new Settings(settings.getTax(), newHours, settings.getPrinter());
        this.currentSettings = newSettings;
        settingsRepository.saveSettings(newSettings);
    }

    public synchronized void setPrinterSettings(Settings.PrinterSettings printerSettings) {
        logger.info("Updating printer settings");
        Settings settings = getSettings();
        Settings newSettings = new Settings(settings.getTax(), settings.getHours(), printerSettings);
        this.currentSettings = newSettings;
        settingsRepository.saveSettings(newSettings);
    }
}
