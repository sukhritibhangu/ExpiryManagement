package model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Abstract base type for anything stored in the ledger.
 *
 * <p>Demonstrates inheritance, an abstract class and interface usage:</p>
 * <ul>
 *   <li>{@link Item} extends this class and inherits id/name/category/notes.</li>
 *   <li>Each subclass must supply its own {@link #getEntryType()} and {@link #getSummary()}.</li>
 *   <li>Implements {@link Serializable} so entries can be written to a binary snapshot.</li>
 * </ul>
 */
public abstract class InventoryEntry implements Serializable {

    /** Serialization version tag; bump it if the field layout ever changes. */
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String category;
    private String notes;

    /**
     * Full constructor for subclasses.
     *
     * @param id       unique identifier
     * @param name     short display name (required)
     * @param category grouping such as Dairy, Medicine or Pantry (required)
     * @param notes    optional free-text notes
     */
    protected InventoryEntry(int id, String name, String category, String notes) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Item name cannot be null").trim();
        this.category = Objects.requireNonNull(category, "Category cannot be null").trim();
        this.notes = notes == null ? "" : notes.trim();
    }

    /**
     * Convenience constructor (method/constructor overloading) for entries without notes.
     */
    protected InventoryEntry(int id, String name, String category) {
        this(id, name, category, "");
    }

    /** Human-readable type name, e.g. "Item". */
    public abstract String getEntryType();

    /** One-line description used by tables and logs. */
    public abstract String getSummary();

    // --- Inherited accessors ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Item name cannot be null").trim();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = Objects.requireNonNull(category, "Category cannot be null").trim();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryEntry other = (InventoryEntry) o;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }

    @Override
    public String toString() {
        return getEntryType() + "{id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
