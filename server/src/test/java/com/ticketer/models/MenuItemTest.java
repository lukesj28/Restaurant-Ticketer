package com.ticketer.models;

import org.junit.Test;
import static org.junit.Assert.*;

public class MenuItemTest {

    @Test
    public void testHasSides() {
        MenuItem cItem = new MenuItem("Name", 100, true, null, null, null, null);
        assertFalse(cItem.hasSides());
    }
}
