package storage;

import model.Item;

import java.sql.Connection;
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
            LOGGER.info("JDBC storage ready at " + url);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialise JDBC storage at " + url
                    + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String describe() {
        return "JDBC (H2) - " + url;
    }

    @Override
    public boolean hasExistingData() {
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM items")) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not count rows in " + url, e);
            return false;
        }
    }

    @Override
    public List<Item> loadItems() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT id, name, category, quantity, expiry_date, notes FROM items ORDER BY id";

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
                        rs.getString("notes") == null ? "" : rs.getString("notes"));
                items.add(item);
            }
            LOGGER.fine(() -> "Loaded " + items.size() + " row(s) from " + url);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to read items from " + url, e);
        }
        return items;
    }

    @Override
    public boolean saveItems(List<Item> items) {
        String insertSql = "INSERT INTO items (id, name, category, quantity, expiry_date, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

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
                    insert.addBatch();
                }
                insert.executeBatch();
                connection.commit();
                LOGGER.fine(() -> "Saved " + items.size() + " row(s) to " + url);
                return true;
            } catch (SQLException e) {
                connection.rollback();
                LOGGER.log(Level.SEVERE, "Failed to save items to " + url + " (transaction rolled back)", e);
                return false;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not open JDBC connection to " + url, e);
            return false;
        }
    }
}
