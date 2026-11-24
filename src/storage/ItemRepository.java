package storage;

import model.Item;

import java.util.List;

/**
 * Storage contract for the inventory.
 *
 * <p>Implemented by {@link CsvStorage} (the default, dependency-free backend)
 * and {@link JdbcStorage} (SQL through JDBC/H2). {@code ItemService} only talks
 * to this interface, so the console app and the web dashboard work with either
 * backend unchanged.</p>
 */
public interface ItemRepository {

    /** Short description used in log lines, e.g. "CSV file - data/inventory.csv". */
    String describe();

    /** True when the repository already holds at least one record. */
    boolean hasExistingData();

    /** Reads every stored item, ordered by ID. */
    List<Item> loadItems();

    /** Replaces the stored contents with the given items; true on success. */
    boolean saveItems(List<Item> items);
}
