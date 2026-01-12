package com.ticketer.tooling;

import com.ticketer.utils.menu.MenuReader;
import com.ticketer.utils.menu.dto.*;
import com.ticketer.models.Item;
import java.util.List;
import java.util.Scanner;

public class TestMenuReaderCLI {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                try {
                    List<MenuItemView> items = MenuReader.getAllItems();
                    for (MenuItemView item : items) {
                        System.out.println(item);
                    }
                    System.out.print("Item: ");
                    String input = scanner.nextLine().trim();

                    ComplexItem item = MenuReader.getItemDetails(input);
                    if (item == null) {
                        System.out.println("Not found.");
                        continue;
                    }

                    if (!item.available) {
                        System.out.println("Unavailable.");
                        continue;
                    }

                    if (item.hasSides()) {
                        System.out.println("Selected: " + item.name);
                        for (String side : item.sideOptions.keySet()) {
                            Side opt = item.sideOptions.get(side);
                            System.out.printf(" - %s (+$%.2f)\n", side, opt.price);
                        }

                        System.out.print("Side: ");
                        String sideInput = scanner.nextLine().trim();

                        String matchedKey = null;
                        if (item.sideOptions.containsKey(sideInput)) {
                            matchedKey = sideInput;
                        }

                        if (matchedKey != null) {
                            try {
                                Item order = MenuReader.getItem(item, matchedKey);
                                System.out.println("Order: " + order);
                            } catch (IllegalArgumentException e) {
                                System.out.println("Error: " + e.getMessage());
                            }
                        } else {
                            System.out.println("Invalid side.");
                        }

                    } else {
                        Item order = MenuReader.getItem(item, null);
                        System.out.println("Order: " + order);
                    }
                } catch (java.io.IOException e) {
                    System.err.println("Error accessing menu: " + e.getMessage());
                }
            }
        }
    }
}
