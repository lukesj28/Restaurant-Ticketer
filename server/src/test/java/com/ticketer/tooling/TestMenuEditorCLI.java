package com.ticketer.tooling;

import com.ticketer.utils.menu.MenuEditor;
import com.ticketer.utils.menu.MenuReader;
import com.ticketer.utils.menu.dto.MenuItemView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class TestMenuEditorCLI {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("[1] Add\n[2] Edit");
            System.out.print("> ");
            String option = scanner.nextLine().trim();

            if (option.equals("1")) {
                addNewItem(scanner);
            } else if (option.equals("2")) {
                editExistingItem(scanner);
            } else {
                System.out.println("Invalid.");
            }
        }
    }

    private static void addNewItem(Scanner scanner) {
        System.out.print("Category: ");
        String category = scanner.nextLine().trim();

        System.out.print("Name: ");
        String name = scanner.nextLine().trim();

        int price = getIntInput(scanner, "Price (cents): ");

        Map<String, Integer> sides = new HashMap<>();
        System.out.print("Sides? (y/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            while (true) {
                System.out.print("Side name (or 'done'): ");
                String sideName = scanner.nextLine().trim();
                if (sideName.equalsIgnoreCase("done"))
                    break;
                if (sideName.isEmpty())
                    continue;

                int sidePrice = getIntInput(scanner, "Side price (cents): ");
                sides.put(sideName, sidePrice);
            }
        }

        try {
            MenuEditor.addItem(category, name, price, sides);
            System.out.println("Added.");
        } catch (com.ticketer.exceptions.StorageException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void editExistingItem(Scanner scanner) {
        List<MenuItemView> allItems;
        try {
            allItems = MenuReader.readMenu().getAllItems();
        } catch (com.ticketer.exceptions.StorageException e) {
            System.err.println("Error reading menu: " + e.getMessage());
            return;
        }

        System.out.println("Items:");
        for (MenuItemView item : allItems) {
            System.out.println(" - " + item.name + " (" + item.category + ")");
        }

        System.out.print("\nEdit item: ");
        String itemName = scanner.nextLine().trim();

        System.out.println("[1] Price\n[2] Availability\n[3] Name\n[4] Category\n[5] Side");
        System.out.print("> ");
        String editOpt = scanner.nextLine().trim();

        try {
            switch (editOpt) {
                case "1":
                    int newPrice = getIntInput(scanner, "New price (cents): ");
                    MenuEditor.editItemPrice(itemName, newPrice);
                    break;
                case "2":
                    System.out.print("Available? (t/f): ");
                    boolean avail = Boolean.parseBoolean(scanner.nextLine().trim());
                    MenuEditor.editItemAvailability(itemName, avail);
                    break;
                case "3":
                    System.out.print("New name: ");
                    String newName = scanner.nextLine().trim();
                    MenuEditor.renameItem(itemName, newName);
                    break;
                case "4":
                    System.out.print("New cat: ");
                    String newCat = scanner.nextLine().trim();
                    MenuEditor.changeCategory(itemName, newCat);
                    break;
                case "5":
                    System.out.print("Side name: ");
                    String sideName = scanner.nextLine().trim();
                    int sidePrice = getIntInput(scanner, "Side price (cents): ");
                    MenuEditor.updateSide(itemName, sideName, sidePrice);
                    break;
                default:
                    System.out.println("Invalid.");
                    return;
            }
            System.out.println("Updated.");
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (com.ticketer.exceptions.StorageException e) {
            System.err.println("IO Error: " + e.getMessage());
        }
    }

    private static int getIntInput(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid.");
            }
        }
    }
}
