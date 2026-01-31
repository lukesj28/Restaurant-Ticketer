package com.ticketer.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ticketer.models.*;
import com.ticketer.services.*;

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
                                .setControllerAdvice(new com.ticketer.exceptions.GlobalExceptionHandler())
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

                mockMvc.perform(post("/api/tickets").param("tableNumber", "Table1"))
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
                MenuItem menuItem = new MenuItem("TestItem", 1000, true, sides);
                when(menuService.getItem("TestItem")).thenReturn(menuItem);

                doAnswer(invocation -> {
                        OrderItem item = invocation.getArgument(2);
                        ticket.getOrders().get(0).addItem(item);
                        return null;
                }).when(ticketService).addItemToOrder(eq(1), eq(0), any(OrderItem.class));

                doAnswer(invocation -> {
                        ticket.getOrders().get(0).getItems().clear();
                        return null;
                }).when(ticketService).removeItemFromOrder(eq(1), eq(0), any(OrderItem.class));

                String json = "{\"name\":\"TestItem\",\"selectedSide\":\"Fries\"}";
                mockMvc.perform(post("/api/tickets/1/orders/0/items")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.orders[0].items[0].name").value("TestItem"));

                verify(ticketService).addItemToOrder(eq(1), eq(0), any(OrderItem.class));

                mockMvc.perform(delete("/api/tickets/1/orders/0/items")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk());

                verify(ticketService).removeItemFromOrder(eq(1), eq(0), any(OrderItem.class));

                mockMvc.perform(delete("/api/tickets/1/orders/0"))
                                .andExpect(status().isOk());
                verify(ticketService).removeOrder(1, 0);
        }

        @Test
        public void testOrderExceptions() throws Exception {
                when(menuService.getItem("MissingItem")).thenReturn(null);

                String json = "{\"name\":\"MissingItem\"}";
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
        public void testTicketTally() throws Exception {
                Ticket ticket = new Ticket(1);
                Order order = new Order();
                order.addItem(new OrderItem("Burger", "Fries", 1000));
                order.addItem(new OrderItem("Soda", null, 200));
                ticket.addOrder(order);

                when(ticketService.getTicket(1)).thenReturn(ticket);

                mockMvc.perform(get("/api/tickets/1/tally"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.payload.Burger").value(1))
                                .andExpect(jsonPath("$.payload.Fries").value(1))
                                .andExpect(jsonPath("$.payload.Soda").value(1));
        }

        @Test
        public void testExceptions() throws Exception {
                when(ticketService.createTicket(anyString())).thenThrow(new RuntimeException("Generic Error"));

                mockMvc.perform(post("/api/tickets")
                                .param("tableNumber", "T1"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.status").value("ERROR"));
        }
}
