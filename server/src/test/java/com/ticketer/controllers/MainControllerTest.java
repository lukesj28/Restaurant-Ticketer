package com.ticketer.controllers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Collections;

import com.ticketer.api.ApiResponse;
import com.ticketer.api.ApiStatus;
import com.ticketer.dtos.*;

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
        assertNotNull(mainController.getActiveTickets());
    }

    @Test
    public void testMenuDelegations() {
        mainController.refreshMenu();
        assertNotNull(mainController.getAllItems().payload());
        assertNotNull(mainController.getCategories().payload());
    }

    @Test
    public void testItemManagementDelegations() {
        String testItem = "TestDelegationItem";
        int price = 1000;

        try {
            mainController.removeItem(testItem);
        } catch (Exception ignored) {
        }

        mainController.addItem("Entrees", testItem, price, Collections.emptyMap());
        ApiResponse<ItemDto> response = mainController.getItem(testItem);
        assertNotNull(response.payload());
        assertEquals(price, response.payload().price());

        mainController.editItemPrice(testItem, 1200);
        assertEquals(1200, mainController.getItem(testItem).payload().price());

        mainController.editItemAvailability(testItem, false);
        assertFalse(mainController.getItem(testItem).payload().available());

        mainController.removeItem(testItem);
        // Verify removal by checking looking it up now returns error or null payload
        ApiResponse<ItemDto> deletedResponse = mainController.getItem(testItem);
        assertEquals(ApiStatus.ERROR, deletedResponse.status());
    }

    @Test
    public void testSettingsDelegations() {
        mainController.refreshSettings();

        Double taxPayload = mainController.getTax().payload();
        double originalTax = taxPayload != null ? taxPayload : 0.0;

        mainController.setTax(0.15);
        Double newTaxPayload = mainController.getTax().payload();
        assertNotNull(newTaxPayload);
        assertEquals(0.15, newTaxPayload, 0.001);

        mainController.setTax(originalTax);
    }

    @Test
    public void testTicketDelegations() {

        TicketDto t = mainController.createTicket("Table1").payload();
        assertNotNull(t);
        assertEquals("Table1", t.tableNumber());

        assertEquals(t.id(), mainController.getTicket(t.id()).payload().id());

        OrderDto o = mainController.createOrder(0.1).payload();
        mainController.addOrderToTicket(t.id(), o);

        t = mainController.getTicket(t.id()).payload();
        assertEquals(1, t.orders().size());

        mainController.removeOrderFromTicket(t.id(), o);
        t = mainController.getTicket(t.id()).payload();
        assertEquals(0, t.orders().size());
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
