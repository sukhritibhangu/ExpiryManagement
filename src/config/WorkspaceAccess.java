package config;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class WorkspaceAccess implements AutoCloseable {
    private final Path directory;
    private final FileChannel lease;

    private WorkspaceAccess(Path directory, FileChannel lease) {
        this.directory = directory;
        this.lease = lease;
    }

    public static String generateKey() {
        byte[] token = new byte[32];
        new SecureRandom().nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    public static WorkspaceAccess open(Path root, String key, boolean create) throws IOException {
        if (!key.matches("[A-Za-z0-9_-]{43}")) throw new IOException("Enter a valid workspace key.");
        Path directory = root.resolve(hash(key));
        if (create) {
            Files.createDirectories(root);
            Files.createDirectory(directory);
        } else if (!Files.isDirectory(directory)) {
            throw new IOException("No workspace matches that key.");
        }
        FileChannel lease = FileChannel.open(directory.resolve("workspace.lock"),
                StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        try {
            if (lease.tryLock() == null) throw new IOException("This workspace is already open.");
            return new WorkspaceAccess(directory, lease);
        } catch (OverlappingFileLockException e) {
            lease.close();
            throw new IOException("This workspace is already open.", e);
        } catch (IOException | RuntimeException e) {
            lease.close();
            throw e;
        }
    }

    public Path directory() { return directory; }

    @Override public void close() throws IOException { lease.close(); }

    private static String hash(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
