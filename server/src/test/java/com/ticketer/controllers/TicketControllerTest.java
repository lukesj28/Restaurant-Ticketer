package com.ticketer.controllers;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
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

public class TicketControllerTest {

    private MockTicketService ticketService;
    private MockMenuService menuService;
    private MockSettingsService settingsService;
    private TicketController ticketController;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        ticketService = new MockTicketService();
        menuService = new MockMenuService();
        settingsService = new MockSettingsService();
        ticketController = new TicketController(ticketService, menuService, settingsService);
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController)
                .setControllerAdvice(new com.ticketer.exceptions.GlobalExceptionHandler())
                .build();
    }

    @Test
    public void testInitialization() {
        assertNotNull(ticketController);
    }

    @Test
    public void testTicketDelegations() {
        ApiResponse<TicketDto> createResponse = ticketController.createTicket("Table1");
        assertNotNull(createResponse.payload());
        assertEquals("Table1", createResponse.payload().tableNumber());
        assertTrue(ticketService.createTicketCalled);

        ApiResponse<TicketDto> getResponse = ticketController.getTicket(1);
        assertEquals(1, getResponse.payload().id());

        ticketController.addOrderToTicket(1);
        assertTrue(ticketService.addOrderToTicketCalled);

        ticketController.removeOrderFromTicket(1, 0);
        assertTrue(ticketService.removeOrderCalled);
    }

    @Test
    public void testOrderManagement() {
        ApiResponse<TicketDto> createResponse = ticketController.createTicket("Table1");
        int ticketId = createResponse.payload().id();

        ApiResponse<TicketDto> addOrderResponse = ticketController.addOrderToTicket(ticketId);
        assertEquals(ApiStatus.SUCCESS, addOrderResponse.status());
        assertEquals(1, addOrderResponse.payload().orders().size());

        Requests.AddItemRequest addItemRequest = new Requests.AddItemRequest("TestItem", "Fries");
        ApiResponse<TicketDto> addItemResponse = ticketController.addItemToOrder(ticketId, 0, addItemRequest);
        assertEquals(ApiStatus.SUCCESS, addItemResponse.status());
        assertEquals(1, addItemResponse.payload().orders().get(0).items().size());
        assertEquals("TestItem", addItemResponse.payload().orders().get(0).items().get(0).name());

        ApiResponse<TicketDto> removeItemResponse = ticketController.removeItemFromOrder(ticketId, 0, addItemRequest);
        assertEquals(ApiStatus.SUCCESS, removeItemResponse.status());
        assertTrue(removeItemResponse.payload().orders().get(0).items().isEmpty());

        ApiResponse<TicketDto> removeOrderResponse = ticketController.removeOrderFromTicket(ticketId, 0);
        assertEquals(ApiStatus.SUCCESS, removeOrderResponse.status());
        assertTrue(removeOrderResponse.payload().orders().isEmpty());
    }

    @Test
    public void testOrderExceptions() throws Exception {
        Requests.AddItemRequest invalidRequest = new Requests.AddItemRequest("MissingItem", null);

        try {
            ticketController.addItemToOrder(1, 0, invalidRequest);
            fail("Should have thrown EntityNotFoundException");
        } catch (com.ticketer.exceptions.EntityNotFoundException e) {

        }
    }

    @Test
    public void testNullHandling() throws Exception {
        ticketService.returnNull = true;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tickets/999"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value("ERROR"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value("Ticket not found"));

        ticketService.returnNull = false;
    }

    @Test
    public void testAdditionalDelegations() {
        ticketController.moveToCompleted(1);
        ticketController.moveToClosed(1);
        ticketController.moveToActive(1);
        ticketController.removeTicket(1);
        ticketController.getActiveTickets();
        ticketController.getCompletedTickets();
        ticketController.getClosedTickets();
    }

    @Test
    public void testExceptions() throws Exception {
        ticketService.throwGenericException = true;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tickets")
                .param("tableNumber", "T1"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value("ERROR"));
        ticketService.throwGenericException = false;
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
            return java.util.Collections.emptyList();
        }

        @Override
        public List<Ticket> findAllCompleted() {
            return java.util.Collections.emptyList();
        }

        @Override
        public List<Ticket> findAllClosed() {
            return java.util.Collections.emptyList();
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
        public MockMenuService() {
            super(new FakeMenuRepository());
        }

        @Override
        public MenuItem getItem(String name) {
            if ("MissingItem".equals(name))
                return null;
            Map<String, Side> sides = new HashMap<>();
            Side s = new Side();
            s.price = 200;
            s.available = true;
            sides.put("Fries", s);
            return new MenuItem(name, 1000, true, sides);
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
        public MockSettingsService() {
            super(new FakeSettingsRepository());
        }

        @Override
        public double getTax() {
            return 0.1;
        }
    }
}
