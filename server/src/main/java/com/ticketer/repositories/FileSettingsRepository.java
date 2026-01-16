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
        try (FileReader reader = new FileReader(filePath)) {
            Settings settings = gson.fromJson(reader, Settings.class);
            if (settings == null) {
                return null;
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
