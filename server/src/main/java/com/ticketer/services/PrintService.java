package com.ticketer.services;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.ticketer.exceptions.ActionNotAllowedException;
import com.ticketer.exceptions.EntityNotFoundException;
import com.ticketer.exceptions.PrinterException;
import com.ticketer.models.Order;
import com.ticketer.models.OrderItem;
import com.ticketer.models.Settings;
import com.ticketer.models.Ticket;
import com.ticketer.components.SerialPortManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PrintService {

    private static final Logger logger = LoggerFactory.getLogger(PrintService.class);

    private final TicketService ticketService;
    private final SettingsService settingsService;
    private final SerialPortManager serialPortManager;

    @Autowired
    public PrintService(TicketService ticketService, SettingsService settingsService,
            SerialPortManager serialPortManager) {
        this.ticketService = ticketService;
        this.settingsService = settingsService;
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

                renderReceiptBlocks(escpos, ticket);

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

        Ticket filteredTicket = new Ticket(ticket.getId());
        filteredTicket.setTableNumber(ticket.getTableNumber());
        filteredTicket.setStatus(ticket.getStatus());
        filteredTicket.setCreatedAt(ticket.getCreatedAt());
        filteredTicket.setClosedAt(ticket.getClosedAt());
        filteredTicket.setComment(ticket.getComment());
        filteredTicket.setOrders(java.util.List.of(ticket.getOrders().get(orderIndex)));
        filteredTicket.setOrderLabel("Order #" + (orderIndex + 1));
        filteredTicket.recalculatePersistedTotals();

        logger.info("Getting printer output stream via SerialPortManager");

        try {
            OutputStream outputStream = serialPortManager.getOutputStream();

            try (EscPos escpos = new EscPos(outputStream)) {
                logger.info("Connected to printer at {} baud, formatting order {} receipt for ticket {}",
                        serialPortManager.getBaudRate(), orderIndex, ticketId);

                renderReceiptBlocks(escpos, filteredTicket);

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

    public void printOrders(int ticketId, List<Integer> orderIndices) {
        logger.info("Attempting to print orders {} for ticket {}", orderIndices, ticketId);

        Ticket ticket = ticketService.getTicket(ticketId);
        if (ticket == null) {
            throw new EntityNotFoundException("Ticket not found: " + ticketId);
        }
        if (!"CLOSED".equals(ticket.getStatus())) {
            throw new ActionNotAllowedException("Only closed tickets can be printed");
        }

        List<Order> allOrders = ticket.getOrders();
        List<Order> selectedOrders = new java.util.ArrayList<>();
        for (int idx : orderIndices) {
            if (idx < 0 || idx >= allOrders.size()) {
                throw new EntityNotFoundException("Order index " + idx + " not found");
            }
            selectedOrders.add(allOrders.get(idx));
        }

        int earliestOrderNum = orderIndices.stream().mapToInt(Integer::intValue).min().orElse(0) + 1;

        Ticket filteredTicket = new Ticket(ticket.getId());
        filteredTicket.setTableNumber(ticket.getTableNumber());
        filteredTicket.setStatus(ticket.getStatus());
        filteredTicket.setCreatedAt(ticket.getCreatedAt());
        filteredTicket.setClosedAt(ticket.getClosedAt());
        filteredTicket.setComment(ticket.getComment());
        filteredTicket.setOrders(selectedOrders);
        filteredTicket.setOrderLabel("Order #" + earliestOrderNum);
        filteredTicket.recalculatePersistedTotals();

        try {
            OutputStream outputStream = serialPortManager.getOutputStream();

            try (EscPos escpos = new EscPos(outputStream)) {
                renderReceiptBlocks(escpos, filteredTicket);

                escpos.feed(4);
                escpos.cut(EscPos.CutMode.PART);

                logger.info("Successfully printed selected orders for ticket {}", ticketId);
            }
        } catch (IOException e) {
            logger.error("Failed to print orders for ticket {}: {}", ticketId, e.getMessage(), e);
            throw new PrinterException(
                    "Printer unavailable. Please check that the printer is connected and powered on.", e);
        }
    }

    public void printDailyStats(com.ticketer.dtos.DailyStatsDto stats) {
        logger.info("Attempting to print daily stats");
        logger.info("Getting printer output stream via SerialPortManager");

        try {
            OutputStream outputStream = serialPortManager.getOutputStream();

            try (EscPos escpos = new EscPos(outputStream)) {
                logger.info("Connected to printer at {} baud, formatting daily stats receipt",
                        serialPortManager.getBaudRate());
                
                int fontSize = settingsService.getReceiptSettings().getFontSize();
                int receiptWidth = getReceiptWidth(fontSize);

                printDailyStatsHeader(escpos, fontSize, receiptWidth);
                printDailyStatsBody(escpos, stats, fontSize, receiptWidth);

                escpos.feed(4);
                escpos.cut(EscPos.CutMode.PART);

                logger.info("Successfully printed daily stats");
            }
        } catch (IOException e) {
            logger.error("Failed to print daily stats: {}", e.getMessage(), e);
            throw new PrinterException(
                    "Printer unavailable. Please check that the printer is connected and powered on.", e);
        }
    }

    private void renderReceiptBlocks(EscPos escpos, Ticket ticket) throws IOException {
        Settings.ReceiptSettings receiptSettings = settingsService.getReceiptSettings();
        Settings.RestaurantDetails restaurant = settingsService.getRestaurantDetails();
        List<Settings.ReceiptBlock> blocks = receiptSettings.getBlocks();
        int fontSize = receiptSettings.getFontSize();
        int receiptWidth = getReceiptWidth(fontSize);

        for (Settings.ReceiptBlock block : blocks) {
            renderBlock(escpos, block, ticket, restaurant, fontSize, receiptWidth);
        }
    }

    private int getReceiptWidth(int size) {
        switch (size) {
            case 2: return 24;
            case 1:
            default: return 48;
        }
    }

    private Style createStyle(int size, EscPosConst.Justification justification, boolean bold) {
        Style style = new Style().setJustification(justification).setBold(bold);
        switch (size) {
            case 2:
                style.setFontName(Style.FontName.Font_A_Default);
                style.setFontSize(Style.FontSize._2, Style.FontSize._2);
                break;
            case 1:
            default:
                style.setFontName(Style.FontName.Font_A_Default);
                style.setFontSize(Style.FontSize._1, Style.FontSize._1);
                break;
        }
        return style;
    }

    private void renderBlock(EscPos escpos, Settings.ReceiptBlock block, Ticket ticket,
            Settings.RestaurantDetails restaurant, int fontSizeVal, int receiptWidth) throws IOException {

        Style leftStyle = createStyle(fontSizeVal, EscPosConst.Justification.Left_Default, false);
        Style centerStyle = createStyle(fontSizeVal, EscPosConst.Justification.Center, false);
        Style boldCenterStyle = createStyle(fontSizeVal, EscPosConst.Justification.Center, true);

        escpos.setStyle(leftStyle);
        if (fontSizeVal == 2) {
            escpos.write(new byte[]{0x1B, 0x33, 60}, 0, 3);
        } else {
            escpos.write(new byte[]{0x1B, 0x32}, 0, 2);
        }

        switch (block.getType()) {
            case "RESTAURANT_NAME":
                String name = restaurant.getName();
                if (name != null && !name.isEmpty()) {
                    escpos.writeLF(boldCenterStyle, name);
                }
                break;
            case "ADDRESS":
                String address = restaurant.getAddress();
                if (address != null && !address.isEmpty()) {
                    int commaIdx = address.indexOf(',');
                    if (commaIdx > 0) {
                        escpos.writeLF(centerStyle, address.substring(0, commaIdx).trim());
                        escpos.writeLF(centerStyle, address.substring(commaIdx + 1).trim());
                    } else {
                        escpos.writeLF(centerStyle, address);
                    }
                }
                break;
            case "PHONE":
                String phone = restaurant.getPhone();
                if (phone != null && !phone.isEmpty()) {
                    escpos.writeLF(centerStyle, phone);
                }
                break;
            case "TIMESTAMP":
                escpos.writeLF(centerStyle, getCurrentDateTime());
                break;
            case "TABLE_NUMBER":
                String tableNum = ticket.getTableNumber();
                if (tableNum != null && !tableNum.isEmpty()) {
                    escpos.writeLF("Table: " + tableNum);
                }
                String orderLabel = ticket.getOrderLabel();
                if (orderLabel != null && !orderLabel.isEmpty()) {
                    escpos.writeLF(orderLabel);
                }
                break;
            case "CUSTOM_TEXT":
                String content = block.getContent();
                if (content != null && !content.isEmpty()) {
                    escpos.writeLF(centerStyle, content);
                }
                break;
            case "DIVIDER":
                escpos.writeLF(repeatChar('-', receiptWidth));
                break;
            case "SPACE":
                escpos.feed(1);
                break;
            case "ITEMS":
                printTicketItems(escpos, ticket, fontSizeVal, receiptWidth);
                break;
            case "TOTALS":
                printTicketTotals(escpos, ticket, fontSizeVal, receiptWidth);
                break;
            default:
                logger.warn("Unknown receipt block type: {}", block.getType());
                break;
        }
    }

    private void printTicketItems(EscPos escpos, Ticket ticket, int fontSizeVal, int receiptWidth) throws IOException {
        List<OrderItem> allItems = new java.util.ArrayList<>();
        for (Order order : ticket.getOrders()) {
            allItems.addAll(order.getItems());
        }
        for (int i = 0; i < allItems.size(); i++) {
            printItem(escpos, allItems.get(i), fontSizeVal, receiptWidth);
            if (i < allItems.size() - 1) {
                escpos.write(new byte[] { 0x1B, 0x4A, 20 }, 0, 3);
            }
        }
    }

    private void printItem(EscPos escpos, OrderItem item, int fontSizeVal, int receiptWidth) throws IOException {
        Style normalStyle = createStyle(fontSizeVal, EscPosConst.Justification.Left_Default, false);
        escpos.setStyle(normalStyle);
        if (fontSizeVal == 2) {
            escpos.write(new byte[]{0x1B, 0x33, 60}, 0, 3);
        } else {
            escpos.write(new byte[]{0x1B, 0x32}, 0, 2);
        }

        String mainPrice = formatPrice(item.getMainPrice());
        for (String line : formatLineWrapped(item.getName(), mainPrice, receiptWidth)) {
            escpos.writeLF(line);
        }

        if (item.getSelectedSide() != null && !item.getSelectedSide().isEmpty()) {
            String sidePrice = formatPrice(item.getSidePrice());
            for (String line : formatLineWrapped("  + " + item.getSelectedSide(), sidePrice, receiptWidth)) {
                escpos.writeLF(line);
            }
        }

        if (item.isCombo() && item.getSlotSelections() != null) {
            for (com.ticketer.models.ComboSlotSelection sel : item.getSlotSelections()) {
                for (String line : formatLineWrapped("  + " + sel.getSelectedName(), "", receiptWidth)) {
                    escpos.writeLF(line);
                }
            }
        }
    }

    private void printTicketTotals(EscPos escpos, Ticket ticket, int fontSizeVal, int receiptWidth) throws IOException {
        Style normalStyle = createStyle(fontSizeVal, EscPosConst.Justification.Left_Default, false);
        escpos.setStyle(normalStyle);
        if (fontSizeVal == 2) {
            escpos.write(new byte[]{0x1B, 0x33, 60}, 0, 3);
        } else {
            escpos.write(new byte[]{0x1B, 0x32}, 0, 2);
        }
        for (String line : formatLineWrapped("Subtotal", formatPrice(ticket.getSubtotal()), receiptWidth)) {
            escpos.writeLF(line);
        }

        long tax = ticket.getTax();
        for (String line : formatLineWrapped("Tax", formatPrice(tax), receiptWidth)) {
            escpos.writeLF(line);
        }

        Style boldStyle = createStyle(fontSizeVal, EscPosConst.Justification.Left_Default, true);
        escpos.setStyle(boldStyle);
        if (fontSizeVal == 2) {
            escpos.write(new byte[]{0x1B, 0x33, 60}, 0, 3);
        } else {
            escpos.write(new byte[]{0x1B, 0x32}, 0, 2);
        }
        for (String line : formatLineWrapped("TOTAL", formatPrice(ticket.getTotal()), receiptWidth)) {
            escpos.writeLF(line);
        }
    }

    private String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return now.format(formatter);
    }

    private String formatPrice(long cents) {
        return String.format("$%.2f", cents / 100.0);
    }

    private List<String> formatLineWrapped(String left, String right, int receiptWidth) {
        List<String> lines = new java.util.ArrayList<>();
        if (left == null) left = "";
        if (right == null) right = "";
        
        String[] words = left.split(" ");
        int priceSpace = right.length();
        int firstLineAvail = receiptWidth - priceSpace;
        if (firstLineAvail <= 0) firstLineAvail = 1;

        StringBuilder currentLine = new StringBuilder();
        int wordIdx = 0;

        while (wordIdx < words.length) {
            String word = words[wordIdx];
            if (currentLine.length() == 0) {
                if (word.length() > firstLineAvail) {
                    currentLine.append(word.substring(0, firstLineAvail));
                    words[wordIdx] = word.substring(firstLineAvail);
                    break;
                } else {
                    currentLine.append(word);
                    wordIdx++;
                }
            } else {
                if (currentLine.length() + 1 + word.length() <= firstLineAvail) {
                    currentLine.append(" ").append(word);
                    wordIdx++;
                } else {
                    break;
                }
            }
        }
        
        int spaces = receiptWidth - currentLine.length() - priceSpace;
        if (spaces < 0) spaces = 0;
        
        if (currentLine.length() == 0) {
            lines.add(repeatChar(' ', receiptWidth - priceSpace) + right);
        } else {
            lines.add(currentLine.toString() + repeatChar(' ', spaces) + right);
        }

        currentLine = new StringBuilder();
        while (wordIdx < words.length) {
            String word = words[wordIdx];
            if (word.isEmpty()) {
                wordIdx++;
                continue;
            }
            if (currentLine.length() == 0) {
                if (word.length() > receiptWidth) {
                    lines.add(word.substring(0, receiptWidth));
                    words[wordIdx] = word.substring(receiptWidth);
                } else {
                    currentLine.append(word);
                    wordIdx++;
                }
            } else {
                if (currentLine.length() + 1 + word.length() <= receiptWidth) {
                    currentLine.append(" ").append(word);
                    wordIdx++;
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private void printDailyStatsHeader(EscPos escpos, int fontSizeVal, int receiptWidth) throws IOException {
        Style centerStyle = createStyle(fontSizeVal, EscPosConst.Justification.Center, false);
        Style boldCenterStyle = createStyle(fontSizeVal, EscPosConst.Justification.Center, true);

        escpos.writeLF(centerStyle, repeatChar('=', receiptWidth));
        escpos.writeLF(boldCenterStyle, "DAILY SALES REPORT");
        escpos.writeLF(centerStyle, getCurrentDateTime());
        escpos.writeLF(centerStyle, repeatChar('=', receiptWidth));
        escpos.feed(1);
    }

    private void printDailyStatsBody(EscPos escpos, com.ticketer.dtos.DailyStatsDto stats, int fontSizeVal, int receiptWidth) throws IOException {
        Style normalStyle = createStyle(fontSizeVal, EscPosConst.Justification.Left_Default, false);
        escpos.setStyle(normalStyle);
        if (fontSizeVal == 2) {
            escpos.write(new byte[]{0x1B, 0x33, 60}, 0, 3);
        } else {
            escpos.write(new byte[]{0x1B, 0x32}, 0, 2);
        }
        
        for (String line : formatLineWrapped("Subtotal:", formatPrice(stats.getSubtotal()), receiptWidth)) {
            escpos.writeLF(line);
        }
        for (String line : formatLineWrapped("Tax:", formatPrice(stats.getTotalTax()), receiptWidth)) {
            escpos.writeLF(line);
        }
        
        Style boldStyle = createStyle(fontSizeVal, EscPosConst.Justification.Left_Default, true);
        escpos.setStyle(boldStyle);
        if (fontSizeVal == 2) {
            escpos.write(new byte[]{0x1B, 0x33, 60}, 0, 3);
        }
        for (String line : formatLineWrapped("TOTAL REVENUE:", formatPrice(stats.getTotalRevenue()), receiptWidth)) {
            escpos.writeLF(line);
        }
        
        escpos.setStyle(normalStyle);
        if (fontSizeVal == 2) {
            escpos.write(new byte[]{0x1B, 0x33, 60}, 0, 3);
        }
        escpos.feed(1);
        escpos.writeLF(repeatChar('-', receiptWidth));

        for (String line : formatLineWrapped("Tot Tickets:", String.valueOf(stats.getTicketCount()), receiptWidth)) {
            escpos.writeLF(line);
        }
        for (String line : formatLineWrapped("Tot Orders:", String.valueOf(stats.getOrderCount()), receiptWidth)) {
            escpos.writeLF(normalStyle, line);
        }

        String turnoverStr = stats.getAverageTurnoverTimeMinutes() + " min";
        for (String line : formatLineWrapped("Avg Turnover:", turnoverStr, receiptWidth)) {
            escpos.writeLF(normalStyle, line);
        }

        for (String line : formatLineWrapped("Avg / Ticket:", formatPrice(stats.getAverageCostPerTicket()), receiptWidth)) {
            escpos.writeLF(normalStyle, line);
        }

        escpos.writeLF(repeatChar('-', receiptWidth));

        if (stats.getBarTotal() > 0) {
            escpos.writeLF(normalStyle, "BAR SUMMARY");
            for (String line : formatLineWrapped("Bar Subtotal:", formatPrice(stats.getBarSubtotal()), receiptWidth)) {
                escpos.writeLF(normalStyle, line);
            }
            for (String line : formatLineWrapped("Bar Tax:", formatPrice(stats.getBarTax()), receiptWidth)) {
                escpos.writeLF(normalStyle, line);
            }
            escpos.setStyle(boldStyle);
            if (fontSizeVal == 2) {
                escpos.write(new byte[]{0x1B, 0x33, 60}, 0, 3);
            }
            for (String line : formatLineWrapped("BAR TOTAL:", formatPrice(stats.getBarTotal()), receiptWidth)) {
                escpos.writeLF(line);
            }
            escpos.setStyle(normalStyle);
            if (fontSizeVal == 2) {
                escpos.write(new byte[]{0x1B, 0x33, 60}, 0, 3);
            }
            escpos.writeLF(repeatChar('-', receiptWidth));
        }
    }

    private String repeatChar(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
