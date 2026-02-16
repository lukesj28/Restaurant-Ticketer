package com.ticketer.models;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExtraTest {

    @Test
    public void testDefaultConstructor() {
        Extra extra = new Extra();
        assertEquals(0, extra.price);
        assertFalse(extra.available);
    }

    @Test
    public void testCopyConstructor() {
        Extra original = new Extra();
        original.price = 150;
        original.available = true;

        Extra copy = new Extra(original);
        assertEquals(150, copy.price);
        assertTrue(copy.available);

        copy.price = 200;
        assertEquals(150, original.price);
    }
}
