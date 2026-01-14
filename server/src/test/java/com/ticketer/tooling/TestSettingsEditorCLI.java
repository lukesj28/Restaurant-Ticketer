package com.ticketer.tooling;

import com.ticketer.utils.settings.SettingsEditor;
import com.ticketer.utils.settings.SettingsReader;

import java.util.Scanner;

public class TestSettingsEditorCLI {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n[1] Set Tax");
                System.out.println("[2] Set Opening Hours");
                System.out.print("> ");

                String input = scanner.nextLine().trim();

                try {
                    switch (input) {
                        case "1":
                            System.out.printf("Current Tax: %.2f\n", SettingsReader.getTax());
                            double newTax = getDoubleInput(scanner, "New Tax: ");
                            SettingsEditor.setTax(newTax);
                            System.out.println("Tax updated.");
                            break;
                        case "2":
                            System.out.print("Day: ");
                            String day = scanner.nextLine().trim();

                            System.out.printf("Current Hours (%s): %s\n", day, SettingsReader.getOpeningHours(day));

                            System.out.print("New Hours (e.g. '09:00 - 17:00' or 'closed'): ");
                            String hours = scanner.nextLine().trim();

                            SettingsEditor.setOpeningHours(day, hours);
                            System.out.println("Hours updated.");
                            break;
                        default:
                            System.out.println("Invalid option.");
                    }
                } catch (com.ticketer.exceptions.StorageException e) {
                    System.err.println("IO Error: " + e.getMessage());
                } catch (RuntimeException e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }
    }

    private static double getDoubleInput(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
            }
        }
    }
}
