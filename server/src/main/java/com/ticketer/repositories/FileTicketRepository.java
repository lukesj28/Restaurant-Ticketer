package com.ticketer.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketer.models.Ticket;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class FileTicketRepository implements TicketRepository {

    private final List<Ticket> activeTickets = new CopyOnWriteArrayList<>();
    private final List<Ticket> completedTickets = new CopyOnWriteArrayList<>();
    private final List<Ticket> closedTickets = new CopyOnWriteArrayList<>();

    private final String ticketsDir;
    private final String recoveryFilePath;
    private final ObjectMapper objectMapper;
    private final Object fileLock = new Object();

    @Autowired
    public FileTicketRepository(ObjectMapper objectMapper) {
        this.ticketsDir = System.getProperty("tickets.dir", "data/tickets");
        this.recoveryFilePath = System.getProperty("recovery.file", "data/recovery.json");
        this.objectMapper = objectMapper;

        loadStateFromRecoveryFile();
    }

    public FileTicketRepository(String ticketsDir, String recoveryFilePath, ObjectMapper objectMapper) {
        this.ticketsDir = ticketsDir;
        this.recoveryFilePath = recoveryFilePath;
        this.objectMapper = objectMapper;

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
            boolean created = directory.mkdirs();
            if (!created && !directory.exists()) {
                throw new RuntimeException("Failed to create directory: " + ticketsDir);
            }
        }

        synchronized (fileLock) {
            try (FileWriter writer = new FileWriter(filename)) {
                objectMapper.writeValue(writer, closedTickets);
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
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        synchronized (fileLock) {
            try (FileWriter writer = new FileWriter(file)) {
                objectMapper.writeValue(writer, state);
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
                RecoverableState state = objectMapper.readValue(reader, RecoverableState.class);
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
        public List<Ticket> active;
        public List<Ticket> completed;

        @SuppressWarnings("unused")
        public RecoverableState() {
        }

        public RecoverableState(List<Ticket> active, List<Ticket> completed) {
            this.active = active;
            this.completed = completed;
        }
    }
}
