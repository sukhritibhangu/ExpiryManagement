package service;

import model.Item;
import storage.CsvStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service managing inventory business logic.
 * 
 * Responsibilities:
 * - Adding, deleting, searching, and listing items.
 * - Generating unique sequential IDs.
 * - Coordinating with CsvStorage to persist changes automatically.
 * - Providing alert queries via ExpiryService.
 */
public class ItemService {

    private final List<Item> items;
    private final CsvStorage storage;
    private final ExpiryService expiryService;
    private int nextId;

    /**
     * Initializes the service with storage and expiry services,
     * loading either existing CSV data or initial sample items.
     *
     * @param storage       The CSV persistence handler
     * @param expiryService The expiry status and alert calculation handler
     */
    public ItemService(CsvStorage storage, ExpiryService expiryService) {
        this.storage = storage;
        this.expiryService = expiryService;
        this.items = new ArrayList<>(storage.loadOrCreateInitialData());
        this.nextId = calculateNextId();
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
        storage.saveItems(items);
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
            storage.saveItems(items);
        }
        return removed;
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
