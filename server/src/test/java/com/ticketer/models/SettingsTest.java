package com.ticketer.models;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class SettingsTest {

    @Test
    public void testGetTax() {
        Settings settings = new Settings(500, new HashMap<>(), null, null, null);
        assertEquals(500, settings.getTax());
    }

    @Test
    public void testGetHours() {
        Map<String, String> hours = new HashMap<>();
        hours.put("monday", "10:00 - 20:00");
        Settings settings = new Settings(500, hours, null, null, null);

        assertEquals(hours, settings.getHours());
    }

    @Test
    public void testGetHoursNull() {
        Settings settings = new Settings(1200, null, null, null, null);
        assertNull(settings.getHours());
    }
}
