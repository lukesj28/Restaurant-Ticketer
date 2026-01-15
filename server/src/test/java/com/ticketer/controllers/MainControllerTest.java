package com.ticketer.controllers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Collections;

import com.ticketer.models.Ticket;
import com.ticketer.models.Order;
import com.ticketer.utils.menu.dto.ComplexItem;

public class MainControllerTest {

    private MainController mainController;

    private static final String ORIGINAL_MENU_PATH = "data/menu.json";
    private static final String TEST_MENU_PATH = "target/test-main-menu.json";
    private static final String ORIGINAL_SETTINGS_PATH = "data/settings.json";
    private static final String TEST_SETTINGS_PATH = "target/test-main-settings.json";

    @Before
    public void setUp() {
        java.io.File dataDir = new java.io.File("target");
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }

        copyFile(ORIGINAL_MENU_PATH, TEST_MENU_PATH);
        copyFile(ORIGINAL_SETTINGS_PATH, TEST_SETTINGS_PATH);

        System.setProperty("menu.file", TEST_MENU_PATH);
        System.setProperty("settings.file", TEST_SETTINGS_PATH);

        mainController = new MainController();
    }

    @After
    public void tearDown() {
        deleteFile(TEST_MENU_PATH);
        deleteFile(TEST_SETTINGS_PATH);
        System.clearProperty("menu.file");
        System.clearProperty("settings.file");

        java.io.File ticketDir = new java.io.File("data/tickets");
        if (ticketDir.exists()) {
            java.io.File[] files = ticketDir.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    String today = new java.text.SimpleDateFormat("ddMMyyyy").format(new java.util.Date());
                    if (f.getName().equals(today + ".json")) {
                        f.delete();
                    }
                }
            }
        }
    }

    private void copyFile(String src, String dest) {
        java.io.File original = new java.io.File(src);
        if (original.exists()) {
            try {
                java.nio.file.Files.copy(original.toPath(), new java.io.File(dest).toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to set up test environment: " + src, e);
            }
        } else {
            try (java.io.FileWriter writer = new java.io.FileWriter(dest)) {
                writer.write("{}");
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to create default test file: " + dest, e);
            }
        }
    }

    private void deleteFile(String path) {
        try {
            java.nio.file.Files.deleteIfExists(new java.io.File(path).toPath());
        } catch (java.io.IOException e) {
            System.err.println("Failed to delete test file: " + path);
        }
    }

    @Test
    public void testInitialization() {
        assertNotNull(mainController.getMenu());
        assertNotNull(mainController.getSettings());
        assertNotNull(mainController.getActiveTickets());
    }

    @Test
    public void testMenuDelegations() {
        mainController.refreshMenu();
        assertNotNull(mainController.getAllItems());
        assertNotNull(mainController.getCategories());
    }

    @Test
    public void testItemManagementDelegations() {
        String testItem = "TestDelegationItem";
        double price = 10.0;

        try {
            mainController.removeItem(testItem);
        } catch (Exception ignored) {
        }

        mainController.addItem("Entrees", testItem, price, Collections.emptyMap());
        ComplexItem item = mainController.getItem(testItem);
        assertNotNull(item);
        assertEquals(price, item.basePrice, 0.001);

        mainController.editItemPrice(testItem, 12.0);
        assertEquals(12.0, mainController.getItem(testItem).basePrice, 0.001);

        mainController.editItemAvailability(testItem, false);
        assertFalse(mainController.getItem(testItem).available);

        mainController.removeItem(testItem);
    }

    @Test
    public void testSettingsDelegations() {
        mainController.refreshSettings();
        assertNotNull(mainController.getSettings());

        double originalTax = mainController.getTax();
        mainController.setTax(0.15);
        assertEquals(0.15, mainController.getTax(), 0.001);
        mainController.setTax(originalTax);
    }

    @Test
    public void testTicketDelegations() {
        mainController.clearAllTickets();

        Ticket t = mainController.createTicket("Table1");
        assertNotNull(t);
        assertEquals("Table1", t.getTableNumber());

        assertEquals(t, mainController.getTicket(t.getId()));

        Order o = mainController.createOrder(0.1);
        mainController.addOrderToTicket(t.getId(), o);
        assertEquals(1, t.getOrders().size());

        mainController.removeOrderFromTicket(t.getId(), o);
        assertEquals(0, t.getOrders().size());
    }

    @Test
    public void testIsOpen() {
        mainController.isOpen();
    }

    @Test
    public void testShutdown() {
        mainController.shutdown();
    }
}
