package com.ticketer.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ticketer.models.*;
import com.ticketer.services.*;
import com.ticketer.exceptions.InvalidInputException;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.GlobalExceptionHandler;

public class TicketControllerTest {

        @Mock
        private TicketService ticketService;

        @Mock
        private MenuService menuService;

        @Mock
        private SettingsService settingsService;

        @InjectMocks
        private TicketController ticketController;

        private MockMvc mockMvc;

        @BeforeEach
        public void setUp() {
                MockitoAnnotations.openMocks(this);
                mockMvc = MockMvcBuilders.standaloneSetup(ticketController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        public void testInitialization() {
                assertNotNull(ticketController);
        }

        @Test
        public void testTicketDelegations() throws Exception {
                Ticket ticket = new Ticket(1);
                ticket.setTableNumber("Table1");
                when(ticketService.createTicket("Table1")).thenReturn(ticket);
                when(ticketService.getTicket(1)).thenReturn(ticket);

                String json = "{\"tableNumber\":\"Table1\"}";
                mockMvc.perform(post("/api/tickets")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.tableNumber").value("Table1"));
                verify(ticketService).createTicket("Table1");

                mockMvc.perform(get("/api/tickets/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.id").value(1));
                verify(ticketService).getTicket(1);

                mockMvc.perform(post("/api/tickets/1/orders"))
                                .andExpect(status().isOk());
                verify(ticketService).addOrderToTicket(eq(1), any(Order.class));

                mockMvc.perform(delete("/api/tickets/1/orders/0"))
                                .andExpect(status().isOk());
                verify(ticketService).removeOrder(1, 0);
        }

        @Test
        public void testOrderManagement() throws Exception {
                Ticket ticket = new Ticket(1);
                ticket.setTableNumber("Table1");
                Order order = new Order();
                ticket.addOrder(order);

                when(ticketService.getTicket(1)).thenReturn(ticket);

                Map<String, Side> sides = new HashMap<>();
                Side fries = new Side();
                fries.price = 200;
                fries.available = true;
                sides.put("Fries", fries);
                MenuItem menuItem = new MenuItem("TestItem", 1000, true, sides, null, null, null);
                when(menuService.getItem("Entrees", "TestItem")).thenReturn(menuItem);

                when(menuService.createOrderItem("Entrees", "TestItem", "Fries", null))
                                .thenReturn(new OrderItem("test-category", "TestItem", "Fries", null, 1000, 200, 0, null));

                doAnswer(invocation -> {
                        OrderItem item = invocation.getArgument(2);
                        if (item != null) {
                                ticket.getOrders().get(0).addItem(item);
                        }
                        return null;
                }).when(ticketService).addItemToOrder(eq(1), eq(0), any(OrderItem.class), any());

                doAnswer(invocation -> {
                        ticket.getOrders().get(0).getItems().clear();
                        return null;
                }).when(ticketService).removeItemFromOrder(eq(1), eq(0), any(OrderItem.class));

                String json = "{\"category\":\"Entrees\",\"name\":\"TestItem\",\"selectedSide\":\"Fries\"}";
                mockMvc.perform(post("/api/tickets/1/orders/0/items")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.orders[0].items[0].name").value("TestItem"));

                verify(ticketService).addItemToOrder(eq(1), eq(0), any(OrderItem.class), any());

                mockMvc.perform(delete("/api/tickets/1/orders/0/items")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk());

                verify(ticketService).removeItemFromOrder(eq(1), eq(0), any(OrderItem.class));

                mockMvc.perform(delete("/api/tickets/1/orders/0"))
                                .andExpect(status().isOk());
                verify(ticketService).removeOrder(1, 0);

                Order returnedOrder = new Order();
                when(ticketService.getOrder(1, 0)).thenReturn(returnedOrder);
                mockMvc.perform(get("/api/tickets/1/orders/0"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload").exists());
                verify(ticketService).getOrder(1, 0);
        }

        @Test
        public void testOrderExceptions() throws Exception {
                when(menuService.getItem("Entrees", "MissingItem")).thenReturn(null);
                when(menuService.createOrderItem("Entrees", "MissingItem", null, null))
                                .thenThrow(new EntityNotFoundException(
                                                "Item not found: MissingItem"));

                String json = "{\"category\":\"Entrees\",\"name\":\"MissingItem\"}";
                mockMvc.perform(post("/api/tickets/1/orders/0/items")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value("ERROR"))
                                .andExpect(jsonPath("$.message").value("Item not found: MissingItem"));
        }

        @Test
        public void testNullHandling() throws Exception {
                when(ticketService.getTicket(999)).thenReturn(null);

                mockMvc.perform(get("/api/tickets/999"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value("ERROR"))
                                .andExpect(jsonPath("$.message").value("Ticket not found"));
        }

        @Test
        public void testAdditionalDelegations() throws Exception {
                Ticket t1 = new Ticket(1);
                when(ticketService.getCompletedTickets()).thenReturn(Collections.singletonList(t1));
                when(ticketService.getClosedTickets()).thenReturn(Collections.singletonList(t1));
                when(ticketService.getTicket(1)).thenReturn(t1);

                mockMvc.perform(put("/api/tickets/1/completed"))
                                .andExpect(status().isOk());
                verify(ticketService).moveToCompleted(1);

                mockMvc.perform(put("/api/tickets/1/closed"))
                                .andExpect(status().isOk());
                verify(ticketService).moveToClosed(1);

                mockMvc.perform(put("/api/tickets/1/active"))
                                .andExpect(status().isOk());
                verify(ticketService).moveToActive(1);

                mockMvc.perform(delete("/api/tickets/1"))
                                .andExpect(status().isOk());
                verify(ticketService).removeTicket(1);

                when(ticketService.getActiveTickets()).thenReturn(Collections.emptyList());
                mockMvc.perform(get("/api/tickets/active"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload").isArray());
                verify(ticketService).getActiveTickets();

                when(ticketService.getCompletedTickets()).thenReturn(Collections.emptyList());
                mockMvc.perform(get("/api/tickets/completed"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload").isArray());
                verify(ticketService, times(2)).getCompletedTickets();

                when(ticketService.getClosedTickets()).thenReturn(Collections.emptyList());
                mockMvc.perform(get("/api/tickets/closed"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload").isArray());
                verify(ticketService, times(2)).getClosedTickets();
        }

        @Test
        public void testGetTicketTally() throws Exception {
                Ticket ticket = new Ticket(1);
                Order order = new Order(1300);
                order.addItem(new OrderItem("test-category", "Burger", "Fries", null, 1000, 0, 0, null));
                ticket.addOrder(order);

                when(ticketService.getTicket(1)).thenReturn(ticket);

                mockMvc.perform(get("/api/tickets/1/tally"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.Burger").value(1))
                                .andExpect(jsonPath("$.payload.Fries").value(1));
        }

        @Test
        public void testGetTicketKitchenTally() throws Exception {
                Ticket ticket = new Ticket(1);
                Order order = new Order(1300);
                order.addItem(new OrderItem("test-category", "Burger", "Fries", null, 1000, 0, 0, null));
                order.addItem(new OrderItem("test-category", "Pizza", "None", null, 1200, 0, 0, null));
                order.addItem(new OrderItem("test-category", "Coke", "None", null, 200, 0, 0, null));
                ticket.addOrder(order);

                when(ticketService.getTicket(1)).thenReturn(ticket);

                java.util.Map<String, Integer> kitchenTally = new java.util.LinkedHashMap<>();
                kitchenTally.put("Burger", 1);
                kitchenTally.put("Pizza", 1);
                when(menuService.getKitchenTally(ticket)).thenReturn(kitchenTally);

                mockMvc.perform(get("/api/tickets/1/tally/kitchen"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.Burger").value(1))
                                .andExpect(jsonPath("$.payload.Pizza").value(1))
                                .andExpect(jsonPath("$.payload.Fries").doesNotExist())
                                .andExpect(jsonPath("$.payload.Coke").doesNotExist());
        }

        @Test
        public void testExceptions() throws Exception {
                when(ticketService.createTicket(anyString())).thenThrow(new RuntimeException("Generic Error"));

                String json = "{\"tableNumber\":\"T1\"}";
                mockMvc.perform(post("/api/tickets")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.status").value("ERROR"));
        }

        @Test
        public void testGetClosedTicketsSubtotal() throws Exception {
                when(ticketService.getClosedTicketsSubtotal()).thenReturn(5000L);

                mockMvc.perform(get("/api/tickets/closed/subtotal"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload").value(5000));

                verify(ticketService).getClosedTicketsSubtotal();
        }

        @Test
        public void testGetClosedTicketsTotal() throws Exception {
                when(ticketService.getClosedTicketsTotal()).thenReturn(5500L);

                mockMvc.perform(get("/api/tickets/closed/total"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload").value(5500));

                verify(ticketService).getClosedTicketsTotal();
        }

        @Test
        public void testGetActiveAndCompletedTicketsSubtotal() throws Exception {
                when(ticketService.getActiveAndCompletedTicketsSubtotal()).thenReturn(3000L);

                mockMvc.perform(get("/api/tickets/active-completed/subtotal"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload").value(3000));

                verify(ticketService).getActiveAndCompletedTicketsSubtotal();
        }

        @Test
        public void testGetActiveAndCompletedTicketsTotal() throws Exception {
                when(ticketService.getActiveAndCompletedTicketsTotal()).thenReturn(3300L);

                mockMvc.perform(get("/api/tickets/active-completed/total"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload").value(3300));

                verify(ticketService).getActiveAndCompletedTicketsTotal();
        }

        @Test
        public void testGetOrderNotFound() throws Exception {
                when(ticketService.getOrder(1, 0))
                                .thenThrow(new EntityNotFoundException("Order not found"));

                mockMvc.perform(get("/api/tickets/1/orders/0"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value("ERROR"))
                                .andExpect(jsonPath("$.message").value("Order not found"));
        }

        @Test
        public void testCreateTicketInvalid() throws Exception {
                doThrow(new InvalidInputException("Table number cannot be empty"))
                                .when(ticketService).createTicket(anyString());

                String json = "{\"tableNumber\":\"\"}";
                mockMvc.perform(post("/api/tickets")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest());
        }

        @Test
        public void testAddItemToOrderInvalid() throws Exception {
                doThrow(new InvalidInputException("Item name cannot be empty"))
                                .when(menuService).createOrderItem(any(), any(), any(), any());

                String json = "{\"category\":\"Entrees\",\"name\":\"\",\"selectedSide\":\"None\"}";
                mockMvc.perform(post("/api/tickets/1/orders/0/items")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest());
        }

        @Test
        public void testMoveItem() throws Exception {
                Ticket ticket = new Ticket(1);
                ticket.setTableNumber("Table1");
                Order o1 = new Order();
                o1.addItem(new OrderItem("cat", "Burger", null, null, 1000, 0, 0, null));
                Order o2 = new Order();
                o2.addItem(new OrderItem("cat", "Coke", null, null, 200, 0, 0, null));
                ticket.addOrder(o1);
                ticket.addOrder(o2);

                when(ticketService.moveItemBetweenOrders(1, 0, 0, 1)).thenReturn(ticket);
                when(ticketService.getTicket(1)).thenReturn(ticket);

                String json = "{\"targetOrderIndex\":1}";
                mockMvc.perform(put("/api/tickets/1/orders/0/items/0/move")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.id").value(1));

                verify(ticketService).moveItemBetweenOrders(1, 0, 0, 1);
        }

        @Test
        public void testMergeOrders() throws Exception {
                Ticket ticket = new Ticket(1);
                ticket.setTableNumber("Table1");
                Order o1 = new Order();
                o1.addItem(new OrderItem("cat", "Burger", null, null, 1000, 0, 0, null));
                ticket.addOrder(o1);

                when(ticketService.mergeOrders(1, 0, 1)).thenReturn(ticket);
                when(ticketService.getTicket(1)).thenReturn(ticket);

                String json = "{\"targetOrderIndex\":1}";
                mockMvc.perform(put("/api/tickets/1/orders/0/merge")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.id").value(1));

                verify(ticketService).mergeOrders(1, 0, 1);
        }
}
