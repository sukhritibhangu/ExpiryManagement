package service;

import model.ExpiryStatus;
import model.Item;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service responsible for calculating expiry statuses and alerts.
 * 
 * Rules:
 * - Expired: Expiry date is before today.
 * - Expiring Soon: Expiry date is within the next 7 days (inclusive of today).
 * - Safe: Expiry date is more than 7 days in the future.
 */
public class ExpiryService {

    private static final Logger LOGGER = Logger.getLogger(ExpiryService.class.getName());

    /**
     * Calculates the ExpiryStatus for a given item against a reference date.
     *
     * @param expiryDate    The item's expiration date
     * @param referenceDate The date to compare against (usually today)
     * @return ExpiryStatus (EXPIRED, EXPIRING_SOON, or SAFE)
     */
    public ExpiryStatus calculateStatus(LocalDate expiryDate, LocalDate referenceDate) {
        if (expiryDate.isBefore(referenceDate)) {
            return ExpiryStatus.EXPIRED;
        }

        LocalDate alertThreshold = referenceDate.plusDays(7);
        if (!expiryDate.isAfter(alertThreshold)) {
            return ExpiryStatus.EXPIRING_SOON;
        }

        return ExpiryStatus.SAFE;
    }

    /**
     * Overloaded method using the current system date as reference.
     */
    public ExpiryStatus calculateStatus(LocalDate expiryDate) {
        return calculateStatus(expiryDate, LocalDate.now());
    }

    /**
     * Calculates the number of days remaining until the item expires.
     * Returns a negative number if the item has already expired.
     *
     * @param expiryDate    The item's expiration date
     * @param referenceDate The date to compare against
     * @return Number of days remaining (positive, 0, or negative)
     */
    public long getDaysRemaining(LocalDate expiryDate, LocalDate referenceDate) {
        return ChronoUnit.DAYS.between(referenceDate, expiryDate);
    }

    /**
     * Overloaded method to calculate days remaining from today.
     */
    public long getDaysRemaining(LocalDate expiryDate) {
        return getDaysRemaining(expiryDate, LocalDate.now());
    }

    /**
     * Returns a human-friendly string describing the time left.
     * Examples: "Expired 3 days ago", "Expires today", "Expires tomorrow", "5 days left"
     */
    public String formatDaysRemaining(long days) {
        if (days < -1) {
            return "Expired " + Math.abs(days) + " days ago";
        } else if (days == -1) {
            return "Expired yesterday";
        } else if (days == 0) {
            return "Expires today!";
        } else if (days == 1) {
            return "1 day left (Tomorrow)";
        } else {
            return days + " days left";
        }
    }

    /**
     * Filters a list of items and returns only items needing immediate attention
     * (either EXPIRED or EXPIRING_SOON), sorted with the most urgent items first.
     *
     * @param items List of items to evaluate
     * @return List of alert items sorted by expiry date ascending
     */
    public List<Item> getAlertItems(List<Item> items) {
        LocalDate today = LocalDate.now();
        List<Item> alertItems = new ArrayList<>();

        for (Item item : items) {
            ExpiryStatus status = calculateStatus(item.getExpiryDate(), today);
            if (status == ExpiryStatus.EXPIRED || status == ExpiryStatus.EXPIRING_SOON) {
                alertItems.add(item);
            }
        }

        // Sort urgent items by earliest expiry date first
        alertItems.sort(Comparator.comparing(Item::getExpiryDate));
        LOGGER.fine(() -> "Evaluated " + items.size() + " item(s): " + alertItems.size()
                + " need attention");
        return alertItems;
    }
}
