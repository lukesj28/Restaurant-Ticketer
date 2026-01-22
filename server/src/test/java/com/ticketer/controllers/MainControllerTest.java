package com.ticketer.controllers;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
import com.ticketer.models.MenuItem;
import com.ticketer.models.MenuItemView;

public class MainControllerTest {

    private MockMenuService menuService;
    private MockSettingsService settingsService;
    private MockTicketService ticketService;
    private MockRestaurantStateService restaurantStateService;
    private MainController mainController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        menuService = new MockMenuService();
        settingsService = new MockSettingsService();
        ticketService = new MockTicketService();
        restaurantStateService = new MockRestaurantStateService();
        mainController = new MainController(menuService, settingsService, ticketService, restaurantStateService);
        mockMvc = MockMvcBuilders.standaloneSetup(mainController)
                .setControllerAdvice(new com.ticketer.exceptions.GlobalExceptionHandler())
                .build();
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

        mainController.addItem(new Requests.ItemCreateRequest("Entrees", testItem, price, Collections.emptyMap()));
        assertTrue(menuService.addItemCalled);

        ApiResponse<ItemDto> response = mainController.getItem(testItem);
        assertNotNull(response.payload());
        assertEquals(price, response.payload().price());

        mainController.editItemPrice(testItem, new Requests.ItemPriceUpdateRequest(1200));
        assertTrue(menuService.editItemPriceCalled);

        mainController.editItemAvailability(testItem, new Requests.ItemAvailabilityUpdateRequest(false));
        assertTrue(menuService.editItemAvailabilityCalled);

        mainController.removeItem(testItem);
        assertTrue(menuService.removeItemCalled);
    }

    @Test
    public void testSetOpeningHours() {
        mainController.setOpeningHours("Monday", new Requests.OpeningHoursUpdateRequest("10:00 - 20:00"));
        assertTrue(restaurantStateService.checkAndScheduleStateCalled);
    }

    @Test
    public void testSettingsDelegations() {
        mainController.refreshSettings();
        ApiResponse<Double> taxResponse = mainController.getTax();
        assertEquals(0.1, taxResponse.payload(), 0.001);

        mainController.setTax(new Requests.TaxUpdateRequest(0.15));
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

        mainController.addOrderToTicket(1);
        assertTrue(ticketService.addOrderToTicketCalled);

        mainController.removeOrderFromTicket(1, 0);
        assertTrue(ticketService.removeOrderCalled);
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
        mainController.renameItem("OldName", new Requests.ItemRenameRequest("NewName"));
        mainController.renameCategory("OldCat", new Requests.CategoryRenameRequest("NewCat"));
        mainController.changeCategory("Item", new Requests.ItemCategoryUpdateRequest("NewCat"));
        mainController.updateSide("Item", "Side", new Requests.SideUpdateRequest(500));
        mainController.getCategory("Cat");

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
        ApiResponse<TicketDto> createResponse = mainController.createTicket("Table1");
        int ticketId = createResponse.payload().id();

        ApiResponse<TicketDto> addOrderResponse = mainController.addOrderToTicket(ticketId);
        assertEquals(ApiStatus.SUCCESS, addOrderResponse.status());
        assertEquals(1, addOrderResponse.payload().orders().size());

        Requests.AddItemRequest addItemRequest = new Requests.AddItemRequest("TestItem", "Fries");
        ApiResponse<TicketDto> addItemResponse = mainController.addItemToOrder(ticketId, 0, addItemRequest);
        assertEquals(ApiStatus.SUCCESS, addItemResponse.status());
        assertEquals(1, addItemResponse.payload().orders().get(0).items().size());
        assertEquals("TestItem", addItemResponse.payload().orders().get(0).items().get(0).name());

        ApiResponse<TicketDto> removeItemResponse = mainController.removeItemFromOrder(ticketId, 0, addItemRequest);
        assertEquals(ApiStatus.SUCCESS, removeItemResponse.status());
        assertTrue(removeItemResponse.payload().orders().get(0).items().isEmpty());

        ApiResponse<TicketDto> removeOrderResponse = mainController.removeOrderFromTicket(ticketId, 0);
        assertEquals(ApiStatus.SUCCESS, removeOrderResponse.status());
        assertTrue(removeOrderResponse.payload().orders().isEmpty());
    }

    @Test
    public void testOrderExceptions() throws Exception {
        Requests.AddItemRequest invalidRequest = new Requests.AddItemRequest("MissingItem", null);

        try {
            mainController.addItemToOrder(1, 0, invalidRequest);
            fail("Should have thrown EntityNotFoundException");
        } catch (com.ticketer.exceptions.EntityNotFoundException e) {

        }

        try {
            mainController.removeItemFromOrder(1, 0, new Requests.AddItemRequest("Item", null));
        } catch (Exception e) {

        }
    }

    @Test
    public void testNullHandling() throws Exception {
        MockTicketService ticket = (MockTicketService) ticketService;
        ticket.returnNull = true;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tickets/999"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value("ERROR"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value("Ticket not found"));

        ticket.returnNull = false;

        MockMenuService menu = (MockMenuService) menuService;
        menu.returnNull = true;

        ApiResponse<ItemDto> itemResponse = mainController.getItem("Missing");
        assertEquals(ApiStatus.SUCCESS, itemResponse.status());
        assertNull(itemResponse.payload());

        menu.returnNull = false;
    }

    @Test
    public void testAllExceptions() throws Exception {
        MockMenuService menu = (MockMenuService) menuService;
        menu.throwGenericException = true;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/menu/refresh"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value("ERROR"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/menu/items/Item"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value("ERROR"));

        menu.throwGenericException = false;
        menu.throwTicketerException = true;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/menu/items/Item"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value("ERROR"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value("Ticketer Error"));

        menu.throwTicketerException = false;

        MockTicketService ticket = (MockTicketService) ticketService;
        ticket.throwGenericException = true;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tickets")
                .param("tableNumber", "T1"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value("ERROR"));

        ticket.throwGenericException = false;
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
            list.add(new MenuItemView("TestItem", 1000, true));
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
            if (returnNull || "MissingItem".equals(name))
                return null;
            Map<String, Side> sides = new HashMap<>();
            Side s = new Side();
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
        boolean removeOrderCalled = false;

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
        public void addItemToOrder(int ticketId, int orderIndex, OrderItem item) {
            checkExceptions();
            Ticket t = getTicket(ticketId);
            t.getOrders().get(orderIndex).addItem(item);
        }

        @Override
        public void removeItemFromOrder(int ticketId, int orderIndex, OrderItem item) {
            checkExceptions();
            Ticket t = getTicket(ticketId);
            t.getOrders().get(orderIndex).removeItem(item);
        }

        @Override
        public void removeOrder(int id, int index) {
            checkExceptions();
            removeOrderCalled = true;
            Ticket t = getTicket(id);
            if (!t.getOrders().isEmpty()) {
                t.removeOrder(t.getOrders().get(0));
            }
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

    private static class MockRestaurantStateService extends RestaurantStateService {
        boolean checkAndScheduleStateCalled = false;

        public MockRestaurantStateService() {
            super(null, null);
        }

        @Override
        public void checkAndScheduleState() {
            checkAndScheduleStateCalled = true;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void shutdown() {
        }
    }
}
