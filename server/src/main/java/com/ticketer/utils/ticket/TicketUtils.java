package com.ticketer.utils.ticket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ticketer.models.Ticket;

import java.lang.reflect.Type;
import java.util.List;

public class TicketUtils {

    private static class TicketTypeAdapter implements JsonSerializer<Ticket> {
        @Override
        public JsonElement serialize(Ticket ticket, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("tableNumber", ticket.getTableNumber());

            JsonObject ordersJson = new JsonObject();
            List<com.ticketer.models.Order> orders = ticket.getOrders();
            for (int i = 0; i < orders.size(); i++) {
                ordersJson.add(String.valueOf(i + 1), context.serialize(orders.get(i)));
            }
            json.add("orders", ordersJson);

            json.addProperty("subtotal", ticket.getSubtotal() / 100.0);
            json.addProperty("total", ticket.getTotal() / 100.0);
            json.addProperty("createdAt", ticket.getCreatedAt().toString());
            if (ticket.getClosedAt() != null) {
                json.addProperty("closedAt", ticket.getClosedAt().toString());
            }
            return json;
        }
    }

    private static class ItemTypeAdapter implements JsonSerializer<com.ticketer.models.OrderItem> {
        @Override
        public JsonElement serialize(com.ticketer.models.OrderItem item, Type typeOfSrc,
                JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("name", item.getName());
            if (item.getSelectedSide() != null) {
                json.addProperty("selectedSide", item.getSelectedSide());
            }
            json.addProperty("price", item.getPrice() / 100.0);
            return json;
        }
    }

    private static class OrderTypeAdapter implements JsonSerializer<com.ticketer.models.Order> {
        @Override
        public JsonElement serialize(com.ticketer.models.Order order, Type typeOfSrc,
                JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.add("items", context.serialize(order.getItems()));
            json.addProperty("subtotal", order.getSubtotal() / 100.0);
            json.addProperty("total", order.getTotal() / 100.0);
            return json;
        }
    }

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Ticket.class, new TicketTypeAdapter())
            .registerTypeAdapter(com.ticketer.models.OrderItem.class, new ItemTypeAdapter())
            .registerTypeAdapter(com.ticketer.models.Order.class, new OrderTypeAdapter())
            .create();

    public static String serializeTicket(Ticket ticket) {
        return gson.toJson(ticket);
    }

    public static String serializeTickets(List<Ticket> tickets) {
        return gson.toJson(tickets);
    }

    public static java.util.Map<String, Integer> countItems(Ticket ticket) {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (com.ticketer.models.Order order : ticket.getOrders()) {
            for (com.ticketer.models.OrderItem item : order.getItems()) {
                counts.put(item.getName(), counts.getOrDefault(item.getName(), 0) + 1);

                String side = item.getSelectedSide();
                if (side != null && !side.equalsIgnoreCase("none")) {
                    counts.put(side, counts.getOrDefault(side, 0) + 1);
                }
            }
        }
        return counts;
    }
}
