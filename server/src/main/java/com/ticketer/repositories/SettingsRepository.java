package com.ticketer.repositories;

import com.ticketer.models.Settings;

public interface SettingsRepository {
    Settings getSettings();

    void saveSettings(Settings settings);
}
