package com.ticketer.utils.menu.dto;

import org.junit.Test;
import static org.junit.Assert.*;

public class ComplexItemTest {

    @Test
    public void testHasSides() {
        ComplexItem cItem = new ComplexItem("Name", 1.0, true, null);
        assertFalse(cItem.hasSides());
    }
}
