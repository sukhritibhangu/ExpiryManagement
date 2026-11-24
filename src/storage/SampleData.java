package storage;

import model.Item;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Starter items used when a brand-new install has neither CSV rows nor a snapshot.
 * Kept separate from {@link CsvStorage} so the JDBC backend can seed the same data.
 */
public final class SampleData {

    private SampleData() {
        // utility class
    }

    public static List<Item> createSampleItems() {
        LocalDate today = LocalDate.now();
        List<Item> samples = new ArrayList<>();

        // 1. Expired item (demonstrates EXPIRED alert)
        samples.add(new Item(1, "Whole Wheat Bread", "Bakery", 1, today.minusDays(2), "Check for mold before discarding"));

        // 2. Expiring soon item (in 2 days)
        samples.add(new Item(2, "Fresh Milk (1L)", "Dairy", 2, today.plusDays(2), "Store on middle refrigerator shelf"));

        // 3. Expiring soon item (in 5 days)
        samples.add(new Item(3, "Greek Yogurt (4-pack)", "Dairy", 1, today.plusDays(5), "Berry flavor"));

        // 4. Expiring soon item (in 6 days)
        samples.add(new Item(4, "Farm Fresh Eggs", "Poultry", 12, today.plusDays(6), "Grade A free-range"));

        // 5. Safe item (in 6 months)
        samples.add(new Item(5, "Paracetamol 500mg", "Medicine", 20, today.plusMonths(6), "First-aid kit medicine box"));

        // 6. Safe item (in 1 year)
        samples.add(new Item(6, "Extra Virgin Olive Oil", "Pantry", 1, today.plusYears(1), "Dark bottle in dry pantry"));

        return samples;
    }
}
