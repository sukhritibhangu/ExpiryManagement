package model;

public enum ExpiryBand {
    EXPIRED("Expired", "rose"), TODAY("Expires today", "rose"),
    THREE_DAYS("Within 3 days", "amber"), SEVEN_DAYS("Within 7 days", "amber"),
    THIRTY_DAYS("Within 30 days", "amber"), SAFE("Safe", "sage");

    private final String label;
    private final String color;

    ExpiryBand(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public static ExpiryBand of(long days) {
        if (days < 0) return EXPIRED;
        if (days == 0) return TODAY;
        if (days <= 3) return THREE_DAYS;
        if (days <= 7) return SEVEN_DAYS;
        if (days <= 30) return THIRTY_DAYS;
        return SAFE;
    }

    public String color() { return color; }
    @Override public String toString() { return label; }
}
