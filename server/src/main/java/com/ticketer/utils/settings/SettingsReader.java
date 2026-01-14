package com.ticketer.utils.settings;

import com.google.gson.Gson;
import com.ticketer.models.Settings;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class SettingsReader {

    private static final Gson gson = new Gson();

    public static String getSettingsFilePath() {
        return System.getProperty("settings.file", "data/settings.json");
    }

    private static Settings loadConfig() {
        try (FileReader reader = new FileReader(getSettingsFilePath())) {
            return gson.fromJson(reader, Settings.class);
        } catch (IOException e) {
            throw new com.ticketer.exceptions.StorageException("Failed to load settings", e);
        }
    }

    public static Settings readSettings() {
        return loadConfig();
    }

    public static double getTax() {
        Settings config = loadConfig();
        if (config == null) {
            throw new com.ticketer.exceptions.StorageException("Could not load settings configuration.");
        }
        return config.getTax();
    }

    public static String getOpeningHours(String day) {
        Settings config = loadConfig();
        if (config == null || config.getHours() == null) {
            return "closed";
        }
        return config.getHours().getOrDefault(day.toLowerCase(), "closed");
    }

    public static String getOpenTime(String day) {
        String hours = getOpeningHours(day);
        if ("closed".equalsIgnoreCase(hours)) {
            return null;
        }
        String[] parts = hours.split(" - ");
        return parts.length > 0 ? parts[0] : null;
    }

    public static String getCloseTime(String day) {
        String hours = getOpeningHours(day);
        if ("closed".equalsIgnoreCase(hours)) {
            return null;
        }
        String[] parts = hours.split(" - ");
        return parts.length > 1 ? parts[1] : null;
    }

    public static Map<String, String> getOpeningHours() {
        Settings config = loadConfig();
        if (config == null || config.getHours() == null) {
            return Collections.emptyMap();
        }
        return config.getHours();
    }
}
