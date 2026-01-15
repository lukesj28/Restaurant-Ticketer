package com.ticketer.utils.menu.dto;

import org.junit.Test;
import static org.junit.Assert.*;

public class ComplexItemTest {

    @Test
    public void testHasSides() {
        ComplexItem cItem = new ComplexItem("Name", 100, true, null);
        assertFalse(cItem.hasSides());
    }
}
