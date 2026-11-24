package storage;

import java.util.logging.Logger;

/**
 * Chooses the storage backend at startup.
 *
 * <p>CSV is the default so the project still runs with nothing but a JDK.
 * Enable the JDBC backend with either:</p>
 * <pre>
 *   java -Dledger.storage=jdbc -cp "bin:lib/*" app.Main
 *   LEDGER_STORAGE=jdbc java -cp "bin:lib/*" app.Main
 * </pre>
 */
public final class StorageFactory {

    private static final Logger LOGGER = Logger.getLogger(StorageFactory.class.getName());

    public static final String BACKEND_PROPERTY = "ledger.storage";
    public static final String BACKEND_ENV = "LEDGER_STORAGE";
    public static final String DB_URL_PROPERTY = "ledger.db.url";
    public static final String DB_URL_ENV = "LEDGER_DB_URL";

    static final String DEFAULT_BACKEND = "csv";
    static final String DEFAULT_DB_URL = "jdbc:h2:./data/inventory";

    private StorageFactory() {
        // utility class
    }

    /** Creates the repository configured by system properties / environment. */
    public static ItemRepository create() {
        return create(resolve(BACKEND_PROPERTY, BACKEND_ENV, DEFAULT_BACKEND),
                resolve(DB_URL_PROPERTY, DB_URL_ENV, DEFAULT_DB_URL));
    }

    /** Creates a specific backend; exposed for tests and the JavaFX app. */
    public static ItemRepository create(String backend, String dbUrl) {
        String choice = backend == null ? "" : backend.trim().toLowerCase();

        if (choice.isEmpty() || choice.equals("csv")) {
            CsvStorage csv = new CsvStorage();
            LOGGER.info("Storage backend: " + csv.describe());
            return csv;
        }

        if (choice.equals("jdbc") || choice.equals("h2") || choice.equals("sql")) {
            String url = (dbUrl == null || dbUrl.isBlank()) ? DEFAULT_DB_URL : dbUrl.trim();
            JdbcStorage jdbc = new JdbcStorage(url, System.getenv("LEDGER_DB_USER"),
                    System.getenv("LEDGER_DB_PASSWORD"));
            LOGGER.info("Storage backend: " + jdbc.describe());
            return jdbc;
        }

        throw new IllegalArgumentException("Unknown storage backend '" + backend
                + "'. Use 'csv' (default) or 'jdbc'.");
    }

    private static String resolve(String propertyName, String envName, String fallback) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            value = System.getenv(envName);
        }
        return (value == null || value.isBlank()) ? fallback : value;
    }
}

