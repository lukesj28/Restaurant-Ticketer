package com.ticketer.repositories;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ticketer.models.Order;
import com.ticketer.models.OrderItem;
import com.ticketer.models.Ticket;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileTicketRepository implements TicketRepository {

    private final List<Ticket> activeTickets = new ArrayList<>();
    private final List<Ticket> completedTickets = new ArrayList<>();
    private final List<Ticket> closedTickets = new ArrayList<>();

    private final String ticketsDir;
    private final Gson gson;

    public FileTicketRepository() {
        this.ticketsDir = System.getProperty("tickets.dir", "data/tickets");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Ticket.class, new TicketTypeAdapter())
                .registerTypeAdapter(OrderItem.class, new ItemTypeAdapter())
                .registerTypeAdapter(Order.class, new OrderTypeAdapter())
                .create();
    }

    @Override
    public Ticket save(Ticket ticket) {
        if (findById(ticket.getId()).isPresent()) {
            return ticket;
        }
        activeTickets.add(ticket);
        return ticket;
    }

    @Override
    public Optional<Ticket> findById(int id) {
        Optional<Ticket> found = activeTickets.stream().filter(t -> t.getId() == id).findFirst();
        if (found.isPresent())
            return found;

        found = completedTickets.stream().filter(t -> t.getId() == id).findFirst();
        if (found.isPresent())
            return found;

        return closedTickets.stream().filter(t -> t.getId() == id).findFirst();
    }

    @Override
    public List<Ticket> findAllActive() {
        return activeTickets;
    }

    @Override
    public List<Ticket> findAllCompleted() {
        return completedTickets;
    }

    @Override
    public List<Ticket> findAllClosed() {
        return closedTickets;
    }

    @Override
    public boolean deleteById(int id) {
        if (activeTickets.removeIf(t -> t.getId() == id))
            return true;
        if (completedTickets.removeIf(t -> t.getId() == id))
            return true;
        return closedTickets.removeIf(t -> t.getId() == id);
    }

    @Override
    public void deleteAll() {
        activeTickets.clear();
        completedTickets.clear();
        closedTickets.clear();
    }

    @Override
    public void persistClosedTickets() {
        String date = LocalDate.now().toString();
        String filename = ticketsDir + "/" + date + ".json";

        File directory = new File(ticketsDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(closedTickets, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize closed tickets", e);
        }
    }

    @Override
    public void moveToCompleted(int id) {
        Optional<Ticket> ticketOpt = activeTickets.stream().filter(t -> t.getId() == id).findFirst();
        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            activeTickets.remove(ticket);
            completedTickets.add(ticket);
        }
    }

    @Override
    public void moveToClosed(int id) {
        Optional<Ticket> ticketOpt = completedTickets.stream().filter(t -> t.getId() == id).findFirst();
        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            completedTickets.remove(ticket);
            closedTickets.add(ticket);
        }
    }

    @Override
    public void moveToActive(int id) {
        Optional<Ticket> ticketOpt = completedTickets.stream().filter(t -> t.getId() == id).findFirst();
        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            completedTickets.remove(ticket);
            activeTickets.add(ticket);
        }
    }

    private static class TicketTypeAdapter implements JsonSerializer<Ticket> {
        @Override
        public JsonElement serialize(Ticket ticket, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("tableNumber", ticket.getTableNumber());

            JsonObject ordersJson = new JsonObject();
            List<Order> orders = ticket.getOrders();
            for (int i = 0; i < orders.size(); i++) {
                ordersJson.add(String.valueOf(i + 1), context.serialize(orders.get(i)));
            }
            json.add("orders", ordersJson);

            json.addProperty("subtotal", ticket.getSubtotal() / 100.0);
            json.addProperty("total", ticket.getTotal() / 100.0);
            json.addProperty("createdAt", ticket.getCreatedAt());
            json.addProperty("closedAt", java.time.Instant.now().toString());
            return json;
        }
    }

    private static class ItemTypeAdapter implements JsonSerializer<OrderItem> {
        @Override
        public JsonElement serialize(OrderItem item, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("name", item.getName());
            if (item.getSelectedSide() != null) {
                json.addProperty("selectedSide", item.getSelectedSide());
            }
            json.addProperty("price", item.getPrice() / 100.0);
            return json;
        }
    }

    private static class OrderTypeAdapter implements JsonSerializer<Order> {
        @Override
        public JsonElement serialize(Order order, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.add("items", context.serialize(order.getItems()));
            json.addProperty("subtotal", order.getSubtotal() / 100.0);
            json.addProperty("total", order.getTotal() / 100.0);
            return json;
        }
    }
}
