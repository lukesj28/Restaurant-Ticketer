package com.ticketer.models;

import org.junit.Test;
import static org.junit.Assert.*;

public class ItemTest {

    @Test
    public void testToString() {
        Item item = new Item("Name", "Side", 10.0);
        assertNotNull(item.toString());

        Item item2 = new Item("Name", null, 10.0);
        assertNotNull(item2.toString());
    }
}
