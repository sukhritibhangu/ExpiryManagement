package storage;

import model.Item;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles persistent storage of inventory items to and from a local CSV file.
 * 
 * Features:
 * - Reads and writes standard CSV format.
 * - Safely handles quotes and commas inside text fields.
 * - Auto-creates storage directory if missing.
 * - Generates beginner-friendly sample items when no data file exists.
 */
public class CsvStorage implements ItemRepository {

    private static final Logger LOGGER = Logger.getLogger(CsvStorage.class.getName());

    private final String filePath;
    private static final String CSV_HEADER = "ID,Name,Category,Quantity,ExpiryDate,Notes";

    /**
     * Default constructor pointing to "data/inventory.csv".
     */
    public CsvStorage() {
        this("data/inventory.csv");
    }

    /**
     * Constructor allowing custom file path (useful for testing or flexible location).
     *
     * @param filePath Path to the CSV file
     */
    public CsvStorage(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String describe() {
        return "CSV file - " + filePath;
    }

    /**
     * Checks if the CSV file exists and has saved records beyond just a header.
     *
     * @return true if valid saved data exists, false otherwise
     */
    public boolean hasExistingData() {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    return true; // Found at least one data row
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    /**
     * Loads items from the CSV file. If no file or records exist,
     * initializes and saves a curated set of sample items.
     *
     * @return List of loaded or newly initialized Item objects
     */
    public List<Item> loadOrCreateInitialData() {
        if (!hasExistingData()) {
            List<Item> sampleItems = createSampleItems();
            saveItems(sampleItems);
            return sampleItems;
        }
        return loadItems();
    }

    /**
     * Loads all items from the CSV file.
     *
     * @return List of parsed Item objects
     */
    public List<Item> loadItems() {
        List<Item> items = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) {
            return items;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // Read header line
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    Item item = parseCsvLine(line);
                    if (item != null) {
                        items.add(item);
                    }
                } catch (Exception e) {
                    LOGGER.warning("Skipping corrupt CSV row at line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read CSV file: " + filePath, e);
        }

        LOGGER.fine(() -> "Loaded " + items.size() + " item(s) from " + filePath);
        return items;
    }

    /**
     * Saves the provided list of items to the CSV file.
     * Overwrites existing content to ensure persistent state matches in-memory state.
     *
     * @param items List of items to save
     * @return true if successful, false otherwise
     */
    public boolean saveItems(List<Item> items) {
        try {
            Path path = Paths.get(filePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
                writer.write(CSV_HEADER);
                writer.newLine();

                for (Item item : items) {
                    writer.write(formatItemToCsv(item));
                    writer.newLine();
                }
            }
            LOGGER.fine(() -> "Saved " + items.size() + " item(s) to " + filePath);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save items to CSV: " + filePath, e);
            return false;
        }
    }

    /**
     * Creates default realistic sample items for initial onboarding.
     */
    public List<Item> createSampleItems() {
        return SampleData.createSampleItems();
    }

    /**
     * Formats an Item into a single CSV row, escaping commas and double-quotes if necessary.
     */
    private String formatItemToCsv(Item item) {
        return escapeCsvField(String.valueOf(item.getId())) + "," +
               escapeCsvField(item.getName()) + "," +
               escapeCsvField(item.getCategory()) + "," +
               item.getQuantity() + "," +
               item.getExpiryDate() + "," +
               escapeCsvField(item.getNotes());
    }

    /**
     * Parses a single CSV line into an Item object, respecting quoted values.
     */
    private Item parseCsvLine(String line) {
        List<String> tokens = splitCsvLine(line);
        if (tokens.size() < 5) {
            throw new IllegalArgumentException("Expected at least 5 fields, got " + tokens.size());
        }

        int id = Integer.parseInt(tokens.get(0).trim());
        String name = tokens.get(1).trim();
        String category = tokens.get(2).trim();
        int quantity = Integer.parseInt(tokens.get(3).trim());
        LocalDate expiryDate = LocalDate.parse(tokens.get(4).trim());
        String notes = tokens.size() > 5 ? tokens.get(5).trim() : "";

        return new Item(id, name, category, quantity, expiryDate, notes);
    }

    /**
     * Splits a CSV line into fields, handling quotes and commas properly.
     */
    private List<String> splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    // Escaped quote: "" -> "
                    sb.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());
        return fields;
    }

    /**
     * Wraps a field in quotes if it contains commas, double quotes, or newlines.
     */
    private String escapeCsvField(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
