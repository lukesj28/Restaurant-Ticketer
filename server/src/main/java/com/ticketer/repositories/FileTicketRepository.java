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
import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class FileTicketRepository implements TicketRepository {

    private static final Logger logger = LoggerFactory.getLogger(FileTicketRepository.class);

    private final List<Ticket> activeTickets = new CopyOnWriteArrayList<>();
    private final List<Ticket> completedTickets = new CopyOnWriteArrayList<>();
    private final List<Ticket> closedTickets = new CopyOnWriteArrayList<>();

    private final String ticketsDir;
    private final String recoveryFilePath;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Object fileLock = new Object();

    @Autowired
    public FileTicketRepository(ObjectMapper objectMapper, Clock clock) {
        this.ticketsDir = System.getProperty("tickets.dir", "data/tickets");
        this.recoveryFilePath = System.getProperty("recovery.file", "data/recovery.json");
        this.objectMapper = objectMapper;
        this.clock = clock;

        loadStateFromRecoveryFile();
    }

    public FileTicketRepository(ObjectMapper objectMapper) {
        this(objectMapper, Clock.systemUTC());
    }

    public FileTicketRepository(String ticketsDir, String recoveryFilePath, ObjectMapper objectMapper) {
        this(ticketsDir, recoveryFilePath, objectMapper, Clock.systemUTC());
    }

    public FileTicketRepository(String ticketsDir, String recoveryFilePath, ObjectMapper objectMapper, Clock clock) {
        this.ticketsDir = ticketsDir;
        this.recoveryFilePath = recoveryFilePath;
        this.objectMapper = objectMapper;
        this.clock = clock;

        loadStateFromRecoveryFile();
    }

    @Override
    public synchronized Ticket save(Ticket ticket) {
        for (int i = 0; i < activeTickets.size(); i++) {
            if (activeTickets.get(i).getId() == ticket.getId()) {
                activeTickets.set(i, ticket);
                appendLog(new LogEntry(LogType.UPDATE, ticket));
                return ticket;
            }
        }

        for (int i = 0; i < completedTickets.size(); i++) {
            if (completedTickets.get(i).getId() == ticket.getId()) {
                completedTickets.set(i, ticket);
                appendLog(new LogEntry(LogType.UPDATE, ticket));
                return ticket;
            }
        }

        activeTickets.add(ticket);
        appendLog(new LogEntry(LogType.CREATE, ticket));
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
    public synchronized boolean deleteById(int id) {
        boolean removed = activeTickets.removeIf(t -> t.getId() == id)
                || completedTickets.removeIf(t -> t.getId() == id)
                || closedTickets.removeIf(t -> t.getId() == id);

        if (removed) {
            appendLog(new LogEntry(LogType.DELETE, id));
        }
        return removed;
    }

    @Override
    public synchronized void deleteAll() {
        activeTickets.clear();
        completedTickets.clear();
        closedTickets.clear();
        synchronized (fileLock) {
            File file = new File(recoveryFilePath);
            if (file.exists()) {
                file.delete();
            }
        }
        logger.info("Deleted all tickets and recovery file");
    }

    @Override
    public void persistClosedTickets() {
        String date = LocalDate.now(clock).toString();
        String filename = ticketsDir + "/" + date + ".json";

        File directory = new File(ticketsDir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created && !directory.exists()) {
                throw new RuntimeException("Failed to create directory: " + ticketsDir);
            }
        }

        synchronized (fileLock) {
            List<Ticket> allTickets = new java.util.ArrayList<>();
            File file = new File(filename);

            if (file.exists()) {
                try {
                    com.ticketer.models.DailyTicketLog dailyLog = objectMapper.readValue(file,
                            com.ticketer.models.DailyTicketLog.class);
                    if (dailyLog.getTickets() != null) {
                        allTickets.addAll(dailyLog.getTickets());
                    }
                } catch (IOException e) {
                    logger.warn("Failed to read existing daily log from file {}, overwriting.", filename, e);
                }
            }

            allTickets.addAll(closedTickets);

            java.util.Map<String, Integer> dailyTally = new java.util.HashMap<>();
            int dailySubtotalCents = 0;
            int dailyTotalCents = 0;

            for (Ticket ticket : allTickets) {
                java.util.Map<String, Integer> ticketTally = ticket.getTally();
                ticketTally.forEach((item, count) -> dailyTally.merge(item, count, Integer::sum));

                dailySubtotalCents += ticket.getSubtotal();
                dailyTotalCents += ticket.getTotal();
            }

            com.ticketer.models.DailyTicketLog log = new com.ticketer.models.DailyTicketLog(dailyTally, allTickets,
                    dailySubtotalCents / 100.0, dailyTotalCents / 100.0);

            try (FileWriter writer = new FileWriter(filename)) {
                objectMapper.writeValue(writer, log);
                logger.info("Persisted {} closed tickets to {} (merged with existing)", allTickets.size(), filename);
                closedTickets.clear();
            } catch (IOException e) {
                throw new RuntimeException("Failed to serialize closed tickets", e);
            }
        }
    }

    @Override
    public synchronized void moveToCompleted(int id) {
        Optional<Ticket> ticketOpt = activeTickets.stream().filter(t -> t.getId() == id).findFirst();
        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            activeTickets.remove(ticket);
            completedTickets.add(ticket);
            appendLog(new LogEntry(LogType.MOVE_COMPLETED, id));
        }
    }

    @Override
    public synchronized void moveToClosed(int id) {
        Optional<Ticket> ticketOpt = activeTickets.stream().filter(t -> t.getId() == id).findFirst();
        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            activeTickets.remove(ticket);
            ticket.setClosedAt(java.time.Instant.now(clock));
            closedTickets.add(ticket);
            appendLog(new LogEntry(LogType.MOVE_CLOSED, id));
            return;
        }
        ticketOpt = completedTickets.stream().filter(t -> t.getId() == id).findFirst();
        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            completedTickets.remove(ticket);
            ticket.setClosedAt(java.time.Instant.now(clock));
            closedTickets.add(ticket);
            appendLog(new LogEntry(LogType.MOVE_CLOSED, id));
        }
    }

    @Override
    public synchronized void moveToActive(int id) {
        Optional<Ticket> ticketOpt = completedTickets.stream().filter(t -> t.getId() == id).findFirst();
        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            completedTickets.remove(ticket);
            ticket.setClosedAt(null);
            activeTickets.add(ticket);
            appendLog(new LogEntry(LogType.MOVE_ACTIVE, id));
        }
    }

    private void appendLog(LogEntry entry) {
        File file = new File(recoveryFilePath);
        File parent = file.getParentFile();
        if (parent != null)
            parent.mkdirs();

        synchronized (fileLock) {
            try (FileWriter writer = new FileWriter(file, true)) {
                String json = objectMapper.writer()
                        .without(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
                        .writeValueAsString(entry);
                writer.write(json + "\n");
            } catch (IOException e) {
                logger.error("Failed to append recovery state", e);
            }
        }
    }

    private void loadStateFromRecoveryFile() {
        File file = new File(recoveryFilePath);
        if (!file.exists())
            return;

        synchronized (fileLock) {
            try (FileReader reader = new FileReader(file);
                    java.io.BufferedReader bufferedReader = new java.io.BufferedReader(reader)) {

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.trim().isEmpty())
                        continue;
                    try {
                        LogEntry entry = objectMapper.readValue(line, LogEntry.class);
                        replayLogEntry(entry);
                    } catch (Exception e) {
                        logger.warn("Skipping malformed or legacy log line: " + line, e);
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to load recovery state", e);
            }
        }
    }

    private void replayLogEntry(LogEntry entry) {
        if (entry.type == null)
            return;

        switch (entry.type) {
            case CREATE:
            case UPDATE:
                if (entry.ticket != null) {
                    upsertTicket(entry.ticket);
                }
                break;
            case MOVE_COMPLETED:
                moveTicketToCompleted(entry.ticketId);
                break;
            case MOVE_CLOSED:
                moveTicketToClosedReplay(entry.ticketId);
                break;
            case MOVE_ACTIVE:
                moveTicketToActiveReplay(entry.ticketId);
                break;
            case DELETE:
                deleteTicketInternal(entry.ticketId);
                break;
        }
    }

    private void upsertTicket(Ticket ticket) {
        for (int i = 0; i < activeTickets.size(); i++) {
            if (activeTickets.get(i).getId() == ticket.getId()) {
                activeTickets.set(i, ticket);
                return;
            }
        }
        for (int i = 0; i < completedTickets.size(); i++) {
            if (completedTickets.get(i).getId() == ticket.getId()) {
                completedTickets.set(i, ticket);
                return;
            }
        }
        activeTickets.add(ticket);
    }

    private void moveTicketToCompleted(int id) {
        Optional<Ticket> t = activeTickets.stream().filter(x -> x.getId() == id).findFirst();
        if (t.isPresent()) {
            activeTickets.remove(t.get());
            completedTickets.add(t.get());
        }
    }

    private void moveTicketToClosedReplay(int id) {
        Optional<Ticket> t = activeTickets.stream().filter(x -> x.getId() == id).findFirst();
        if (t.isPresent()) {
            activeTickets.remove(t.get());
            if (t.get().getClosedAt() == null) {
                t.get().setClosedAt(java.time.Instant.now(clock));
            }
            closedTickets.add(t.get());
            return;
        }
        t = completedTickets.stream().filter(x -> x.getId() == id).findFirst();
        if (t.isPresent()) {
            completedTickets.remove(t.get());
            if (t.get().getClosedAt() == null) {
                t.get().setClosedAt(java.time.Instant.now(clock));
            }
            closedTickets.add(t.get());
        }
    }

    private void moveTicketToActiveReplay(int id) {
        Optional<Ticket> t = completedTickets.stream().filter(x -> x.getId() == id).findFirst();
        if (t.isPresent()) {
            completedTickets.remove(t.get());
            t.get().setClosedAt(null);
            activeTickets.add(t.get());
        }
    }

    private void deleteTicketInternal(int id) {
        activeTickets.removeIf(t -> t.getId() == id);
        completedTickets.removeIf(t -> t.getId() == id);
        closedTickets.removeIf(t -> t.getId() == id);
    }

    private static class LogEntry {
        public LogType type;
        public Ticket ticket;
        public int ticketId;

        @SuppressWarnings("unused")
        public LogEntry() {
        }

        public LogEntry(LogType type, Ticket ticket) {
            this.type = type;
            this.ticket = ticket;
            if (ticket != null)
                this.ticketId = ticket.getId();
        }

        public LogEntry(LogType type, int ticketId) {
            this.type = type;
            this.ticketId = ticketId;
        }
    }

    private enum LogType {
        CREATE, UPDATE, MOVE_COMPLETED, MOVE_CLOSED, MOVE_ACTIVE, DELETE
    }
}
