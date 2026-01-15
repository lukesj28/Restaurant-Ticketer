package com.ticketer.controllers;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;

import com.ticketer.models.Item;
import com.ticketer.models.Ticket;
import com.ticketer.models.Order;
import com.ticketer.utils.menu.dto.ComplexItem;
import com.ticketer.utils.menu.dto.MenuItemView;

public class MainController {

    private final MenuController menuController;
    private final SettingsController settingsController;
    private final TicketController ticketController;

    private final ScheduledExecutorService scheduler;
    private boolean isOpen;

    public MainController() {
        this.menuController = new MenuController();
        this.settingsController = new SettingsController();
        this.ticketController = new TicketController();

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.isOpen = false;

        checkAndScheduleState();
    }

    private void checkAndScheduleState() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        String dayName = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toLowerCase();

        String openTimeStr = settingsController.getOpenTime(dayName);
        String closeTimeStr = settingsController.getCloseTime(dayName);

        if (openTimeStr == null || closeTimeStr == null) {
            setClosedState();
            scheduleNextDayCheck();
            return;
        }

        try {
            LocalTime openTime = LocalTime.parse(openTimeStr);
            LocalTime closeTime = LocalTime.parse(closeTimeStr);
            LocalTime now = LocalTime.now();

            if (now.isBefore(openTime)) {
                setClosedState();
                long delay = java.time.Duration.between(now, openTime).toMillis();
                scheduler.schedule(this::handleOpening, delay, TimeUnit.MILLISECONDS);
            } else if (now.isAfter(openTime) && now.isBefore(closeTime)) {
                setOpenState();
                long delay = java.time.Duration.between(now, closeTime).toMillis();
                scheduler.schedule(this::handleClosing, delay, TimeUnit.MILLISECONDS);
            } else {
                if (isOpen) {
                    handleClosing();
                } else {
                    setClosedState();
                    scheduleNextDayCheck();
                    runClosingSequence();
                }
            }

        } catch (DateTimeParseException e) {
            System.err.println("Error parsing time settings: " + e.getMessage());
            setClosedState();
            scheduleNextDayCheck();
        }
    }

    private void scheduleNextDayCheck() {
        LocalTime now = LocalTime.now();
        LocalTime midnight = LocalTime.MAX;
        long delay = java.time.Duration.between(now, midnight).toMillis() + 1000;
        scheduler.schedule(this::checkAndScheduleState, delay, TimeUnit.MILLISECONDS);
    }

    private void setOpenState() {
        this.isOpen = true;
        System.out.println("Restaurant is now OPEN.");
    }

    private void setClosedState() {
        this.isOpen = false;
        System.out.println("Restaurant is now CLOSED.");
    }

    private void handleOpening() {
        setOpenState();
        checkAndScheduleState();
    }

    private void handleClosing() {
        setClosedState();
        scheduler.execute(this::runClosingSequence);
        scheduleNextDayCheck();
    }

    private void runClosingSequence() {
        System.out.println("Starting closing sequence...");

        long startTime = System.currentTimeMillis();
        long maxWaitTime = 3600000;
        long checkInterval = 300000;

        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            if (ticketController.areAllTicketsClosed()) {
                break;
            }
            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        System.out.println("Finalizing closing sequence. Moving remaining tickets to closed.");
        ticketController.moveAllToClosed();
        ticketController.serializeClosedTickets();
        ticketController.clearAllTickets();
        System.out.println("Closing sequence completed.");
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void refreshMenu() {
        menuController.refreshMenu();
    }

    public ComplexItem getItem(String name) {
        return menuController.getItem(name);
    }

    public Item getItem(ComplexItem item, String sideName) {
        return menuController.getItem(item, sideName);
    }

    public List<ComplexItem> getCategory(String categoryName) {
        return menuController.getCategory(categoryName);
    }

    public List<MenuItemView> getAllItems() {
        return menuController.getAllItems();
    }

    public Map<String, List<ComplexItem>> getCategories() {
        return menuController.getCategories();
    }

    public void addItem(String category, String name, int price, Map<String, Integer> sides) {
        menuController.addItem(category, name, price, sides);
    }

    public void editItemPrice(String itemName, int newPrice) {
        menuController.editItemPrice(itemName, newPrice);
    }

    public void editItemAvailability(String itemName, boolean available) {
        menuController.editItemAvailability(itemName, available);
    }

    public void renameItem(String oldName, String newName) {
        menuController.renameItem(oldName, newName);
    }

    public void removeItem(String itemName) {
        menuController.removeItem(itemName);
    }

    public void renameCategory(String oldCategory, String newCategory) {
        menuController.renameCategory(oldCategory, newCategory);
    }

    public void changeCategory(String itemName, String newCategory) {
        menuController.changeCategory(itemName, newCategory);
    }

    public void updateSide(String itemName, String sideName, int newPrice) {
        menuController.updateSide(itemName, sideName, newPrice);
    }

    public void refreshSettings() {
        settingsController.refreshSettings();
    }

    public double getTax() {
        return settingsController.getTax();
    }

    public Map<String, String> getOpeningHours() {
        return settingsController.getOpeningHours();
    }

    public String getOpeningHours(String day) {
        return settingsController.getOpeningHours(day);
    }

    public String getOpenTime(String day) {
        return settingsController.getOpenTime(day);
    }

    public String getCloseTime(String day) {
        return settingsController.getCloseTime(day);
    }

    public void setTax(double tax) {
        settingsController.setTax(tax);
    }

    public void setOpeningHours(String day, String hours) {
        settingsController.setOpeningHours(day, hours);
    }

    public Ticket createTicket(String tableNumber) {
        return ticketController.createTicket(tableNumber);
    }

    public Ticket getTicket(int ticketId) {
        return ticketController.getTicket(ticketId);
    }

    public void addOrderToTicket(int ticketId, Order order) {
        ticketController.addOrderToTicket(ticketId, order);
    }

    public void removeOrderFromTicket(int ticketId, Order order) {
        ticketController.removeOrderFromTicket(ticketId, order);
    }

    public Order createOrder(double taxRate) {
        return ticketController.createOrder(taxRate);
    }

    public void addItemToOrder(Order order, Item item) {
        ticketController.addItemToOrder(order, item);
    }

    public void removeItemFromOrder(Order order, Item item) {
        ticketController.removeItemFromOrder(order, item);
    }

    public List<Ticket> getActiveTickets() {
        return ticketController.getActiveTickets();
    }

    public List<Ticket> getCompletedTickets() {
        return ticketController.getCompletedTickets();
    }

    public List<Ticket> getClosedTickets() {
        return ticketController.getClosedTickets();
    }

    public void moveToCompleted(int ticketId) {
        ticketController.moveToCompleted(ticketId);
    }

    public void moveToClosed(int ticketId) {
        ticketController.moveToClosed(ticketId);
    }

    public void moveToActive(int ticketId) {
        ticketController.moveToActive(ticketId);
    }

    public void removeTicket(int ticketId) {
        ticketController.removeTicket(ticketId);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
