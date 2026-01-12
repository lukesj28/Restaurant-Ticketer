package com.ticketer.tooling;

import com.ticketer.utils.settings.SettingsReader;
import java.util.Map;
import java.util.Scanner;

public class TestSettingsReaderCLI {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n[1] View Tax");
                System.out.println("[2] View All Hours");
                System.out.println("[3] View Open Time");
                System.out.println("[4] View Close Time");
                System.out.print("> ");

                String input = scanner.nextLine().trim();

                try {
                    switch (input) {
                        case "1":
                            System.out.println("Tax: " + SettingsReader.getTax());
                            break;
                        case "2":
                            Map<String, String> hours = SettingsReader.getOpeningHours();
                            if (hours.isEmpty()) {
                                System.out.println("No hours defined or closed.");
                            } else {
                                hours.forEach((day, time) -> System.out.println(day + ": " + time));
                            }
                            break;
                        case "3":
                            System.out.print("Day: ");
                            String openDay = scanner.nextLine().trim();
                            String openTime = SettingsReader.getOpenTime(openDay);
                            System.out.println(openTime != null ? "Open: " + openTime : "Closed or Not Found");
                            break;
                        case "4":
                            System.out.print("Day: ");
                            String closeDay = scanner.nextLine().trim();
                            String closeTime = SettingsReader.getCloseTime(closeDay);
                            System.out.println(closeTime != null ? "Close: " + closeTime : "Closed or Not Found");
                            break;
                        default:
                            System.out.println("Invalid option.");
                    }
                } catch (java.io.IOException e) {
                    System.err.println("Error accessing settings: " + e.getMessage());
                }
            }
        }
    }
}
