import config.LedgerPreferences;
import model.ExpiryBand;
import model.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.ExpiryService;
import service.ItemService;
import service.LabelScanner;
import storage.CsvStorage;
import storage.ItemRepository;
import storage.JdbcStorage;
import storage.SnapshotStorage;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LedgerRegressionTest {
    @TempDir Path temp;
    private final LocalDate now = LocalDate.of(2026, 11, 24);

    @Test void exactExpiryBoundariesAndLegacyRule() {
        long[] days = {-1, 0, 1, 3, 4, 7, 8, 30, 31};
        ExpiryBand[] bands = {ExpiryBand.EXPIRED, ExpiryBand.TODAY, ExpiryBand.THREE_DAYS,
                ExpiryBand.THREE_DAYS, ExpiryBand.SEVEN_DAYS, ExpiryBand.SEVEN_DAYS,
                ExpiryBand.THIRTY_DAYS, ExpiryBand.THIRTY_DAYS, ExpiryBand.SAFE};
        ExpiryService expiry = new ExpiryService();
        for (int i = 0; i < days.length; i++) {
            assertEquals(bands[i], ExpiryBand.of(expiry.getDaysRemaining(now.plusDays(days[i]), now)));
        }
        assertEquals(model.ExpiryStatus.EXPIRING_SOON, expiry.calculateStatus(now.plusDays(7), now));
        assertEquals(model.ExpiryStatus.SAFE, expiry.calculateStatus(now.plusDays(8), now));
    }

    @Test void csvReadsOldSchemaAndPersistsCrudIncludingEmptyInventory() throws Exception {
        Path file = temp.resolve("inventory.csv");
        Files.writeString(file, "ID,Name,Category,Quantity,ExpiryDate,Notes\n9,Milk,Dairy,2,2026-11-28,Cold\n");
        CsvStorage csv = new CsvStorage(file.toString());
        ItemService service = open(csv);
        assertEquals("Milk", service.searchItems("dAiRy").get(0).getName());
        Item saved = service.saveItem(draft(0));
        assertEquals(10, saved.getId());
        assertEquals("Café, \"Organic\"", open(csv).findById(10).orElseThrow().getName());
        assertEquals("Line one\nLine two", open(csv).findById(10).orElseThrow().getNotes());
        assertEquals(new BigDecimal("10.50"), open(csv).findById(10).orElseThrow().getPrice());
        service.saveItem(new Item(10, "Tea", "Pantry", 3, now));
        assertEquals("Tea", open(csv).searchItems("tea").get(0).getName());
        assertTrue(service.deleteItem(10));
        assertFalse(service.deleteItem(10));
        service.restoreItem(saved);
        assertTrue(open(csv).findById(10).isPresent());
        service.deleteItem(9);
        service.deleteItem(10);
        assertEquals(0, open(csv).getTotalCount());
    }

    @Test void jdbcMigratesOldTableAndPersistsMetadataAndEmptyInventory() throws Exception {
        String url = "jdbc:h2:" + temp.resolve("h2").toString().replace('\\', '/');
        try (var connection = DriverManager.getConnection(url); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE items (id INT PRIMARY KEY, name VARCHAR(200), category VARCHAR(100),"
                    + " quantity INT, expiry_date DATE, notes VARCHAR(500))");
            sql.execute("INSERT INTO items VALUES (1, 'Milk', 'Dairy', 1, DATE '2026-11-28', '')");
        }
        ItemService service = open(new JdbcStorage(url));
        assertNull(service.findById(1).orElseThrow().getPrice());
        Item saved = service.saveItem(draft(0));
        Item loaded = open(new JdbcStorage(url)).findById(saved.getId()).orElseThrow();
        assertEquals("B-23", loaded.getBatch());
        assertEquals(now.minusDays(3), loaded.getManufactured());
        assertEquals(new BigDecimal("21.00"), loaded.inventoryValue());
        service.deleteItem(1);
        service.deleteItem(saved.getId());
        assertEquals(0, open(new JdbcStorage(url)).getTotalCount());
    }

    @Test void failedSaveDoesNotChangeMemoryOrConsumeId() {
        class Unreliable implements ItemRepository {
            boolean available;
            @Override public String describe() { return "test"; }
            @Override public boolean hasExistingData() { return true; }
            @Override public List<Item> loadItems() { return List.of(); }
            @Override public boolean saveItems(List<Item> items) { return available; }
        }
        Unreliable storage = new Unreliable();
        ItemService service = open(storage);
        assertThrows(IllegalStateException.class, () -> service.saveItem(draft(0)));
        assertEquals(0, service.getTotalCount());
        storage.available = true;
        assertEquals(1, service.saveItem(draft(0)).getId());
        storage.available = false;
        assertThrows(IllegalStateException.class, () -> service.deleteItem(1));
        assertEquals(1, service.getTotalCount());
    }

    @Test void preferencesAndReadStateSurviveRestart() throws Exception {
        Path path = temp.resolve("profile.properties");
        LedgerPreferences preferences = new LedgerPreferences(path);
        preferences.update(30, true, "My ledger");
        preferences.markRead(Set.of("1/2026-11-24/TODAY"));
        LedgerPreferences reopened = new LedgerPreferences(path);
        assertEquals(30, reopened.reminder());
        assertEquals("My ledger", reopened.profile());
        assertTrue(reopened.reduced());
        assertTrue(reopened.read("1/2026-11-24/TODAY"));
    }

    @Test void snapshotRoundTripsAndRestoresMetadata() {
        SnapshotStorage snapshot = new SnapshotStorage(temp.resolve("saved.snapshot").toString());
        assertTrue(snapshot.saveSnapshot(List.of(draft(5))));
        ItemService restored = new ItemService(new CsvStorage(temp.resolve("restore.csv").toString()),
                new ExpiryService(), snapshot, false);
        assertEquals("B-23", restored.findById(5).orElseThrow().getBatch());
    }

    @Test void labelParserRecognizesOnlyLabeledDates() {
        LabelScanner.Result parsed = LabelScanner.interpret("Organic Tea\nMFG 21/11/2026\nEXP 2026-12-24\nBatch: B-23");
        assertEquals("Organic Tea", parsed.name());
        assertEquals(now.minusDays(3), parsed.manufactured());
        assertEquals(now.plusDays(30), parsed.expires());
        assertEquals("B-23", parsed.batch());
        assertNull(LabelScanner.interpret("Printed 2026-12-24").expires());
        assertNull(LabelScanner.interpret("EXP 31/02/2026").expires());
        assertThrows(IllegalArgumentException.class, () -> LabelScanner.interpret(" \n"));
    }

    @Test void browserWorkspacesArePrivateAndRejectConcurrentWriters() throws Exception {
        String firstKey = config.WorkspaceAccess.generateKey();
        String secondKey = config.WorkspaceAccess.generateKey();
        Path root = temp.resolve("workspaces");
        try (var first = config.WorkspaceAccess.open(root, firstKey, true);
             var second = config.WorkspaceAccess.open(root, secondKey, true)) {
            assertNotEquals(first.directory(), second.directory());
            assertFalse(first.directory().getFileName().toString().contains(firstKey));
            var one = open(new CsvStorage(first.directory().resolve("inventory.csv").toString()));
            var two = open(new CsvStorage(second.directory().resolve("inventory.csv").toString()));
            one.saveItem(draft(0));
            assertEquals(0, two.getTotalCount());
            assertThrows(java.io.IOException.class, () -> config.WorkspaceAccess.open(root, firstKey, false));
            assertThrows(java.io.IOException.class, () -> config.WorkspaceAccess.open(root, "../other", false));
        }
        try (var reopened = config.WorkspaceAccess.open(root, firstKey, false)) {
            assertEquals(1, open(new CsvStorage(reopened.directory().resolve("inventory.csv").toString())).getTotalCount());
        }
    }

    @Test void legacyProfilePasswordHashUsesUniqueSaltAndVerifies() {
        String first = util.PasswordHash.create("correct horse battery staple");
        String second = util.PasswordHash.create("correct horse battery staple");
        assertNotEquals(first, second);
        assertTrue(util.PasswordHash.verify("correct horse battery staple", first));
        assertFalse(util.PasswordHash.verify("other value", first));
        assertFalse(util.PasswordHash.verify("other value", "not a hash"));
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "LEDGER_TESSERACT", matches = ".+")
    void actualOcrExtractsPrintedLabel() throws Exception {
        var image = new java.awt.image.BufferedImage(1350, 470, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(java.awt.Color.BLACK);
            graphics.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 54));
            graphics.drawString("Organic Green Tea", 45, 90);
            graphics.drawString("MFG 2026-11-21", 45, 185);
            graphics.drawString("EXP 2026-12-24", 45, 280);
            graphics.drawString("Batch B-23", 45, 375);
        } finally {
            graphics.dispose();
        }
        Path path = temp.resolve("label.png");
        javax.imageio.ImageIO.write(image, "png", path.toFile());
        LabelScanner.Result parsed = new LabelScanner().scan(path);
        assertEquals("Organic Green Tea", parsed.name());
        assertEquals(now.minusDays(3), parsed.manufactured());
        assertEquals(now.plusDays(30), parsed.expires());
        assertEquals("B-23", parsed.batch());
    }

    private ItemService open(ItemRepository repository) {
        return new ItemService(repository, new ExpiryService(), null, false);
    }

    private Item draft(int id) {
        return new Item(id, "Café, \"Organic\"", "Pantry", 2, now,
                "Line one\nLine two", now.minusDays(3), new BigDecimal("10.50"), "B-23");
    }
}
