package com.ticketer.models;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class SettingsTest {

    @Test
    public void testGetTax() {
        Settings settings = new Settings(0.05, new HashMap<>());
        assertEquals(0.05, settings.getTax(), 0.0001);
    }

    @Test
    public void testGetHours() {
        Map<String, String> hours = new HashMap<>();
        hours.put("monday", "10:00 - 20:00");
        Settings settings = new Settings(0.05, hours);

        assertEquals(hours, settings.getHours());
    }

    @Test
    public void testGetHoursNull() {
        Settings settings = new Settings(0.12, null);
        assertNull(settings.getHours());
    }
}
