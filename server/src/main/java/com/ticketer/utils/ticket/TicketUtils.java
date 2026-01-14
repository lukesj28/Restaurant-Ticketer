package com.ticketer.utils.ticket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ticketer.models.Ticket;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TicketUtils {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

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

            json.addProperty("subtotal", round(ticket.getSubtotal()));
            json.addProperty("total", round(ticket.getTotal()));
            json.addProperty("createdAt", TIME_FORMAT.format(new Date(ticket.getCreatedAt())));
            json.addProperty("closedAt", TIME_FORMAT.format(new Date()));
            return json;
        }
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class ItemTypeAdapter implements JsonSerializer<com.ticketer.models.Item> {
        @Override
        public JsonElement serialize(com.ticketer.models.Item item, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("name", item.getName());
            if (item.getSelectedSide() != null) {
                json.addProperty("selectedSide", item.getSelectedSide());
            }
            json.addProperty("price", round(item.getPrice()));
            return json;
        }
    }

    private static class OrderTypeAdapter implements JsonSerializer<com.ticketer.models.Order> {
        @Override
        public JsonElement serialize(com.ticketer.models.Order order, Type typeOfSrc,
                JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.add("items", context.serialize(order.getItems()));
            json.addProperty("subtotal", round(order.getSubtotal()));
            json.addProperty("total", round(order.getTotal()));
            return json;
        }
    }

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Ticket.class, new TicketTypeAdapter())
            .registerTypeAdapter(com.ticketer.models.Item.class, new ItemTypeAdapter())
            .registerTypeAdapter(com.ticketer.models.Order.class, new OrderTypeAdapter())
            .create();

    public static String serializeTicket(Ticket ticket) {
        return gson.toJson(ticket);
    }

    public static java.util.Map<String, Integer> countItems(Ticket ticket) {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (com.ticketer.models.Order order : ticket.getOrders()) {
            for (com.ticketer.models.Item item : order.getItems()) {
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
