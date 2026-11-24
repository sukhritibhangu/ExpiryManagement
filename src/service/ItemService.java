package service;

import model.Item;
import storage.ItemRepository;
import storage.SampleData;
import storage.SnapshotStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

public class ItemService {
    private static final Logger LOGGER = Logger.getLogger(ItemService.class.getName());
    private final ItemRepository storage;
    private final ExpiryService expiryService;
    private final SnapshotStorage snapshot;
    private List<Item> items;
    private int nextId;

    public ItemService(ItemRepository storage, ExpiryService expiryService) {
        this(storage, expiryService, new SnapshotStorage());
    }

    public ItemService(ItemRepository storage, ExpiryService expiryService, SnapshotStorage snapshot) {
        this(storage, expiryService, snapshot, true);
    }

    public ItemService(ItemRepository storage, ExpiryService expiryService, SnapshotStorage snapshot,
                       boolean seedSamples) {
        this.storage = storage;
        this.expiryService = expiryService;
        this.snapshot = snapshot;
        if (storage.hasExistingData()) {
            items = storage.loadItems();
        } else {
            items = snapshot == null ? new ArrayList<>() : snapshot.loadSnapshot();
            if (items.isEmpty() && seedSamples) items = SampleData.createSampleItems();
            persist(items);
        }
        nextId = items.stream().mapToInt(Item::getId).max().orElse(0) + 1;
    }

    public Item addItem(String name, String category, int quantity, LocalDate expiryDate, String notes) {
        return saveItem(new Item(0, name, category, quantity, expiryDate, notes));
    }

    public synchronized Item saveItem(Item draft) {
        Item item = copy(draft);
        boolean adding = item.getId() == 0;
        var updated = new ArrayList<>(items);
        if (adding) {
            item.setId(nextId);
            updated.add(item);
        } else {
            int index = updated.indexOf(item);
            if (index < 0) throw new IllegalArgumentException("This product no longer exists.");
            updated.set(index, item);
        }
        persist(updated);
        if (adding) nextId++;
        LOGGER.info(() -> (adding ? "Added product " : "Updated product ") + item.getId());
        return copy(item);
    }

    public synchronized void restoreItem(Item item) {
        var updated = new ArrayList<>(items);
        if (updated.contains(item)) throw new IllegalArgumentException("This product ID is already in use.");
        updated.add(copy(item));
        persist(updated);
        nextId = Math.max(nextId, item.getId() + 1);
    }

    public synchronized boolean deleteItem(int id) {
        var updated = new ArrayList<>(items);
        if (!updated.removeIf(item -> item.getId() == id)) return false;
        persist(updated);
        LOGGER.info(() -> "Deleted product " + id);
        return true;
    }

    private void persist(List<Item> updated) {
        if (!storage.saveItems(updated)) {
            throw new IllegalStateException("Inventory could not be saved. Check storage permissions and try again.");
        }
        items = updated;
        if (snapshot != null) snapshot.saveSnapshot(updated);
    }

    private static Item copy(Item item) {
        return new Item(item.getId(), item.getName(), item.getCategory(), item.getQuantity(),
                item.getExpiryDate(), item.getNotes(), item.getManufactured(), item.getPrice(), item.getBatch());
    }

    public synchronized List<Item> getAllItems() {
        return new ArrayList<>(items.stream().map(ItemService::copy).toList());
    }

    public synchronized Optional<Item> findById(int id) {
        return items.stream().filter(item -> item.getId() == id).findFirst().map(ItemService::copy);
    }

    public List<Item> searchItems(String query) {
        String term = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return getAllItems().stream().filter(item -> item.getName().toLowerCase(Locale.ROOT).contains(term)
                || item.getCategory().toLowerCase(Locale.ROOT).contains(term)).toList();
    }

    public List<Item> getAlertItems() {
        return expiryService.getAlertItems(getAllItems());
    }

    public synchronized int getTotalCount() {
        return items.size();
    }
}
