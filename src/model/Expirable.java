package model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Contract for anything that carries an expiry date.
 *
 * <p>Shows a user-defined interface with a concrete default method
 * ({@link #daysUntilExpiry(LocalDate)}) alongside the abstract one.</p>
 */
public interface Expirable {

    /** The date this entry expires. */
    LocalDate getExpiryDate();

    /**
     * Days between a reference date and the expiry date.
     * Negative when already expired.
     */
    default long daysUntilExpiry(LocalDate referenceDate) {
        return ChronoUnit.DAYS.between(referenceDate, getExpiryDate());
    }

    /** Days remaining calculated from today. */
    default long daysUntilExpiry() {
        return daysUntilExpiry(LocalDate.now());
    }
}
