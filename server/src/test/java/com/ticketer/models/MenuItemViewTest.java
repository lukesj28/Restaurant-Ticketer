package com.ticketer.models;

import org.junit.Test;
import static org.junit.Assert.*;

public class MenuItemViewTest {

    @Test
    public void testToString() {
        MenuItemView view = new MenuItemView("Name", 1000, true);
        assertEquals("Name: $10.00 [Available]", view.toString());

        MenuItemView view2 = new MenuItemView("Name", 1000, false);
        assertEquals("Name: $10.00 [Out of Stock]", view2.toString());
    }
}
