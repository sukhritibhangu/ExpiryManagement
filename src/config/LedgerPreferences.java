package config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public final class LedgerPreferences {
    private final Path file;
    private final Set<String> read = new HashSet<>();
    private int reminder = 7;
    private boolean reduced;
    private String profile = "Local workspace";

    public LedgerPreferences(Path file) throws IOException {
        this.file = file;
        if (file == null || !Files.exists(file)) return;
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }
        String days = properties.getProperty("reminder", "7");
        try {
            reminder = Math.max(0, Math.min(365, Integer.parseInt(days)));
        } catch (NumberFormatException ignored) {
            reminder = 7;
        }
        reduced = Boolean.parseBoolean(properties.getProperty("reducedMotion"));
        profile = properties.getProperty("profile", profile);
        String keys = properties.getProperty("read", "");
        if (!keys.isBlank()) read.addAll(java.util.List.of(keys.split(",")));
    }

    public int reminder() { return reminder; }
    public boolean reduced() { return reduced; }
    public String profile() { return profile; }
    public boolean read(String key) { return read.contains(key); }

    public void update(int threshold, boolean reducedMotion, String name) throws IOException {
        save(threshold, reducedMotion, name, read);
        reminder = threshold;
        reduced = reducedMotion;
        profile = name;
    }

    public void markRead(Set<String> keys) throws IOException {
        var updated = new HashSet<>(read);
        updated.addAll(keys);
        save(reminder, reduced, profile, updated);
        read.addAll(keys);
    }

    private void save(int threshold, boolean reducedMotion, String name, Set<String> keys) throws IOException {
        if (file == null) return;
        var properties = new Properties();
        properties.setProperty("reminder", String.valueOf(threshold));
        properties.setProperty("reducedMotion", String.valueOf(reducedMotion));
        properties.setProperty("profile", name);
        properties.setProperty("read", String.join(",", keys));
        Path target = file.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), "ledger-profile-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temp)) {
                properties.store(writer, "Expiry Alert Ledger preferences");
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
