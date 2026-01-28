package com.ticketer.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketer.models.Settings;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class FileSettingsRepository implements SettingsRepository {

    private final String filePath;
    private final ObjectMapper objectMapper;

    @Autowired
    public FileSettingsRepository(ObjectMapper objectMapper) {
        this.filePath = System.getProperty("settings.file", "data/settings.json");
        this.objectMapper = objectMapper;
    }

    public FileSettingsRepository(String filePath, ObjectMapper objectMapper) {
        this.filePath = filePath;
        this.objectMapper = objectMapper;
    }

    @Override
    public Settings getSettings() {
        File file = new File(filePath);
        if (!file.exists()) {
            return new Settings(0.0, new HashMap<>());
        }

        try (FileReader reader = new FileReader(file)) {
            Settings settings = objectMapper.readValue(reader, Settings.class);
            if (settings == null) {
                return new Settings(0.0, new HashMap<>());
            }
            return settings;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load settings from " + filePath, e);
        }
    }

    @Override
    public void saveSettings(Settings settings) {
        try (FileWriter writer = new FileWriter(filePath)) {
            objectMapper.writeValue(writer, settings);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save settings to " + filePath, e);
        }
    }
}
