package utils.menu.dto;

import org.junit.Test;
import static org.junit.Assert.*;

public class SideTest {

    @Test
    public void testSideFields() {
        Side side = new Side();
        side.price = 1.0;
        side.available = true;
        assertTrue(side.available);
        assertEquals(1.0, side.price, 0.001);
    }
}
