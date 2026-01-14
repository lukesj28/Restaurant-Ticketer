package com.ticketer.utils.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ticketer.models.Settings;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class SettingsEditor {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static void saveSettings(Settings settings) {
        try (FileWriter writer = new FileWriter(SettingsReader.getSettingsFilePath())) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            throw new com.ticketer.exceptions.StorageException("Failed to save settings", e);
        }
    }

    public static void setTax(double tax) {
        Settings settings = SettingsReader.readSettings();
        if (settings == null) {
            settings = new Settings(tax, Collections.emptyMap());
        } else {
            settings = new Settings(tax, settings.getHours());
        }
        saveSettings(settings);
    }

    public static void setOpeningHours(String day, String hours) {
        Settings settings = SettingsReader.readSettings();
        Map<String, String> newHours;

        if (settings == null) {
            newHours = new java.util.HashMap<>();
            newHours.put(day.toLowerCase(), hours);
            settings = new Settings(0.0, newHours);
        } else {
            newHours = settings.getHours() != null ? new java.util.HashMap<>(settings.getHours())
                    : new java.util.HashMap<>();
            newHours.put(day.toLowerCase(), hours);
            settings = new Settings(settings.getTax(), newHours);
        }

        saveSettings(settings);
    }

}
