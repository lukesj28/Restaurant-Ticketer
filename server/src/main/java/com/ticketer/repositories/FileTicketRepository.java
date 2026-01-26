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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Repository;

@Repository
public class FileTicketRepository implements TicketRepository {

    private final List<Ticket> activeTickets = new CopyOnWriteArrayList<>();
    private final List<Ticket> completedTickets = new CopyOnWriteArrayList<>();
    private final List<Ticket> closedTickets = new CopyOnWriteArrayList<>();

    private final String ticketsDir;
    private final String recoveryFilePath;
    private final Gson gson;
    private final Object fileLock = new Object();

    public FileTicketRepository() {
        this.ticketsDir = System.getProperty("tickets.dir", "data/tickets");
        this.recoveryFilePath = System.getProperty("recovery.file", "data/recovery.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Ticket.class, new TicketTypeAdapter())
                .registerTypeAdapter(OrderItem.class, new ItemTypeAdapter())
                .registerTypeAdapter(Order.class, new OrderTypeAdapter())
                .registerTypeAdapter(java.time.Instant.class, new InstantTypeAdapter())
                .create();

        loadStateFromRecoveryFile();
    }

    public FileTicketRepository(String ticketsDir, String recoveryFilePath) {
        this.ticketsDir = ticketsDir;
        this.recoveryFilePath = recoveryFilePath;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Ticket.class, new TicketTypeAdapter())
                .registerTypeAdapter(OrderItem.class, new ItemTypeAdapter())
                .registerTypeAdapter(Order.class, new OrderTypeAdapter())
                .registerTypeAdapter(java.time.Instant.class, new InstantTypeAdapter())
                .create();

        loadStateFromRecoveryFile();
    }

    @Override
    public Ticket save(Ticket ticket) {
        if (findById(ticket.getId()).isPresent()) {
            saveStateToRecoveryFile();
            return ticket;
        }
        activeTickets.add(ticket);
        saveStateToRecoveryFile();
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
        boolean removed = false;
        if (activeTickets.removeIf(t -> t.getId() == id))
            removed = true;
        else if (completedTickets.removeIf(t -> t.getId() == id))
            removed = true;
        else if (closedTickets.removeIf(t -> t.getId() == id))
            removed = true;

        if (removed) {
            saveStateToRecoveryFile();
        }
        return removed;
    }

    @Override
    public void deleteAll() {
        activeTickets.clear();
        completedTickets.clear();
        closedTickets.clear();
        saveStateToRecoveryFile();
    }

    @Override
    public void persistClosedTickets() {
        String date = LocalDate.now().toString();
        String filename = ticketsDir + "/" + date + ".json";

        File directory = new File(ticketsDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        synchronized (fileLock) {
            try (FileWriter writer = new FileWriter(filename)) {
                gson.toJson(closedTickets, writer);
            } catch (IOException e) {
                throw new RuntimeException("Failed to serialize closed tickets", e);
            }
        }
    }

    @Override
    public void moveToCompleted(int id) {
        Optional<Ticket> ticketOpt = activeTickets.stream().filter(t -> t.getId() == id).findFirst();
        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            activeTickets.remove(ticket);
            ticket.setClosedAt(null);
            completedTickets.add(ticket);
            saveStateToRecoveryFile();
        }
    }

    @Override
    public void moveToClosed(int id) {
        Optional<Ticket> ticketOpt = completedTickets.stream().filter(t -> t.getId() == id).findFirst();
        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            completedTickets.remove(ticket);
            ticket.setClosedAt(java.time.Instant.now());
            closedTickets.add(ticket);
            saveStateToRecoveryFile();
        }
    }

    @Override
    public void moveToActive(int id) {
        Optional<Ticket> ticketOpt = completedTickets.stream().filter(t -> t.getId() == id).findFirst();
        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            completedTickets.remove(ticket);
            ticket.setClosedAt(null);
            activeTickets.add(ticket);
            saveStateToRecoveryFile();
        }
    }

    private void saveStateToRecoveryFile() {
        RecoverableState state = new RecoverableState(activeTickets, completedTickets);
        File file = new File(recoveryFilePath);
        file.getParentFile().mkdirs();

        synchronized (fileLock) {
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(state, writer);
            } catch (IOException e) {
                System.err.println("Failed to save recovery state: " + e.getMessage());
            }
        }
    }

    private void loadStateFromRecoveryFile() {
        File file = new File(recoveryFilePath);
        if (!file.exists()) {
            return;
        }

        synchronized (fileLock) {
            try (FileReader reader = new FileReader(file)) {
                RecoverableState state = gson.fromJson(reader, RecoverableState.class);
                if (state != null) {
                    if (state.active != null)
                        activeTickets.addAll(state.active);
                    if (state.completed != null)
                        completedTickets.addAll(state.completed);
                }
            } catch (IOException e) {
                System.err.println("Failed to load recovery state: " + e.getMessage());
            }
        }
    }

    @Override
    public void clearRecoveryFile() {
        synchronized (fileLock) {
            File file = new File(recoveryFilePath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private static class RecoverableState {
        List<Ticket> active;
        List<Ticket> completed;

        RecoverableState(List<Ticket> active, List<Ticket> completed) {
            this.active = active;
            this.completed = completed;
        }
    }

    private static class TicketTypeAdapter implements JsonSerializer<Ticket> {
        @Override
        public JsonElement serialize(Ticket ticket, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("id", ticket.getId());
            json.addProperty("tableNumber", ticket.getTableNumber());

            json.add("orders", context.serialize(ticket.getOrders()));

            json.addProperty("subtotal", ticket.getSubtotal() / 100.0);
            json.addProperty("total", ticket.getTotal() / 100.0);
            json.addProperty("createdAt", ticket.getCreatedAt().toString());
            if (ticket.getClosedAt() != null) {
                json.addProperty("closedAt", ticket.getClosedAt().toString());
            }
            return json;
        }
    }

    private static class ItemTypeAdapter
            implements JsonSerializer<OrderItem>, com.google.gson.JsonDeserializer<OrderItem> {
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

        @Override
        public OrderItem deserialize(JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context)
                throws com.google.gson.JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String name = obj.get("name").getAsString();
            String selectedSide = null;
            if (obj.has("selectedSide") && !obj.get("selectedSide").isJsonNull()) {
                selectedSide = obj.get("selectedSide").getAsString();
            }
            double priceDouble = obj.get("price").getAsDouble();
            int price = (int) Math.round(priceDouble * 100);
            return new OrderItem(name, selectedSide, price);
        }
    }

    private static class OrderTypeAdapter implements JsonSerializer<Order>, com.google.gson.JsonDeserializer<Order> {
        @Override
        public JsonElement serialize(Order order, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.add("items", context.serialize(order.getItems()));
            json.addProperty("subtotal", order.getSubtotal() / 100.0);
            json.addProperty("total", order.getTotal() / 100.0);
            return json;
        }

        @Override
        public Order deserialize(JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context)
                throws com.google.gson.JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            Order order = new Order();
            if (obj.has("items")) {
                com.google.gson.JsonArray itemsArray = obj.getAsJsonArray("items");
                for (JsonElement itemElement : itemsArray) {
                    OrderItem item = context.deserialize(itemElement, OrderItem.class);
                    order.addItem(item);
                }
            }
            return order;
        }
    }

    private static class InstantTypeAdapter
            implements JsonSerializer<java.time.Instant>, com.google.gson.JsonDeserializer<java.time.Instant> {
        @Override
        public JsonElement serialize(java.time.Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(src.toString());
        }

        @Override
        public java.time.Instant deserialize(JsonElement json, Type typeOfT,
                com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
            return java.time.Instant.parse(json.getAsString());
        }
    }
}
