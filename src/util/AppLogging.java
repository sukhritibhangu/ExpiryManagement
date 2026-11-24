package util;

import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Configures {@code java.util.logging} once per JVM so the console app and the
 * web server print short, readable log lines instead of the default two-line format.
 */
public final class AppLogging {

    private static boolean configured;

    private AppLogging() {
        // utility class
    }

    /** Idempotent setup; safe to call from every entry point. */
    public static synchronized void configure() {
        if (configured) {
            return;
        }
        configured = true;

        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);

        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }

        ConsoleHandler console = new ConsoleHandler();
        console.setLevel(Level.INFO);
        console.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(LogRecord record) {
                return String.format("%1$tF %1$tT [%2$s] %3$s - %4$s%n",
                        new Date(record.getMillis()),
                        record.getLevel().getName(),
                        record.getLoggerName(),
                        formatMessage(record));
            }
        });
        root.addHandler(console);
    }
}
