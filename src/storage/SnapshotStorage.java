package storage;

import model.Item;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Binary backup of the inventory built on Java object serialization.
 *
 * <p>The CSV file remains the primary, human-readable store. This class writes a
 * {@link java.io.Serializable} snapshot next to it so the ledger can be restored
 * if the CSV is deleted or corrupted.</p>
 */
public class SnapshotStorage {

    private static final Logger LOGGER = Logger.getLogger(SnapshotStorage.class.getName());

    /** Magic header so we can detect a snapshot from a completely different program. */
    private static final String SNAPSHOT_HEADER = "ExpiryAlertLedger-Snapshot-v1";

    private static final String DEFAULT_PATH = "data/inventory.snapshot";

    private final String filePath;

    /** Default constructor pointing to {@code data/inventory.snapshot}. */
    public SnapshotStorage() {
        this(DEFAULT_PATH);
    }

    /** Constructor allowing a custom snapshot path (useful for tests). */
    public SnapshotStorage(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    /**
     * Serializes the whole inventory into the snapshot file.
     *
     * @param items current in-memory items
     * @return true when the snapshot was written
     */
    public boolean saveSnapshot(List<Item> items) {
        Path path = Paths.get(filePath);
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(path)))) {
                out.writeUTF(SNAPSHOT_HEADER);
                out.writeObject(new ArrayList<>(items));
            }
            LOGGER.info("Saved serializable snapshot of " + items.size() + " item(s) to " + filePath);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not write snapshot " + filePath, e);
            return false;
        }
    }

    /**
     * Deserializes the snapshot. Returns an empty list when the file is missing,
     * unreadable or was written by an incompatible version.
     */
    public List<Item> loadSnapshot() {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            LOGGER.fine("No snapshot found at " + filePath);
            return new ArrayList<>();
        }

        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {
            String header = in.readUTF();
            if (!SNAPSHOT_HEADER.equals(header)) {
                LOGGER.warning("Ignoring snapshot with unexpected header: " + header);
                return new ArrayList<>();
            }

            Object payload = in.readObject();
            if (!(payload instanceof List<?> rawList)) {
                LOGGER.warning("Snapshot does not contain a list; ignoring " + filePath);
                return new ArrayList<>();
            }

            List<Item> items = new ArrayList<>();
            for (Object entry : rawList) {
                if (entry instanceof Item item) {
                    items.add(item);
                }
            }
            LOGGER.info("Restored " + items.size() + " item(s) from snapshot " + filePath);
            return items;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            LOGGER.log(Level.WARNING, "Could not read snapshot " + filePath + "; starting from CSV", e);
            return new ArrayList<>();
        }
    }
}
