package com.ticketer.models;

import org.junit.Test;
import static org.junit.Assert.*;

public class SideTest {

    @Test
    public void testSideFields() {
        Side side = new Side();
        side.price = 100;
        side.available = true;
        assertTrue(side.available);
        assertEquals(100, side.price);
    }
}
