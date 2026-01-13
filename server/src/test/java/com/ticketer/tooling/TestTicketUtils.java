package com.ticketer.tooling;

import com.ticketer.models.Item;
import com.ticketer.models.Order;
import com.ticketer.models.Ticket;
import com.ticketer.utils.menu.MenuReader;
import com.ticketer.utils.menu.dto.ComplexItem;
import com.ticketer.utils.ticket.TicketUtils;
import java.io.IOException;
import java.util.Scanner;

public class TestTicketUtils {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            int ticketId = 1;

            while (true) {
                System.out.println("New Ticket:");
                Ticket ticket = new Ticket(ticketId++, 0.1);

                System.out.print("Table Number: ");
                String tableNumber = scanner.nextLine().trim();
                ticket.setTableNumber(tableNumber);

                while (true) {
                    System.out.println("\n[1] Create New Order\n[2] Finish Ticket");
                    System.out.print("> ");
                    String action = scanner.nextLine().trim();

                    if (action.equals("2")) {
                        break;
                    } else if (!action.equals("1")) {
                        System.out.println("Invalid option.");
                        continue;
                    }

                    Order currentOrder = new Order();
                    System.out.println("--- New Order ---");

                    while (true) {
                        System.out.print("Add Item (name) [or 'done']: ");
                        String input = scanner.nextLine().trim();

                        if (input.equalsIgnoreCase("done")) {
                            break;
                        }

                        try {
                            ComplexItem itemDetails = MenuReader.getItemDetails(input);
                            if (itemDetails == null) {
                                System.out.println("Item not found in menu.");
                                continue;
                            }

                            if (!itemDetails.available) {
                                System.out.println("Item validation failed: Not available.");
                                continue;
                            }

                            String sideSelection = null;
                            if (itemDetails.hasSides()) {
                                System.out.println("Available sides: " + itemDetails.sideOptions.keySet());
                                System.out.print("Select side: ");
                                sideSelection = scanner.nextLine().trim();
                            }

                            Item item;
                            try {
                                item = MenuReader.getItem(itemDetails, sideSelection);
                            } catch (IllegalArgumentException e) {
                                System.out.println("Error adding item: " + e.getMessage());
                                continue;
                            }

                            currentOrder.addItem(item);
                            System.out.println("Item added: " + item);

                        } catch (IOException e) {
                            System.err.println("Error reading menu: " + e.getMessage());
                        }
                    }

                    if (!currentOrder.getItems().isEmpty()) {
                        ticket.addOrder(currentOrder);
                        System.out.println("Order added to ticket.");
                    } else {
                        System.out.println("Empty order discarded.");
                    }
                }

                String json = TicketUtils.serializeTicket(ticket);
                System.out.println("\nSerialized Ticket JSON:");
                System.out.println(json);

                System.out.println("\nItem Tally:");
                java.util.Map<String, Integer> counts = TicketUtils.countItems(ticket);
                for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
                    System.out.println(" - " + entry.getKey() + ": " + entry.getValue());
                }

                System.out.println("--------------------------------------------------");
            }
        }
    }
}
