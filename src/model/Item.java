package model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents an item in the household or small business inventory.
 * 
 * Demonstrates Object-Oriented Programming (OOP) principles:
 * - Encapsulation: Fields are private and accessed via public getters and setters.
 * - Validation: Ensures required fields like name and quantity are valid.
 */
public class Item {
    private int id;
    private String name;
    private String category;
    private int quantity;
    private LocalDate expiryDate;
    private String notes;

    /**
     * Constructs a new Item with all required and optional attributes.
     *
     * @param id          Unique identifier for the item
     * @param name        Name or description of the item
     * @param category    Category (e.g., Dairy, Medicine, Pantry, Bakery)
     * @param quantity    Positive integer count of the item
     * @param expiryDate  The expiration date as a LocalDate
     * @param notes       Optional notes (e.g., "Keep refrigerated", "Batch #12")
     */
    public Item(int id, String name, String category, int quantity, LocalDate expiryDate, String notes) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Item name cannot be null").trim();
        this.category = Objects.requireNonNull(category, "Category cannot be null").trim();
        this.quantity = quantity;
        this.expiryDate = Objects.requireNonNull(expiryDate, "Expiry date cannot be null");
        this.notes = notes == null ? "" : notes.trim();
    }

    // --- Getters and Setters ---

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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        this.quantity = quantity;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = Objects.requireNonNull(expiryDate, "Expiry date cannot be null");
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
        Item item = (Item) o;
        return id == item.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", quantity=" + quantity +
                ", expiryDate=" + expiryDate +
                ", notes='" + notes + '\'' +
                '}';
    }
}
