package model;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents an item in the household or small business inventory.
 *
 * <p>Demonstrates Object-Oriented Programming (OOP) principles:</p>
 * <ul>
 *   <li>Inheritance: common fields and validation live in {@link InventoryEntry}.</li>
 *   <li>Interface: implements {@link Expirable} for expiry-date behaviour.</li>
 *   <li>Encapsulation: fields are private and accessed via public getters and setters.</li>
 *   <li>Validation: ensures required fields like name and quantity are valid.</li>
 * </ul>
 */
public class Item extends InventoryEntry implements Expirable {

    private static final long serialVersionUID = 1L;

    private int quantity;
    private LocalDate expiryDate;
    private LocalDate manufactured;
    private BigDecimal price;
    private String batch;

    public Item(int id, String name, String category, int quantity, LocalDate expiryDate, String notes,
                LocalDate manufactured, BigDecimal price, String batch) {
        this(id, name, category, quantity, expiryDate, notes);
        if (manufactured != null && manufactured.isAfter(expiryDate)) {
            throw new IllegalArgumentException("Manufacturing date must be on or before expiry.");
        }
        if (price != null && (price.signum() < 0 || price.scale() > 2
                || price.compareTo(new BigDecimal("999999999999.99")) > 0)) {
            throw new IllegalArgumentException("Enter a non-negative unit price with up to two decimal places.");
        }
        this.manufactured = manufactured;
        this.price = price;
        this.batch = batch == null ? "" : batch.strip();
    }

    public LocalDate getManufactured() { return manufactured; }
    public BigDecimal getPrice() { return price; }
    public String getBatch() { return batch == null ? "" : batch; }

    public BigDecimal inventoryValue() {
        return price == null ? BigDecimal.ZERO : price.multiply(BigDecimal.valueOf(quantity));
    }

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
        super(id, name, category, notes);
        setQuantity(quantity);
        this.expiryDate = Objects.requireNonNull(expiryDate, "Expiry date cannot be null");
    }

    /**
     * Overloaded constructor for items without notes.
     */
    public Item(int id, String name, String category, int quantity, LocalDate expiryDate) {
        this(id, name, category, quantity, expiryDate, "");
    }

    // --- Item-specific accessors ---

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        this.quantity = quantity;
    }

    @Override
    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = Objects.requireNonNull(expiryDate, "Expiry date cannot be null");
    }

    // --- InventoryEntry contract ---

    @Override
    public String getEntryType() {
        return "Item";
    }

    @Override
    public String getSummary() {
        return getName() + " (" + getCategory() + ")";
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                ", category='" + getCategory() + '\'' +
                ", quantity=" + quantity +
                ", expiryDate=" + expiryDate +
                ", notes='" + getNotes() + '\'' +
                '}';
    }
}

