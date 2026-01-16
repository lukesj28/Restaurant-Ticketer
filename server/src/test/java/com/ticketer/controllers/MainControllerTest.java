package com.ticketer.controllers;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ticketer.api.ApiResponse;
import com.ticketer.api.ApiStatus;
import com.ticketer.dtos.*;
import com.ticketer.exceptions.TicketerException;
import com.ticketer.models.*;
import com.ticketer.repositories.*;
import com.ticketer.services.*;
import com.ticketer.utils.menu.dto.MenuItem;
import com.ticketer.utils.menu.dto.MenuItemView;

public class MainControllerTest {

    private MockMenuService menuService;
    private MockSettingsService settingsService;
    private MockTicketService ticketService;
    private MainController mainController;

    @Before
    public void setUp() {
        menuService = new MockMenuService();
        settingsService = new MockSettingsService();
        ticketService = new MockTicketService();
        mainController = new MainController(menuService, settingsService, ticketService);
    }

    @Test
    public void testInitialization() {
        assertNotNull(mainController);
    }

    @Test
    public void testMenuDelegations() {
        mainController.refreshMenu();
        assertTrue(menuService.refreshMenuCalled);

        assertNotNull(mainController.getAllItems().payload());
        assertNotNull(mainController.getCategories().payload());
    }

    @Test
    public void testItemManagementDelegations() {
        String testItem = "TestItem";
        int price = 1000;

        mainController.addItem("Entrees", testItem, price, Collections.emptyMap());
        assertTrue(menuService.addItemCalled);

        ApiResponse<ItemDto> response = mainController.getItem(testItem);
        assertNotNull(response.payload());
        assertEquals(price, response.payload().price());

        mainController.editItemPrice(testItem, 1200);
        assertTrue(menuService.editItemPriceCalled);

        mainController.editItemAvailability(testItem, false);
        assertTrue(menuService.editItemAvailabilityCalled);

        mainController.removeItem(testItem);
        assertTrue(menuService.removeItemCalled);
    }

    @Test
    public void testSettingsDelegations() {
        mainController.refreshSettings();
        ApiResponse<Double> taxResponse = mainController.getTax();
        assertEquals(0.1, taxResponse.payload(), 0.001);

        mainController.setTax(0.15);
        assertTrue(settingsService.setTaxCalled);
    }

    @Test
    public void testTicketDelegations() {
        ApiResponse<TicketDto> createResponse = mainController.createTicket("Table1");
        assertNotNull(createResponse.payload());
        assertEquals("Table1", createResponse.payload().tableNumber());
        assertTrue(ticketService.createTicketCalled);

        ApiResponse<TicketDto> getResponse = mainController.getTicket(1);
        assertEquals(1, getResponse.payload().id());

        List<OrderItemDto> items = new ArrayList<>();
        items.add(new OrderItemDto("TestItem", "Fries", 1200));
        OrderDto orderDto = new OrderDto(items, 1000, 1200, 0.1);

        mainController.addOrderToTicket(1, orderDto);
        assertTrue(ticketService.addOrderToTicketCalled);

        mainController.removeOrderFromTicket(1, orderDto);
        assertTrue(ticketService.removeMatchingOrderCalled);
    }

    @Test
    public void testIsOpen() {
        mainController.isOpen();
    }

    @Test
    public void testShutdown() {
        mainController.shutdown();
    }

    @Test
    public void testAdditionalDelegations() {
        mainController.renameItem("OldName", "NewName");
        mainController.renameCategory("OldCat", "NewCat");
        mainController.changeCategory("Item", "NewCat");
        mainController.updateSide("Item", "Side", 500);
        mainController.getCategory("Cat");

        mainController.setOpeningHours("Monday", "09:00 - 17:00");
        mainController.getOpeningHours("Monday");

        mainController.moveToCompleted(1);
        mainController.moveToClosed(1);
        mainController.moveToActive(1);
        mainController.removeTicket(1);
        mainController.getActiveTickets();
        mainController.getCompletedTickets();
        mainController.getClosedTickets();
    }

    @Test
    public void testOrderManagement() {
        ApiResponse<OrderDto> createResponse = mainController.createOrder(0.1);
        assertEquals(ApiStatus.SUCCESS, createResponse.status());
        assertNotNull(createResponse.payload());
        assertEquals(0.1, createResponse.payload().taxRate(), 0.001);

        OrderDto orderDto = createResponse.payload();
        OrderItemDto itemDto = new OrderItemDto("Burger", "Fries", 1200);
        ApiResponse<OrderDto> addResponse = mainController.addItemToOrder(orderDto, itemDto);
        assertEquals(ApiStatus.SUCCESS, addResponse.status());
        assertEquals(1, addResponse.payload().items().size());
        assertEquals("Burger", addResponse.payload().items().get(0).name());

        ApiResponse<OrderDto> removeResponse = mainController.removeItemFromOrder(addResponse.payload(), itemDto);
        assertEquals(ApiStatus.SUCCESS, removeResponse.status());
        assertTrue(removeResponse.payload().items().isEmpty());
    }

    @Test
    public void testOrderExceptions() {
        ApiResponse<OrderDto> addResponse = mainController.addItemToOrder(null, null);
        assertEquals(ApiStatus.ERROR, addResponse.status());

        ApiResponse<OrderDto> removeResponse = mainController.removeItemFromOrder(null, null);
        assertEquals(ApiStatus.ERROR, removeResponse.status());
    }

    @Test
    public void testDefaultConstructor() throws java.io.IOException {
        String testDir = "target/test-default-ctor";
        java.io.File dir = new java.io.File(testDir);
        dir.mkdirs();

        java.nio.file.Files.write(java.nio.file.Paths.get(testDir, "menu.json"),
                "{}".getBytes());

        java.nio.file.Files.write(java.nio.file.Paths.get(testDir, "settings.json"),
                "{ \"tax\": 0.1, \"hours\": {} }".getBytes());

        System.setProperty("tickets.dir", testDir + "/tickets");
        System.setProperty("menu.file", testDir + "/menu.json");
        System.setProperty("settings.file", testDir + "/settings.json");

        try {
            MainController mc = new MainController();
            assertNotNull(mc);
            mc.shutdown();
        } finally {
            System.clearProperty("tickets.dir");
            System.clearProperty("menu.file");
            System.clearProperty("settings.file");

            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(testDir, "menu.json"));
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(testDir, "settings.json"));
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(testDir));
            } catch (Exception e) {
            }
        }
    }

    @Test
    public void testNullHandling() {
        MockTicketService ticket = (MockTicketService) ticketService;
        ticket.returnNull = true;
        ApiResponse<TicketDto> response = mainController.getTicket(999);
        assertEquals(ApiStatus.ERROR, response.status());
        assertEquals("Ticket not found", response.message());
        ticket.returnNull = false;

        MockMenuService menu = (MockMenuService) menuService;
        menu.returnNull = true;
        ApiResponse<ItemDto> itemResponse = mainController.getItem("Missing");
        assertEquals(ApiStatus.SUCCESS, itemResponse.status());
        assertNull(itemResponse.payload());
        menu.returnNull = false;
    }

    @Test
    public void testAllExceptions() {
        MockMenuService menu = (MockMenuService) menuService;
        menu.throwGenericException = true;

        assertEquals(ApiStatus.ERROR, mainController.refreshMenu().status());
        assertEquals(ApiStatus.ERROR, mainController.getItem("Item").status());
        assertEquals(ApiStatus.ERROR, mainController.getCategory("Cat").status());
        assertEquals(ApiStatus.ERROR, mainController.getAllItems().status());
        assertEquals(ApiStatus.ERROR, mainController.getCategories().status());
        assertEquals(ApiStatus.ERROR, mainController.addItem("C", "N", 100, null).status());
        assertEquals(ApiStatus.ERROR, mainController.editItemPrice("Item", 100).status());
        assertEquals(ApiStatus.ERROR, mainController.editItemAvailability("Item", true).status());
        assertEquals(ApiStatus.ERROR, mainController.renameItem("Old", "New").status());
        assertEquals(ApiStatus.ERROR, mainController.removeItem("Item").status());
        assertEquals(ApiStatus.ERROR, mainController.renameCategory("Old", "New").status());
        assertEquals(ApiStatus.ERROR, mainController.changeCategory("Item", "New").status());
        assertEquals(ApiStatus.ERROR, mainController.updateSide("Item", "Side", 100).status());

        menu.throwGenericException = false;
        menu.throwTicketerException = true;

        assertEquals(ApiStatus.ERROR, mainController.getItem("Item").status());
        assertEquals(ApiStatus.ERROR, mainController.getCategory("Cat").status());
        assertEquals(ApiStatus.ERROR, mainController.addItem("C", "N", 100, null).status());
        assertEquals(ApiStatus.ERROR, mainController.editItemPrice("Item", 100).status());
        assertEquals(ApiStatus.ERROR, mainController.editItemAvailability("Item", true).status());
        assertEquals(ApiStatus.ERROR, mainController.renameItem("Old", "New").status());
        assertEquals(ApiStatus.ERROR, mainController.removeItem("Item").status());
        assertEquals(ApiStatus.ERROR, mainController.renameCategory("Old", "New").status());
        assertEquals(ApiStatus.ERROR, mainController.changeCategory("Item", "New").status());
        assertEquals(ApiStatus.ERROR, mainController.updateSide("Item", "Side", 100).status());

        MockSettingsService settings = (MockSettingsService) settingsService;
        settings.throwGenericException = true;

        assertEquals(ApiStatus.ERROR, mainController.refreshSettings().status());
        assertEquals(ApiStatus.ERROR, mainController.getTax().status());
        assertEquals(ApiStatus.ERROR, mainController.getOpeningHours().status());
        assertEquals(ApiStatus.ERROR, mainController.getOpeningHours("Mon").status());
        assertEquals(ApiStatus.ERROR, mainController.getOpenTime("Mon").status());
        assertEquals(ApiStatus.ERROR, mainController.getCloseTime("Mon").status());
        assertEquals(ApiStatus.ERROR, mainController.setTax(0.5).status());

        settings.throwGenericException = false;
        settings.throwTicketerException = true;
        assertEquals(ApiStatus.ERROR, mainController.setTax(0.5).status());

        MockTicketService ticket = (MockTicketService) ticketService;
        ticket.throwGenericException = true;

        assertEquals(ApiStatus.ERROR, mainController.createTicket("T1").status());
        assertEquals(ApiStatus.ERROR, mainController.getTicket(1).status());
        assertEquals(ApiStatus.ERROR,
                mainController.addOrderToTicket(1, new OrderDto(Collections.emptyList(), 0, 0, 0)).status());
        assertEquals(ApiStatus.ERROR,
                mainController.removeOrderFromTicket(1, new OrderDto(Collections.emptyList(), 0, 0, 0)).status());
        assertEquals(ApiStatus.ERROR, mainController.getActiveTickets().status());
        assertEquals(ApiStatus.ERROR, mainController.getCompletedTickets().status());
        assertEquals(ApiStatus.ERROR, mainController.getClosedTickets().status());

        ticket.throwGenericException = false;
        ticket.throwTicketerException = true;

        assertEquals(ApiStatus.ERROR,
                mainController.addOrderToTicket(1, new OrderDto(Collections.emptyList(), 0, 0, 0)).status());
        assertEquals(ApiStatus.ERROR,
                mainController.removeOrderFromTicket(1, new OrderDto(Collections.emptyList(), 0, 0, 0)).status());
        assertEquals(ApiStatus.ERROR, mainController.moveToCompleted(1).status());
        assertEquals(ApiStatus.ERROR, mainController.moveToClosed(1).status());
        assertEquals(ApiStatus.ERROR, mainController.moveToActive(1).status());
        assertEquals(ApiStatus.ERROR, mainController.removeTicket(1).status());
    }

    private static class FakeMenuRepository implements MenuRepository {
        @Override
        public Menu getMenu() {
            return new Menu(new HashMap<>());
        }

        @Override
        public void saveMenu(Menu menu) {
        }
    }

    private static class MockMenuService extends MenuService {
        boolean refreshMenuCalled = false;
        boolean addItemCalled = false;
        boolean editItemPriceCalled = false;
        boolean editItemAvailabilityCalled = false;
        boolean removeItemCalled = false;

        boolean throwGenericException = false;
        boolean throwTicketerException = false;
        boolean returnNull = false;

        public MockMenuService() {
            super(new FakeMenuRepository());
        }

        private void checkExceptions() {
            if (throwTicketerException)
                throw new TicketerException("Ticketer Error", 400);
            if (throwGenericException)
                throw new RuntimeException("Generic Error");
        }

        @Override
        public void refreshMenu() {
            checkExceptions();
            refreshMenuCalled = true;
        }

        @Override
        public List<MenuItemView> getAllItems() {
            checkExceptions();
            if (returnNull)
                return null;
            List<MenuItemView> list = new ArrayList<>();
            list.add(new MenuItemView("TestItem", 1000, true, "Entrees"));
            return list;
        }

        @Override
        public Map<String, List<MenuItem>> getCategories() {
            checkExceptions();
            if (returnNull)
                return null;
            Map<String, List<MenuItem>> map = new HashMap<>();
            List<MenuItem> items = new ArrayList<>();
            items.add(getItem("TestItem"));
            map.put("Entrees", items);
            return map;
        }

        @Override
        public MenuItem getItem(String name) {
            checkExceptions();
            if (returnNull)
                return null;
            Map<String, com.ticketer.utils.menu.dto.Side> sides = new HashMap<>();
            com.ticketer.utils.menu.dto.Side s = new com.ticketer.utils.menu.dto.Side();
            s.price = 200;
            s.available = true;
            sides.put("Fries", s);
            return new MenuItem(name, 1000, true, sides);
        }

        @Override
        public String getCategoryOfItem(String name) {
            checkExceptions();
            return "Entrees";
        }

        @Override
        public void addItem(String c, String n, int p, Map<String, Integer> s) {
            checkExceptions();
            addItemCalled = true;
        }

        @Override
        public void editItemPrice(String n, int p) {
            checkExceptions();
            editItemPriceCalled = true;
        }

        @Override
        public void editItemAvailability(String n, boolean a) {
            checkExceptions();
            editItemAvailabilityCalled = true;
        }

        @Override
        public void removeItem(String n) {
            checkExceptions();
            removeItemCalled = true;
        }

        @Override
        public List<MenuItem> getCategory(String categoryName) {
            checkExceptions();
            List<MenuItem> items = new ArrayList<>();
            items.add(getItem("TestItem"));
            return items;
        }

        @Override
        public void renameItem(String o, String n) {
            checkExceptions();
        }

        @Override
        public void renameCategory(String o, String n) {
            checkExceptions();
        }

        @Override
        public void changeCategory(String n, String c) {
            checkExceptions();
        }

        @Override
        public void updateSide(String i, String s, int p) {
            checkExceptions();
        }
    }

    private static class FakeSettingsRepository implements SettingsRepository {
        @Override
        public Settings getSettings() {
            return new Settings(0.1, new HashMap<>());
        }

        @Override
        public void saveSettings(Settings settings) {
        }
    }

    private static class MockSettingsService extends SettingsService {
        boolean setTaxCalled = false;
        boolean throwGenericException = false;
        boolean throwTicketerException = false;

        public MockSettingsService() {
            super(new FakeSettingsRepository());
        }

        private void checkExceptions() {
            if (throwTicketerException)
                throw new TicketerException("Ticketer Error", 400);
            if (throwGenericException)
                throw new RuntimeException("Generic Error");
        }

        @Override
        public String getOpenTime(String day) {
            checkExceptions();
            return "09:00";
        }

        @Override
        public String getCloseTime(String day) {
            checkExceptions();
            return "22:00";
        }

        @Override
        public Settings getSettings() {
            checkExceptions();
            Map<String, String> hours = new HashMap<>();
            hours.put("monday", "09:00 - 22:00");
            return new Settings(0.1, hours);
        }

        @Override
        public double getTax() {
            checkExceptions();
            return 0.1;
        }

        @Override
        public void setTax(double tax) {
            checkExceptions();
            setTaxCalled = true;
        }

        @Override
        public Map<String, String> getAllOpeningHours() {
            checkExceptions();
            Map<String, String> hours = new HashMap<>();
            hours.put("monday", "09:00 - 22:00");
            return hours;
        }

        @Override
        public String getOpeningHours(String day) {
            checkExceptions();
            return "09:00-22:00";
        }
    }

    private static class FakeTicketRepository implements TicketRepository {
        @Override
        public Ticket save(Ticket ticket) {
            return ticket;
        }

        @Override
        public Optional<Ticket> findById(int id) {
            return Optional.empty();
        }

        @Override
        public List<Ticket> findAllActive() {
            return Collections.emptyList();
        }

        @Override
        public List<Ticket> findAllCompleted() {
            return Collections.emptyList();
        }

        @Override
        public List<Ticket> findAllClosed() {
            return Collections.emptyList();
        }

        @Override
        public boolean deleteById(int id) {
            return true;
        }

        @Override
        public void deleteAll() {
        }

        @Override
        public void persistClosedTickets() {
        }

        @Override
        public void moveToCompleted(int id) {
        }

        @Override
        public void moveToClosed(int id) {
        }

        @Override
        public void moveToActive(int id) {
        }
    }

    private static class MockTicketService extends TicketService {
        boolean createTicketCalled = false;
        boolean addOrderToTicketCalled = false;
        boolean removeMatchingOrderCalled = false;

        boolean throwGenericException = false;
        boolean throwTicketerException = false;
        boolean returnNull = false;

        private Map<Integer, Ticket> tickets = new HashMap<>();

        public MockTicketService() {
            super(new FakeTicketRepository());
        }

        private void checkExceptions() {
            if (throwTicketerException)
                throw new TicketerException("Ticketer Error", 400);
            if (throwGenericException)
                throw new RuntimeException("Generic Error");
        }

        @Override
        public Ticket createTicket(String tableNumber) {
            checkExceptions();
            createTicketCalled = true;
            if (returnNull)
                return null;
            Ticket t = new Ticket(1);
            t.setTableNumber(tableNumber);
            tickets.put(1, t);
            return t;
        }

        @Override
        public Ticket getTicket(int id) {
            checkExceptions();
            if (returnNull)
                return null;
            return tickets.computeIfAbsent(id, k -> {
                Ticket t = new Ticket(k);
                t.setTableNumber("Table" + k);
                return t;
            });
        }

        @Override
        public void addOrderToTicket(int id, Order order) {
            checkExceptions();
            addOrderToTicketCalled = true;
            Ticket t = getTicket(id);
            t.addOrder(order);
        }

        @Override
        public void removeMatchingOrder(int id, Order order) {
            checkExceptions();
            removeMatchingOrderCalled = true;
            Ticket t = getTicket(id);
            t.getOrders().clear();
        }

        @Override
        public List<Ticket> getActiveTickets() {
            checkExceptions();
            List<Ticket> list = new ArrayList<>();
            list.add(getTicket(1));
            return list;
        }

        @Override
        public List<Ticket> getCompletedTickets() {
            checkExceptions();
            List<Ticket> list = new ArrayList<>();
            list.add(getTicket(2));
            return list;
        }

        @Override
        public List<Ticket> getClosedTickets() {
            checkExceptions();
            List<Ticket> list = new ArrayList<>();
            list.add(getTicket(3));
            return list;
        }

        @Override
        public void moveToCompleted(int id) {
            checkExceptions();
        }

        @Override
        public void moveToClosed(int id) {
            checkExceptions();
        }

        @Override
        public void moveToActive(int id) {
            checkExceptions();
        }

        @Override
        public void removeTicket(int id) {
            checkExceptions();
        }
    }
}
