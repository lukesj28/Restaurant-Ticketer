package com.ticketer.repositories;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ticketer.models.Settings;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.springframework.stereotype.Repository;

@Repository
public class FileSettingsRepository implements SettingsRepository {

    private final String filePath;
    private final Gson gson;

    public FileSettingsRepository() {
        this.filePath = System.getProperty("settings.file", "data/settings.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public FileSettingsRepository(String filePath) {
        this.filePath = filePath;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public Settings getSettings() {
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            return new Settings(0.0, new java.util.HashMap<>());
        }

        try (FileReader reader = new FileReader(file)) {
            Settings settings = gson.fromJson(reader, Settings.class);
            if (settings == null) {
                return new Settings(0.0, new java.util.HashMap<>());
            }
            return settings;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load settings from " + filePath, e);
        }
    }

    @Override
    public void saveSettings(Settings settings) {
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save settings to " + filePath, e);
        }
    }
}
