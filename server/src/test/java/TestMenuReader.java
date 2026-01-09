import utils.menu.MenuReader;
import utils.menu.dto.*;
import models.Item;
import java.util.List;
import java.util.Scanner;

public class TestMenuReader {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
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

                // Case-insensitive match
                String matchedKey = null;
                for (String key : item.sideOptions.keySet()) {
                    if (key.equalsIgnoreCase(sideInput)) {
                        matchedKey = key;
                        break;
                    }
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
        }
    }
}
