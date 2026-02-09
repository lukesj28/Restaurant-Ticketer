package com.ticketer.services;

import com.ticketer.models.Ticket;
import com.ticketer.models.Order;
import com.ticketer.models.OrderItem;
import com.ticketer.repositories.TicketRepository;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.InvalidInputException;
import com.ticketer.exceptions.ActionNotAllowedException;

import java.util.List;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final Clock clock;
    private int ticketIdCounter = 0;
    private LocalDate lastTicketDate;

    @Autowired
    public TicketService(TicketRepository ticketRepository, Clock clock) {
        this.ticketRepository = ticketRepository;
        this.clock = clock;
        initializeTicketCounter();
    }

    public TicketService(TicketRepository ticketRepository) {
        this(ticketRepository, Clock.systemDefaultZone());
    }

    private void initializeTicketCounter() {
        int maxId = 0;
        for (Ticket t : ticketRepository.findAllActive()) {
            if (t.getId() > maxId)
                maxId = t.getId();
        }
        for (Ticket t : ticketRepository.findAllCompleted()) {
            if (t.getId() > maxId)
                maxId = t.getId();
        }
        for (Ticket t : ticketRepository.findAllClosed()) {
            if (t.getId() > maxId)
                maxId = t.getId();
        }

        Ticket maxTicket = getTicket(maxId);
        if (maxTicket != null) {
            LocalDate ticketDate = LocalDate.ofInstant(maxTicket.getCreatedAt(), ZoneId.systemDefault());
            LocalDate today = LocalDate.now(clock.withZone(ZoneId.systemDefault()));

            if (!ticketDate.equals(today)) {
                logger.info("Tickets from previous day (last ticket date: {}). Resetting counter and clearing tickets.",
                        ticketDate);
                serializeClosedTickets();
                clearAllTickets();
                this.lastTicketDate = today;
            } else {
                this.ticketIdCounter = maxId;
                this.lastTicketDate = today;
            }
        } else {
            this.ticketIdCounter = 0;
            this.lastTicketDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()));
        }
    }

    public Ticket createTicket(String tableNumber) {
        checkAndResetDailyCounter();
        logger.info("Creating ticket for table: {}", tableNumber);
        if (tableNumber == null || tableNumber.trim().isEmpty()) {
            throw new InvalidInputException("Table number cannot be empty");
        }
        int id = generateTicketId();
        Ticket ticket = new Ticket(id);
        ticket.setTableNumber(tableNumber);
        ticket.setStatus("ACTIVE");
        return ticketRepository.save(ticket);
    }

    private synchronized int generateTicketId() {
        return ++ticketIdCounter;
    }

    private synchronized void checkAndResetDailyCounter() {
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.systemDefault()));
        if (lastTicketDate != null && !lastTicketDate.equals(today)) {
            logger.info("New day detected (was: {}, now: {}). Resetting tickets.", lastTicketDate, today);
            serializeClosedTickets();
            clearAllTickets();
            this.lastTicketDate = today;
        } else if (lastTicketDate == null) {
            this.lastTicketDate = today;
        }
    }

    public void resetTicketCounter() {
        this.ticketIdCounter = 0;
    }

    public Ticket getTicket(int ticketId) {
        return ticketRepository.findById(ticketId).orElse(null);
    }

    public void addOrderToTicket(int ticketId, Order order) {
        logger.info("Adding order to ticket: {}", ticketId);
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            throw new EntityNotFoundException("Ticket with ID " + ticketId + " not found.");
        }

        if (ticketRepository.findAllClosed().contains(ticket)) {
            throw new ActionNotAllowedException("Cannot modify a closed ticket.");
        }

        if (ticketRepository.findAllCompleted().contains(ticket)) {
            ticketRepository.moveToActive(ticket.getId());
        }

        ticket.addOrder(order);
        ticketRepository.save(ticket);
    }

    public void addItemToOrder(int ticketId, int orderIndex, OrderItem item) {
        logger.info("Adding item {} to order {} on ticket {}", item.getName(), orderIndex, ticketId);
        if (item.getName() == null || item.getName().trim().isEmpty()) {
            throw new InvalidInputException("Item name cannot be empty");
        }

        Ticket ticket = getTicket(ticketId);
        if (ticket == null) {
            throw new EntityNotFoundException("Ticket " + ticketId + " not found");
        }
        if (orderIndex < 0 || orderIndex >= ticket.getOrders().size()) {
            throw new EntityNotFoundException("Order index " + orderIndex + " invalid");
        }
        if (ticketRepository.findAllCompleted().contains(ticket)) {
            ticketRepository.moveToActive(ticket.getId());
        }

        ticket.getOrders().get(orderIndex).addItem(item);
        ticketRepository.save(ticket);
    }

    public void removeItemFromOrder(int ticketId, int orderIndex, OrderItem item) {
        logger.info("Removing item {} from order {} on ticket {}", item.getName(), orderIndex, ticketId);
        Ticket ticket = getTicket(ticketId);
        if (ticket == null) {
            throw new EntityNotFoundException("Ticket " + ticketId + " not found");
        }
        if (orderIndex < 0 || orderIndex >= ticket.getOrders().size()) {
            throw new EntityNotFoundException("Order index " + orderIndex + " invalid");
        }
        if (!ticket.getOrders().get(orderIndex).removeItem(item)) {
            throw new EntityNotFoundException("Item not found in order " + orderIndex);
        }
        ticketRepository.save(ticket);
    }

    public void removeOrder(int ticketId, int orderIndex) {
        logger.info("Removing order {} from ticket {}", orderIndex, ticketId);
        Ticket ticket = getTicket(ticketId);
        if (ticket == null) {
            throw new EntityNotFoundException("Ticket " + ticketId + " not found");
        }
        List<Order> orders = ticket.getOrders();
        if (orderIndex < 0 || orderIndex >= orders.size()) {
            throw new EntityNotFoundException("Order index " + orderIndex + " invalid");
        }
        ticket.removeOrder(orders.get(orderIndex));
        ticketRepository.save(ticket);
    }

    public Order getOrder(int ticketId, int orderIndex) {
        Ticket ticket = getTicket(ticketId);
        if (ticket == null) {
            throw new EntityNotFoundException("Ticket " + ticketId + " not found");
        }
        List<Order> orders = ticket.getOrders();
        if (orderIndex < 0 || orderIndex >= orders.size()) {
            throw new EntityNotFoundException("Order index " + orderIndex + " invalid");
        }
        return orders.get(orderIndex);
    }

    public void moveToCompleted(int ticketId) {
        logger.info("Moving ticket {} to completed", ticketId);
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(
                () -> new EntityNotFoundException("Ticket with ID " + ticketId + " not found."));

        if (ticket.getClosedAt() != null) {
            throw new ActionNotAllowedException("Cannot move a closed ticket to completed.");
        }
        ticket.setStatus("COMPLETED");
        ticketRepository.save(ticket);
        ticketRepository.moveToCompleted(ticketId);
    }

    public void moveToClosed(int ticketId) {
        logger.info("Moving ticket {} to closed", ticketId);
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(
                () -> new EntityNotFoundException("Ticket with ID " + ticketId + " not found."));

        ticket.setStatus("CLOSED");
        ticketRepository.save(ticket);
        ticketRepository.moveToClosed(ticketId);
    }

    public void moveToActive(int ticketId) {
        logger.info("Moving ticket {} to active", ticketId);
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(
                () -> new EntityNotFoundException("Ticket with ID " + ticketId + " not found."));

        if (ticket.getClosedAt() != null) {
            throw new ActionNotAllowedException("Cannot move a closed ticket to active.");
        }
        ticket.setStatus("ACTIVE");
        ticketRepository.save(ticket);
        ticketRepository.moveToActive(ticketId);
    }

    public void removeTicket(int ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(
                () -> new EntityNotFoundException("Ticket with ID " + ticketId + " not found."));

        if ("CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            throw new ActionNotAllowedException("Cannot delete closed tickets.");
        }

        ticketRepository.deleteById(ticketId);
    }

    public void moveAllToClosed() {
        List<Ticket> active = new java.util.ArrayList<>(ticketRepository.findAllActive());
        for (Ticket t : active) {
            ticketRepository.moveToCompleted(t.getId());
            ticketRepository.moveToClosed(t.getId());
        }

        moveCompletedToClosed();
    }

    public void moveCompletedToClosed() {
        List<Ticket> completed = new java.util.ArrayList<>(ticketRepository.findAllCompleted());
        for (Ticket t : completed) {
            ticketRepository.moveToClosed(t.getId());
        }
    }

    public void forceCloseCompletedTickets() {
        if (ticketRepository instanceof com.ticketer.repositories.FileTicketRepository) {
            com.ticketer.repositories.FileTicketRepository repo = (com.ticketer.repositories.FileTicketRepository) ticketRepository;
            List<Ticket> completed = new java.util.ArrayList<>(repo.findAllCompleted());
            for (Ticket t : completed) {
                repo.moveToClosed(t.getId(), false);
            }
        } else {
            moveCompletedToClosed();
        }
    }

    public void discardActiveTickets() {
        logger.info("Discarding all active tickets.");
        List<Ticket> active = new java.util.ArrayList<>(ticketRepository.findAllActive());
        for (Ticket t : active) {
            ticketRepository.deleteById(t.getId());
        }
    }

    public void deleteRecoveryFile() {
        ticketRepository.deleteRecoveryFile();
    }

    public void serializeClosedTickets() {
        ticketRepository.persistClosedTickets();
    }

    public void clearAllTickets() {
        ticketRepository.deleteAll();
        resetTicketCounter();
    }

    public boolean areAllTicketsClosed() {
        return ticketRepository.findAllActive().isEmpty() && ticketRepository.findAllCompleted().isEmpty();
    }

    public boolean hasActiveTickets() {
        return !ticketRepository.findAllActive().isEmpty();
    }

    public List<Ticket> getActiveTickets() {
        return ticketRepository.findAllActive();
    }

    public List<Ticket> getCompletedTickets() {
        return ticketRepository.findAllCompleted();
    }

    public List<Ticket> getClosedTickets() {
        return ticketRepository.findAllClosed();
    }

    public long getClosedTicketsSubtotal() {
        return getClosedTickets().stream().mapToLong(Ticket::getSubtotal).sum();
    }

    public long getClosedTicketsTotal() {
        return getClosedTickets().stream().mapToLong(Ticket::getTotal).sum();
    }

    public long getActiveAndCompletedTicketsSubtotal() {
        List<Ticket> active = getActiveTickets();
        List<Ticket> completed = getCompletedTickets();
        return active.stream().mapToLong(Ticket::getSubtotal).sum() +
                completed.stream().mapToLong(Ticket::getSubtotal).sum();
    }

    public long getActiveAndCompletedTicketsTotal() {
        List<Ticket> active = getActiveTickets();
        List<Ticket> completed = getCompletedTickets();
        return active.stream().mapToLong(Ticket::getTotal).sum() +
                completed.stream().mapToLong(Ticket::getTotal).sum();
    }

    public void sendToKitchen(int ticketId) {
        logger.info("Sending ticket {} to kitchen", ticketId);
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(
                () -> new EntityNotFoundException("Ticket with ID " + ticketId + " not found."));
        ticketRepository.addTicketToKitchen(ticketId);
    }

    public void completeKitchenTicket(int ticketId) {
        logger.info("Completing kitchen ticket {}", ticketId);
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(
                () -> new EntityNotFoundException("Ticket with ID " + ticketId + " not found."));

        ticketRepository.removeTicketFromKitchen(ticketId);

        if (ticketRepository.findAllActive().stream().anyMatch(t -> t.getId() == ticketId)) {
            moveToCompleted(ticketId);
        }
    }

    public void removeFromKitchen(int ticketId) {
        ticketRepository.removeTicketFromKitchen(ticketId);
    }

    public List<Ticket> getKitchenTickets() {
        return ticketRepository.findAllKitchen();
    }
}
