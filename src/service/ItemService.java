package service;

import model.Item;
import storage.ItemRepository;
import storage.SampleData;
import storage.SnapshotStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service managing inventory business logic.
 *
 * Responsibilities:
 * - Adding, deleting, searching, and listing items.
 * - Generating unique sequential IDs.
 * - Coordinating with CsvStorage to persist changes automatically.
 * - Keeping a serializable backup via SnapshotStorage.
 * - Providing alert queries via ExpiryService.
 */
public class ItemService {

    private static final Logger LOGGER = Logger.getLogger(ItemService.class.getName());

    private final List<Item> items;
    private final ItemRepository storage;
    private final ExpiryService expiryService;
    private final SnapshotStorage snapshotStorage;
    private int nextId;

    /**
     * Convenience constructor: uses the default CSV file and default snapshot file.
     *
     * @param storage       The inventory repository (CSV or JDBC)
     * @param expiryService The expiry status and alert calculation handler
     */
    public ItemService(ItemRepository storage, ExpiryService expiryService) {
        this(storage, expiryService, new SnapshotStorage());
    }

    /**
     * Full constructor (constructor overloading) allowing a custom snapshot location.
     *
     * <p>Loading order: CSV first, then the serializable snapshot when the CSV
     * holds no records. A snapshot-only restore rewrites the CSV so both stores agree.</p>
     *
     * @param storage         The inventory repository (CSV or JDBC)
     * @param expiryService   The expiry status and alert calculation handler
     * @param snapshotStorage The serializable backup handler
     */
    public ItemService(ItemRepository storage, ExpiryService expiryService, SnapshotStorage snapshotStorage) {
        this.storage = storage;
        this.expiryService = expiryService;
        this.snapshotStorage = snapshotStorage;

        List<Item> loaded;
        if (storage.hasExistingData()) {
            // The primary store (CSV or JDBC) always wins when it has rows.
            loaded = storage.loadItems();
        } else {
            // No CSV rows: fall back to the serializable snapshot before seeding samples.
            List<Item> restored = snapshotStorage.loadSnapshot();
            if (!restored.isEmpty()) {
                loaded = restored;
                storage.saveItems(loaded);
                LOGGER.info("CSV was empty; restored " + loaded.size()
                        + " item(s) from the serializable snapshot");
            } else {
                loaded = SampleData.createSampleItems();
                storage.saveItems(loaded);
                LOGGER.info("No CSV data or snapshot found; created " + loaded.size()
                        + " starter item(s)");
            }
        }

        this.items = new ArrayList<>(loaded);
        this.nextId = calculateNextId();
        LOGGER.info("Inventory ready with " + items.size() + " item(s); next ID is " + nextId);
    }

    /**
     * Finds the next unique ID by locating the current maximum ID + 1.
     */
    private int calculateNextId() {
        int maxId = 0;
        for (Item item : items) {
            if (item.getId() > maxId) {
                maxId = item.getId();
            }
        }
        return maxId + 1;
    }

    /**
     * Adds a new item to the inventory, assigns a unique ID, and saves to CSV.
     *
     * @param name       Item name
     * @param category   Item category
     * @param quantity   Positive quantity
     * @param expiryDate Expiration date
     * @param notes      Optional notes
     * @return The newly created and saved Item
     */
    public Item addItem(String name, String category, int quantity, LocalDate expiryDate, String notes) {
        Item newItem = new Item(nextId++, name, category, quantity, expiryDate, notes);
        items.add(newItem);
        persist();
        LOGGER.info("Added " + newItem.getSummary() + " (ID " + newItem.getId() + ")");
        return newItem;
    }

    /**
     * Returns an unmodifiable view of all inventory items.
     */
    public List<Item> getAllItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Searches for an item by its unique ID.
     *
     * @param id The item ID
     * @return Optional containing the item if found, or empty Optional
     */
    public Optional<Item> findById(int id) {
        for (Item item : items) {
            if (item.getId() == id) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    /**
     * Searches items by name or category (case-insensitive partial match).
     *
     * @param query The search text entered by the user
     * @return List of matching items
     */
    public List<Item> searchItems(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(items);
        }

        String lowerQuery = query.trim().toLowerCase();
        List<Item> matches = new ArrayList<>();

        for (Item item : items) {
            boolean nameMatches = item.getName().toLowerCase().contains(lowerQuery);
            boolean categoryMatches = item.getCategory().toLowerCase().contains(lowerQuery);
            if (nameMatches || categoryMatches) {
                matches.add(item);
            }
        }
        LOGGER.fine(() -> "Search '" + lowerQuery + "' matched " + matches.size() + " item(s)");
        return matches;
    }

    /**
     * Deletes an item by its unique ID and immediately saves changes to CSV.
     *
     * @param id The ID of the item to remove
     * @return true if an item was removed, false if the ID was not found
     */
    public boolean deleteItem(int id) {
        boolean removed = items.removeIf(item -> item.getId() == id);
        if (removed) {
            persist();
            LOGGER.info("Deleted item with ID " + id);
        } else {
            LOGGER.warning("Delete requested for unknown item ID " + id);
        }
        return removed;
    }

    /**
     * Writes the current inventory to the active backend and then to the
     * serializable snapshot. A backend failure is logged but does not stop the
     * snapshot from being written, so the backup still holds the latest state.
     */
    private void persist() {
        boolean saved = storage.saveItems(items);
        if (!saved) {
            LOGGER.warning("Primary storage save failed; the snapshot backup still holds the latest inventory");
        }
        snapshotStorage.saveSnapshot(items);
    }

    /**
     * Returns all items currently flagged with an alert (Expired or Expiring Soon).
     */
    public List<Item> getAlertItems() {
        return expiryService.getAlertItems(items);
    }

    /**
     * Returns the total count of items in inventory.
     */
    public int getTotalCount() {
        return items.size();
    }
}
