package com.ticketer.services;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.ticketer.exceptions.ActionNotAllowedException;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.PrinterException;
import com.ticketer.models.Order;
import com.ticketer.models.OrderItem;
import com.ticketer.models.Ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PrintService {

    private static final Logger logger = LoggerFactory.getLogger(PrintService.class);
    private static final int RECEIPT_WIDTH = 42;

    private final TicketService ticketService;
    private final SerialPortManager serialPortManager;

    @Autowired
    public PrintService(TicketService ticketService, SerialPortManager serialPortManager) {
        this.ticketService = ticketService;
        this.serialPortManager = serialPortManager;
    }

    public void printTicket(int ticketId) {
        logger.info("Attempting to print ticket {}", ticketId);

        Ticket ticket = ticketService.getTicket(ticketId);
        if (ticket == null) {
            logger.warn("Ticket {} not found", ticketId);
            throw new EntityNotFoundException("Ticket not found: " + ticketId);
        }

        logger.info("Found ticket {} with status {}", ticketId, ticket.getStatus());

        if (!"CLOSED".equals(ticket.getStatus())) {
            logger.warn("Cannot print ticket {} - status is {} (expected CLOSED)", ticketId, ticket.getStatus());
            throw new ActionNotAllowedException("Only closed tickets can be printed");
        }

        logger.info("Getting printer output stream via SerialPortManager");

        try {
            OutputStream outputStream = serialPortManager.getOutputStream();

            try (EscPos escpos = new EscPos(outputStream)) {
                logger.info("Connected to printer at {} baud, formatting receipt for ticket {}",
                        serialPortManager.getBaudRate(), ticketId);

                printReceiptHeader(escpos);
                printTicketItems(escpos, ticket);
                printTicketTotals(escpos, ticket);
                printReceiptFooter(escpos);

                escpos.feed(4);
                escpos.cut(EscPos.CutMode.PART);

                logger.info("Successfully printed receipt for ticket {}", ticketId);
            }
        } catch (IOException e) {
            logger.error("Failed to print receipt for ticket {}: {}", ticketId, e.getMessage(), e);
            throw new PrinterException(
                    "Printer unavailable. Please check that the printer is connected and powered on.", e);
        }
    }

    public void printOrder(int ticketId, int orderIndex) {
        logger.info("Attempting to print order {} for ticket {}", orderIndex, ticketId);

        Ticket ticket = ticketService.getTicket(ticketId);
        if (ticket == null) {
            logger.warn("Ticket {} not found", ticketId);
            throw new EntityNotFoundException("Ticket not found: " + ticketId);
        }

        logger.info("Found ticket {} with status {}", ticketId, ticket.getStatus());

        if (!"CLOSED".equals(ticket.getStatus())) {
            logger.warn("Cannot print order for ticket {} - status is {} (expected CLOSED)", ticketId,
                    ticket.getStatus());
            throw new ActionNotAllowedException("Only closed tickets can be printed");
        }
        if (ticket.getOrders() == null || orderIndex < 0 || orderIndex >= ticket.getOrders().size()) {
            logger.warn("Order {} not found for ticket {}", orderIndex, ticketId);
            throw new EntityNotFoundException("Order not found: " + orderIndex);
        }

        Order order = ticket.getOrders().get(orderIndex);

        logger.info("Getting printer output stream via SerialPortManager");

        try {
            OutputStream outputStream = serialPortManager.getOutputStream();

            try (EscPos escpos = new EscPos(outputStream)) {
                logger.info("Connected to printer at {} baud, formatting order {} receipt for ticket {}",
                        serialPortManager.getBaudRate(), orderIndex, ticketId);

                printOrderHeader(escpos, orderIndex);
                printOrderItems(escpos, order);
                printOrderTotal(escpos, order);
                printReceiptFooter(escpos);

                escpos.feed(4);
                escpos.cut(EscPos.CutMode.PART);

                logger.info("Successfully printed order {} for ticket {}", orderIndex, ticketId);
            }
        } catch (IOException e) {
            logger.error("Failed to print order {} for ticket {}: {}", orderIndex, ticketId, e.getMessage(), e);
            throw new PrinterException(
                    "Printer unavailable. Please check that the printer is connected and powered on.", e);
        }
    }

    private void printReceiptHeader(EscPos escpos) throws IOException {
        Style centerStyle = new Style().setJustification(EscPosConst.Justification.Center);

        escpos.writeLF(centerStyle, repeatChar('-', RECEIPT_WIDTH));
        escpos.writeLF(centerStyle, getCurrentDateTime());
        escpos.writeLF(centerStyle, repeatChar('-', RECEIPT_WIDTH));
        escpos.feed(1);
    }

    private void printOrderHeader(EscPos escpos, int orderIndex) throws IOException {
        Style centerStyle = new Style().setJustification(EscPosConst.Justification.Center);

        escpos.writeLF(centerStyle, repeatChar('-', RECEIPT_WIDTH));
        escpos.writeLF(centerStyle, getCurrentDateTime() + " - Order #" + (orderIndex + 1));
        escpos.writeLF(centerStyle, repeatChar('-', RECEIPT_WIDTH));
        escpos.feed(1);
    }

    private void printTicketItems(EscPos escpos, Ticket ticket) throws IOException {
        for (Order order : ticket.getOrders()) {
            for (OrderItem item : order.getItems()) {
                printItem(escpos, item);
            }
        }
    }

    private void printOrderItems(EscPos escpos, Order order) throws IOException {
        for (OrderItem item : order.getItems()) {
            printItem(escpos, item);
        }
    }

    private void printItem(EscPos escpos, OrderItem item) throws IOException {
        String mainPrice = formatPrice(item.getMainPrice());
        String mainLine = formatLine(item.getName(), mainPrice);
        escpos.writeLF(mainLine);

        if (item.getSelectedSide() != null && !"none".equalsIgnoreCase(item.getSelectedSide())) {
            String sidePrice = formatPrice(item.getSidePrice());
            String sideLine = formatLine("  + " + item.getSelectedSide(), sidePrice);
            escpos.writeLF(sideLine);
        }
    }

    private void printTicketTotals(EscPos escpos, Ticket ticket) throws IOException {
        escpos.feed(1);
        escpos.writeLF(repeatChar('-', RECEIPT_WIDTH));

        String subtotalLine = formatLine("Subtotal", formatPrice(ticket.getSubtotal()));
        escpos.writeLF(subtotalLine);

        long tax = ticket.getTax();
        String taxLine = formatLine("Tax", formatPrice(tax));
        escpos.writeLF(taxLine);

        Style boldStyle = new Style().setBold(true);
        String totalLine = formatLine("TOTAL", formatPrice(ticket.getTotal()));
        escpos.writeLF(boldStyle, totalLine);

        escpos.writeLF(repeatChar('-', RECEIPT_WIDTH));
    }

    private void printOrderTotal(EscPos escpos, Order order) throws IOException {
        escpos.feed(1);
        escpos.writeLF(repeatChar('-', RECEIPT_WIDTH));

        Style boldStyle = new Style().setBold(true);
        String totalLine = formatLine("Order Total", formatPrice(order.getTotal()));
        escpos.writeLF(boldStyle, totalLine);

        escpos.writeLF(repeatChar('-', RECEIPT_WIDTH));
    }

    private void printReceiptFooter(EscPos escpos) throws IOException {
    }

    private String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return now.format(formatter);
    }

    private String formatPrice(long cents) {
        return String.format("$%.2f", cents / 100.0);
    }

    private String formatLine(String left, String right) {
        int spaces = RECEIPT_WIDTH - left.length() - right.length();
        if (spaces < 1)
            spaces = 1;
        return left + repeatChar(' ', spaces) + right;
    }

    private String repeatChar(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
