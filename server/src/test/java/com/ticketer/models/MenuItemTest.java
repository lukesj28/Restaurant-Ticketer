package com.ticketer.models;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import static org.junit.Assert.*;

public class MenuItemTest {

    @Test
    public void testConstructorAndGetters() {
        UUID id = UUID.randomUUID();
        MenuItem item = new MenuItem(id, Arrays.asList("side options"));
        assertEquals(id, item.getBaseItemId());
        assertEquals(1, item.getSideSources().size());
        assertEquals("side options", item.getSideSources().get(0));
    }

    @Test
    public void testEmptySideSources() {
        UUID id = UUID.randomUUID();
        MenuItem item = new MenuItem(id, Collections.emptyList());
        assertNotNull(item.getSideSources());
        assertTrue(item.getSideSources().isEmpty());
    }

    @Test
    public void testSetSideSources() {
        UUID id = UUID.randomUUID();
        MenuItem item = new MenuItem(id, Collections.emptyList());
        item.setSideSources(Arrays.asList("a", "b"));
        assertEquals(2, item.getSideSources().size());
    }
}
