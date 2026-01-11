package com.ticketer.utils.menu.dto;

import org.junit.Test;
import static org.junit.Assert.*;

public class MenuItemViewTest {

    @Test
    public void testToString() {
        MenuItemView view = new MenuItemView("Name", 10.0, true, "Cat");
        assertEquals("Name: $10.00 [Available]", view.toString());

        MenuItemView view2 = new MenuItemView("Name", 10.0, false, "Cat");
        assertEquals("Name: $10.00 [Out of Stock]", view2.toString());
    }
}
