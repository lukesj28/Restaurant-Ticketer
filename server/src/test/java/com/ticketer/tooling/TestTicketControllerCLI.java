package com.ticketer.tooling;

import com.ticketer.controllers.MenuController;
import com.ticketer.controllers.TicketController;
import com.ticketer.models.Item;
import com.ticketer.models.Menu;
import com.ticketer.models.Order;
import com.ticketer.models.Ticket;
import com.ticketer.utils.menu.dto.ComplexItem;

import java.util.List;
import java.util.Scanner;

public class TestTicketControllerCLI {

    private static TicketController ticketController;
    private static MenuController menuController;

    public static void main(String[] args) {
        try {
            ticketController = new TicketController();
            menuController = new MenuController();
        } catch (com.ticketer.exceptions.StorageException e) {
            System.err.println("Failed to initialize controllers: " + e.getMessage());
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n--- Ticket Controller CLI ---");
                System.out.println("[1] Create Ticket");
                System.out.println("[2] List Tickets");
                System.out.println("[3] Move Ticket");
                System.out.println("[4] Add Order to Ticket");
                System.out.println("[5] Move All to Closed");
                System.out.println("[6] Serialize Closed Tickets");
                System.out.println("[7] Clear All Tickets");
                System.out.println("[8] Remove Ticket");
                System.out.print("> ");

                String option = scanner.nextLine().trim();

                try {
                    switch (option) {
                        case "1":
                            createTicket(scanner);
                            break;
                        case "2":
                            listTickets();
                            break;
                        case "3":
                            moveTicket(scanner);
                            break;
                        case "4":
                            addOrderToTicket(scanner);
                            break;
                        case "5":
                            ticketController.moveAllToClosed();
                            System.out.println("All active and completed tickets moved to closed.");
                            break;
                        case "6":
                            ticketController.serializeClosedTickets();
                            System.out.println("Closed tickets serialized to data/tickets/");
                            break;
                        case "7":
                            ticketController.clearAllTickets();
                            System.out.println("All tickets cleared and counter reset.");
                            break;
                        case "8":
                            removeTicket(scanner);
                            break;
                        default:
                            System.out.println("Invalid option.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Error: " + e.getMessage());
                } catch (com.ticketer.exceptions.StorageException e) {
                    System.out.println("IO Error: " + e.getMessage());
                }
            }
        }
    }

    private static void createTicket(Scanner scanner) {
        System.out.print("Table Number: ");
        String table = scanner.nextLine().trim();
        Ticket t = ticketController.createTicket(table);
        System.out.println("Created Ticket #" + t.getId() + " for " + table);
    }

    private static void listTickets() {
        System.out.println("ACTIVE:");
        printTicketList(ticketController.getActiveTickets());
        System.out.println("COMPLETED:");
        printTicketList(ticketController.getCompletedTickets());
        System.out.println("CLOSED:");
        printTicketList(ticketController.getClosedTickets());
    }

    private static void printTicketList(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (Ticket t : tickets) {
                System.out.printf("  #%d [Table: %s] Total: $%.2f\n", t.getId(), t.getTableNumber(), t.getTotal());
            }
        }
    }

    private static void moveTicket(Scanner scanner) {
        System.out.print("Ticket ID: ");
        int id = getIntInput(scanner);
        System.out.println("Move to: [1] Active [2] Completed [3] Closed");
        String dest = scanner.nextLine().trim();

        if (dest.equals("1"))
            ticketController.moveToActive(id);
        else if (dest.equals("2"))
            ticketController.moveToCompleted(id);
        else if (dest.equals("3"))
            ticketController.moveToClosed(id);
        else
            System.out.println("Invalid destination.");

        System.out.println("Moved.");
    }

    private static void addOrderToTicket(Scanner scanner) {
        System.out.print("Ticket ID: ");
        int id = getIntInput(scanner);
        Ticket ticket = ticketController.getTicket(id);
        if (ticket == null) {
            System.out.println("Active ticket not found.");
            return;
        }

        Order order = ticketController.createOrder(0.1);
        System.out.println("--- Building Order ---");

        while (true) {
            System.out.print("Add Item (name) [or 'done']: ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("done"))
                break;

            ComplexItem itemDetails = menuController.getItem(input);
            if (itemDetails == null) {
                System.out.println("Item not found.");
                continue;
            }
            if (!itemDetails.available) {
                System.out.println("Unavailable.");
                continue;
            }

            String sideSelection = null;
            if (itemDetails.hasSides()) {
                System.out.println("Sides: " + itemDetails.sideOptions.keySet());
                System.out.print("Select side: ");
                sideSelection = scanner.nextLine().trim();
            }

            try {
                Item item = Menu.getItem(itemDetails, sideSelection);
                order.addItem(item);
                System.out.println("Added: " + item.getName());
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        if (!order.getItems().isEmpty()) {
            ticketController.addOrderToTicket(id, order);
            System.out.println("Order added to Ticket #" + id);
        } else {
            System.out.println("Empty order discarded.");
        }
    }

    private static void removeTicket(Scanner scanner) {
        System.out.print("Ticket ID to remove: ");
        int id = getIntInput(scanner);
        ticketController.removeTicket(id);
        System.out.println("Ticket #" + id + " removed.");
    }

    private static int getIntInput(Scanner scanner) {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
