package storage;

import model.Item;

import java.sql.Connection;
import java.time.LocalDate;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC-backed inventory storage (default database: embedded H2).
 *
 * <p>Only {@code java.sql} is imported, so this class compiles with a plain JDK.
 * The H2 driver is needed at <em>runtime</em> only; put {@code h2-*.jar} on the
 * classpath (Maven does this automatically).</p>
 *
 * <p>Select it with {@code -Dledger.storage=jdbc} or the {@code LEDGER_STORAGE=jdbc}
 * environment variable. Override the database URL with {@code -Dledger.db.url} or
 * {@code LEDGER_DB_URL}.</p>
 */
public class JdbcStorage implements ItemRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcStorage.class.getName());

    /** Driver class name; loaded reflectively so the class compiles without the jar. */
    private static final String DRIVER_CLASS = "org.h2.Driver";

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS items (
                id          INT PRIMARY KEY,
                name        VARCHAR(200) NOT NULL,
                category    VARCHAR(100) NOT NULL,
                quantity    INT NOT NULL,
                expiry_date DATE NOT NULL,
                notes       VARCHAR(500)
            )
            """;

    private final String url;
    private final String username;
    private final String password;

    /** Convenience constructor for a database without credentials (H2 file/memory). */
    public JdbcStorage(String url) {
        this(url, null, null);
    }

    public JdbcStorage(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        loadDriver();
        createSchema();
    }

    /**
     * Loads the JDBC driver with a clear message when the jar is missing.
     */
    private static void loadDriver() {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "H2 JDBC driver not found on the classpath. Add lib/h2-<version>.jar "
                            + "(mvn dependency:copy-dependencies) or run through Maven. "
                            + "CSV mode still works with -Dledger.storage=csv.",
                    e);
        }
    }

    private Connection connect() throws SQLException {
        if (username == null) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, username, password);
    }

    private void createSchema() {
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_TABLE_SQL);
            statement.executeUpdate("ALTER TABLE items ADD COLUMN IF NOT EXISTS manufactured DATE");
            statement.executeUpdate("ALTER TABLE items ADD COLUMN IF NOT EXISTS price DECIMAL(14,2)");
            statement.executeUpdate("ALTER TABLE items ADD COLUMN IF NOT EXISTS batch VARCHAR(100)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ledger_initialized (id INT PRIMARY KEY)");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialise JDBC storage.", e);
        }
    }

    @Override
    public String describe() {
        return "JDBC (H2)";
    }

    @Override
    public boolean hasExistingData() {
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT (SELECT COUNT(*) FROM items) + (SELECT COUNT(*) FROM ledger_initialized)")) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not check database contents.", e);
        }
    }

    @Override
    public List<Item> loadItems() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT id, name, category, quantity, expiry_date, notes, manufactured, price, batch FROM items ORDER BY id";

        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {

            while (rs.next()) {
                Item item = new Item(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getInt("quantity"),
                        rs.getDate("expiry_date").toLocalDate(),
                        rs.getString("notes"), rs.getObject("manufactured", LocalDate.class),
                        rs.getBigDecimal("price"), rs.getString("batch"));
                items.add(item);
            }
            LOGGER.fine(() -> "Loaded " + items.size() + " database row(s)");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read database inventory.", e);
        }
        return items;
    }

    @Override
    public boolean saveItems(List<Item> items) {
        String insertSql = "INSERT INTO items (id, name, category, quantity, expiry_date, notes, manufactured, price, batch) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (Statement clear = connection.createStatement();
                 PreparedStatement insert = connection.prepareStatement(insertSql)) {

                clear.executeUpdate("DELETE FROM items");

                for (Item item : items) {
                    insert.setInt(1, item.getId());
                    insert.setString(2, item.getName());
                    insert.setString(3, item.getCategory());
                    insert.setInt(4, item.getQuantity());
                    insert.setDate(5, Date.valueOf(item.getExpiryDate()));
                    insert.setString(6, item.getNotes());
                    insert.setObject(7, item.getManufactured(), java.sql.Types.DATE);
                    insert.setBigDecimal(8, item.getPrice());
                    insert.setString(9, item.getBatch());
                    insert.addBatch();
                }
                insert.executeBatch();
                clear.executeUpdate("MERGE INTO ledger_initialized KEY(id) VALUES (1)");
                connection.commit();
                LOGGER.fine(() -> "Saved " + items.size() + " database row(s)");
                return true;
            } catch (SQLException e) {
                connection.rollback();
                LOGGER.log(Level.SEVERE, "Database save failed and was rolled back", e);
                return false;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "JDBC connection failed", e);
            return false;
        }
    }
}

