package storage;

import model.Item;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CsvStorage implements ItemRepository {
    private static final Logger LOGGER = Logger.getLogger(CsvStorage.class.getName());
    private static final String HEADER =
            "ID,Name,Category,Quantity,ExpiryDate,Notes,ManufacturingDate,UnitPrice,Batch";
    private final Path path;

    public CsvStorage() { this("data/inventory.csv"); }

    public CsvStorage(String filePath) {
        path = Path.of(filePath);
    }

    public String getFilePath() { return path.toString(); }

    @Override public String describe() { return "CSV file - " + path; }

    @Override public boolean hasExistingData() { return !Files.notExists(path); }

    public List<Item> loadOrCreateInitialData() {
        if (hasExistingData()) return loadItems();
        List<Item> samples = createSampleItems();
        if (!saveItems(samples)) throw new IllegalStateException("Could not initialize inventory CSV.");
        return samples;
    }

    @Override public List<Item> loadItems() {
        List<Item> items = new ArrayList<>();
        if (!hasExistingData()) return items;
        int line = 1;
        try (var reader = Files.newBufferedReader(path)) {
            reader.readLine();
            StringBuilder record = new StringBuilder();
            boolean quoted = false;
            String segment;
            while ((segment = reader.readLine()) != null) {
                line++;
                if (!quoted && segment.isBlank()) continue;
                if (quoted) record.append('\n');
                record.append(segment);
                if ((segment.chars().filter(c -> c == '"').count() & 1) != 0) quoted = !quoted;
                if (!quoted) {
                    items.add(parse(record.toString()));
                    record.setLength(0);
                }
            }
            if (quoted) throw new IllegalArgumentException("Unclosed quoted field");
            return items;
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Could not read inventory CSV near line " + line + ".", e);
        }
    }

    @Override public boolean saveItems(List<Item> items) {
        return write(items, false);
    }

    public boolean exportItems(List<Item> items) {
        return write(items, true);
    }

    private boolean write(List<Item> items, boolean spreadsheetSafe) {
        Path temporary = null;
        try {
            Path target = path.toAbsolutePath();
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), "ledger-", ".tmp");
            try (var writer = Files.newBufferedWriter(temporary)) {
                writer.write(HEADER);
                writer.newLine();
                for (Item item : items) {
                    writer.write(String.join(",", Integer.toString(item.getId()),
                            field(item.getName(), spreadsheetSafe), field(item.getCategory(), spreadsheetSafe),
                            Integer.toString(item.getQuantity()), item.getExpiryDate().toString(),
                            field(item.getNotes(), spreadsheetSafe), value(item.getManufactured()),
                            value(item.getPrice()), field(item.getBatch(), spreadsheetSafe)));
                    writer.newLine();
                }
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write " + path, e);
            return false;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Could not clean up temporary CSV", e);
                }
            }
        }
    }

    public List<Item> createSampleItems() { return SampleData.createSampleItems(); }

    private static Item parse(String record) {
        List<String> fields = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < record.length(); i++) {
            char c = record.charAt(i);
            if (c == '"' && quoted && i + 1 < record.length() && record.charAt(i + 1) == '"') {
                value.append('"');
                i++;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                fields.add(value.toString());
                value.setLength(0);
            } else {
                value.append(c);
            }
        }
        fields.add(value.toString());
        if (fields.size() < 5) throw new IllegalArgumentException("Missing required fields");
        String manufactured = optional(fields, 6);
        String price = optional(fields, 7);
        return new Item(Integer.parseInt(fields.get(0).strip()), fields.get(1), fields.get(2),
                Integer.parseInt(fields.get(3).strip()), LocalDate.parse(fields.get(4).strip()),
                optional(fields, 5), manufactured.isBlank() ? null : LocalDate.parse(manufactured),
                price.isBlank() ? null : new BigDecimal(price), optional(fields, 8));
    }

    private static String optional(List<String> fields, int index) {
        return index < fields.size() ? fields.get(index).strip() : "";
    }

    private static String value(Object value) { return value == null ? "" : value.toString(); }

    private static String field(String value, boolean spreadsheetSafe) {
        String trimmed = value.stripLeading();
        if (spreadsheetSafe && !trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) {
            value = "'" + value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
