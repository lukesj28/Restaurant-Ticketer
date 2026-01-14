package com.ticketer.utils.ticket;

import com.ticketer.models.Item;
import com.ticketer.models.Order;
import com.ticketer.models.Ticket;
import org.junit.Test;
import static org.junit.Assert.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TicketUtilsTest {

    @Test
    public void testSerializeTicket() {
        Ticket ticket = new Ticket(1);
        ticket.setTableNumber("T1");

        Order order = new Order(0.1);
        order.addItem(new Item("Burger", null, 10.0));
        ticket.addOrder(order);

        String jsonString = TicketUtils.serializeTicket(ticket);
        assertNotNull(jsonString);

        JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

        assertTrue(json.has("tableNumber"));
        assertTrue(json.has("orders"));
        assertTrue(json.getAsJsonObject("orders").has("1"));
        assertTrue(json.has("subtotal"));
        assertTrue(json.has("total"));
        assertTrue(json.has("createdAt"));
        assertTrue(json.has("closedAt"));

        assertFalse(json.has("id"));
        assertFalse(json.has("taxRate"));

        assertEquals("T1", json.get("tableNumber").getAsString());
        assertEquals(10.0, json.get("subtotal").getAsDouble(), 0.001);
        assertEquals(11.0, json.get("total").getAsDouble(), 0.001);

        String createdAt = json.get("createdAt").getAsString();
        assertTrue(createdAt.matches("\\d{2}:\\d{2}:\\d{2}"));

        String closedAt = json.get("closedAt").getAsString();
        assertTrue(closedAt.matches("\\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    public void testCountItems() {
        Ticket ticket = new Ticket(1);
        Order order1 = new Order();
        order1.addItem(new Item("Burger", "Fries", 15.0));
        order1.addItem(new Item("Burger", "none", 12.0));

        Order order2 = new Order();
        order2.addItem(new Item("Soda", null, 2.0));
        order2.addItem(new Item("Fries", null, 5.0));

        ticket.addOrder(order1);
        ticket.addOrder(order2);

        java.util.Map<String, Integer> counts = TicketUtils.countItems(ticket);

        assertEquals(2, (int) counts.get("Burger"));
        assertEquals(1, (int) counts.get("Soda"));

        assertEquals(2, (int) counts.get("Fries"));
        assertFalse(counts.containsKey("none"));
    }
}
