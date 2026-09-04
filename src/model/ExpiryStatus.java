package model;

/**
 * Represents the expiration status of an inventory item.
 * 
 * Rules:
 * - EXPIRED: Expiry date is before today.
 * - EXPIRING_SOON: Expiry date is between today and the next 7 days (inclusive).
 * - SAFE: Expiry date is more than 7 days in the future.
 */
public enum ExpiryStatus {
    EXPIRED("Expired", "[EXPIRED]"),
    EXPIRING_SOON("Expiring Soon", "[EXPIRING SOON]"),
    SAFE("Safe", "[SAFE]");

    private final String label;
    private final String badge;

    ExpiryStatus(String label, String badge) {
        this.label = label;
        this.badge = badge;
    }

    public String getLabel() {
        return label;
    }

    public String getBadge() {
        return badge;
    }

    @Override
    public String toString() {
        return label;
    }
}
