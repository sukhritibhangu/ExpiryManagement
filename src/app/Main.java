package app;

import model.ExpiryStatus;
import model.Item;
import service.ExpiryService;
import service.ItemService;
import storage.CsvStorage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Expiry Alert System - Interactive Console Application.
 * 
 * Entry point that connects the UI layer to business services and storage.
 * Designed for household use and small inventories to prevent waste.
 */
public class Main {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD
    private final ItemService itemService;
    private final ExpiryService expiryService;
    private final Scanner scanner;

    public Main() {
        this.expiryService = new ExpiryService();
        this.itemService = new ItemService(new CsvStorage(), expiryService);
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.run();
    }

    /**
     * Main application loop.
     */
    public void run() {
        printWelcomeHeader();

        // Required behavior: Display expiry alerts automatically upon program startup
        displayStartupAlerts();

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = promptString("Enter your choice (1-6): ");

            System.out.println();
            switch (choice.trim()) {
                case "1" -> handleAddItem();
                case "2" -> handleViewAllItems();
                case "3" -> handleViewExpiryAlerts();
                case "4" -> handleSearchItems();
                case "5" -> handleDeleteItem();
                case "6" -> {
                    System.out.println("====================================================");
                    System.out.println("  Thank you for using Expiry Alert System. Goodbye!  ");
                    System.out.println("  All data has been safely saved to inventory.csv.  ");
                    System.out.println("====================================================");
                    running = false;
                }
                default -> System.out.println("[Error] Invalid choice! Please enter a number between 1 and 6.");
            }
            System.out.println();
        }

        scanner.close();
    }

    // =========================================================================
    // MENU HANDLERS
    // =========================================================================

    /**
     * Menu Option 1: Add a new item with thorough input validation.
     */
    private void handleAddItem() {
        System.out.println("--- Add a New Item ---");

        String name = promptNonEmptyString("Enter item name: ");
        String category = promptNonEmptyString("Enter category (e.g. Dairy, Medicine, Pantry, Bakery): ");
        int quantity = promptPositiveInt("Enter quantity: ");
        LocalDate expiryDate = promptValidDate("Enter expiry date (YYYY-MM-DD): ");
        String notes = promptOptionalString("Enter notes (optional, press Enter to skip): ");

        Item added = itemService.addItem(name, category, quantity, expiryDate, notes);
        ExpiryStatus status = expiryService.calculateStatus(added.getExpiryDate());
        long daysRemaining = expiryService.getDaysRemaining(added.getExpiryDate());

        System.out.println("\n[Success] Item added successfully!");
        System.out.printf("  ID: %d | Name: %s | Status: %s (%s)%n",
                added.getId(), added.getName(), status.getBadge(), expiryService.formatDaysRemaining(daysRemaining));
    }

    /**
     * Menu Option 2: View all inventory items in an aligned table.
     */
    private void handleViewAllItems() {
        System.out.println("--- All Inventory Items ---");
        List<Item> items = itemService.getAllItems();
        if (items.isEmpty()) {
            System.out.println("No items found. Choose option 1 to add an item.");
            return;
        }

        renderItemsTable(items);
        printInventorySummary(items);
    }

    /**
     * Menu Option 3: View only items that require attention (Expired or Expiring Soon).
     */
    private void handleViewExpiryAlerts() {
        System.out.println("--- Expiry Alerts ---");
        List<Item> alertItems = itemService.getAlertItems();

        if (alertItems.isEmpty()) {
            System.out.println("Great news! No items are expired or expiring within the next 7 days.");
            return;
        }

        System.out.println("[!] ATTENTION: The following items need your action:\n");
        renderItemsTable(alertItems);
    }

    /**
     * Menu Option 4: Search items by name or category.
     */
    private void handleSearchItems() {
        System.out.println("--- Search Inventory ---");
        String query = promptNonEmptyString("Enter item name or category to search: ");

        List<Item> results = itemService.searchItems(query);
        if (results.isEmpty()) {
            System.out.printf("No items found matching '%s'.%n", query);
        } else {
            System.out.printf("Found %d matching item(s):%n%n", results.size());
            renderItemsTable(results);
        }
    }

    /**
     * Menu Option 5: Delete an item using its ID.
     */
    private void handleDeleteItem() {
        System.out.println("--- Delete an Item ---");
        if (itemService.getTotalCount() == 0) {
            System.out.println("Inventory is empty. Nothing to delete.");
            return;
        }

        int id = promptInt("Enter the ID of the item to delete: ");
        Optional<Item> itemOpt = itemService.findById(id);

        if (itemOpt.isEmpty()) {
            System.out.printf("[Error] No item found with ID %d.%n", id);
            return;
        }

        Item item = itemOpt.get();
        System.out.printf("Are you sure you want to delete '%s' (ID: %d)? (y/n): ", item.getName(), item.getId());
        String confirm = scanner.nextLine().trim();

        if (confirm.equalsIgnoreCase("y") || confirm.equalsIgnoreCase("yes")) {
            boolean deleted = itemService.deleteItem(id);
            if (deleted) {
                System.out.printf("[Success] Item #%d ('%s') was deleted and inventory saved.%n", id, item.getName());
            } else {
                System.out.println("[Error] Failed to delete item.");
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    // =========================================================================
    // STARTUP & TABLE RENDERING HELPERS
    // =========================================================================

    /**
     * Displays a prominent alert banner at program launch if items are expired or expiring soon.
     */
    private void displayStartupAlerts() {
        List<Item> alertItems = itemService.getAlertItems();
        if (alertItems.isEmpty()) {
            System.out.println("STATUS: All inventory items are currently SAFE.\n");
            return;
        }

        long expiredCount = alertItems.stream()
                .filter(i -> expiryService.calculateStatus(i.getExpiryDate()) == ExpiryStatus.EXPIRED)
                .count();
        long expiringSoonCount = alertItems.size() - expiredCount;

        System.out.println("*******************************************************************");
        System.out.printf("  * AUTOMATIC EXPIRY ALERT: %d item(s) need immediate attention! *%n", alertItems.size());
        if (expiredCount > 0) {
            System.out.printf("  * [!] EXPIRED: %d item(s) have passed their expiration date!    *%n", expiredCount);
        }
        if (expiringSoonCount > 0) {
            System.out.printf("  * [!] EXPIRING SOON: %d item(s) expire within the next 7 days!  *%n", expiringSoonCount);
        }
        System.out.println("*******************************************************************\n");
    }

    /**
     * Renders a neatly aligned ASCII table for a list of items.
     */
    private void renderItemsTable(List<Item> items) {
        String line = "+-----+----------------------+------------+-----+------------+----------------------+------------------+---------------------------+";
        System.out.println(line);
        System.out.printf("| %-3s | %-20s | %-10s | %-3s | %-10s | %-20s | %-16s | %-25s |%n",
                "ID", "Name", "Category", "Qty", "Expiry", "Days Left", "Status", "Notes");
        System.out.println(line);

        LocalDate today = LocalDate.now();
        for (Item item : items) {
            ExpiryStatus status = expiryService.calculateStatus(item.getExpiryDate(), today);
            long days = expiryService.getDaysRemaining(item.getExpiryDate(), today);
            String formattedDays = expiryService.formatDaysRemaining(days);

            String name = truncate(item.getName(), 20);
            String cat = truncate(item.getCategory(), 10);
            String notes = truncate(item.getNotes(), 25);

            System.out.printf("| %-3d | %-20s | %-10s | %-3d | %-10s | %-20s | %-16s | %-25s |%n",
                    item.getId(),
                    name,
                    cat,
                    item.getQuantity(),
                    item.getExpiryDate(),
                    formattedDays,
                    status.getBadge(),
                    notes);
        }
        System.out.println(line);
    }

    /**
     * Prints inventory breakdown statistics.
     */
    private void printInventorySummary(List<Item> items) {
        int expired = 0;
        int expiringSoon = 0;
        int safe = 0;

        LocalDate today = LocalDate.now();
        for (Item item : items) {
            ExpiryStatus status = expiryService.calculateStatus(item.getExpiryDate(), today);
            switch (status) {
                case EXPIRED -> expired++;
                case EXPIRING_SOON -> expiringSoon++;
                case SAFE -> safe++;
            }
        }

        System.out.printf("Summary: Total: %d | Expired: %d | Expiring Soon (<= 7 days): %d | Safe: %d%n",
                items.size(), expired, expiringSoon, safe);
    }

    private void printWelcomeHeader() {
        System.out.println("===================================================================");
        System.out.println("                    EXPIRY ALERT SYSTEM                            ");
        System.out.println("        Smart Household & Small Inventory Expiry Tracker           ");
        System.out.println("===================================================================");
    }

    private void printMainMenu() {
        System.out.println("-------------------------------------------------------------------");
        System.out.println(" 1. Add an item");
        System.out.println(" 2. View all items");
        System.out.println(" 3. View expiry alerts");
        System.out.println(" 4. Search items");
        System.out.println(" 5. Delete an item");
        System.out.println(" 6. Exit");
        System.out.println("-------------------------------------------------------------------");
    }

    // =========================================================================
    // INPUT VALIDATION HELPERS
    // =========================================================================

    private String promptString(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    private String promptNonEmptyString(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("[Error] Field cannot be blank. Please enter a valid value.");
        }
    }

    private String promptOptionalString(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    private int promptPositiveInt(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value > 0) {
                    return value;
                }
                System.out.println("[Error] Quantity must be a positive number greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("[Error] Invalid integer! Please enter a whole number (e.g., 1, 5, 12).");
            }
        }
    }

    private int promptInt(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("[Error] Invalid number! Please enter a valid numeric ID.");
            }
        }
    }

    private LocalDate promptValidDate(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                return LocalDate.parse(input, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("[Error] Invalid date format or non-existent date! Use YYYY-MM-DD (e.g., 2026-09-30).");
            }
        }
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}
