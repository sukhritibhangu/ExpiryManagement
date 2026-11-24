package service;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class LabelScanner {
    private static final long MAX_BYTES = 12L * 1024 * 1024;
    private static final DateTimeFormatter DAY_FIRST = new DateTimeFormatterBuilder()
            .appendPattern("d/M/uuuu").toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern DATE = Pattern.compile("\\b(?:\\d{4}-\\d{2}-\\d{2}|\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{4})\\b");
    private static final Pattern BATCH = Pattern.compile("(?im)\\b(?:batch|lot)\\s*(?:no\\.?|#)?\\s*[:#-]?\\s+([\\w-]+)");

    public record Result(String name, LocalDate manufactured, LocalDate expires, String batch, String text) { }

    public Result scan(Path image) throws IOException, InterruptedException {
        if (!Files.isRegularFile(image) || Files.size(image) == 0 || Files.size(image) > MAX_BYTES) {
            throw new IllegalArgumentException("Choose an image between 1 byte and 12 MB.");
        }
        verifyImageDimensions(image);
        String executable = System.getenv().getOrDefault("LEDGER_TESSERACT", "tesseract");
        Path output = Files.createTempFile("ledger-ocr-", ".txt");
        Process process = null;
        try {
            try {
                process = new ProcessBuilder(executable, image.toAbsolutePath().toString(),
                        "stdout", "-l", "eng", "--psm", "11")
                        .redirectOutput(output.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            } catch (IOException e) {
                throw new IOException("Install Tesseract OCR and set LEDGER_TESSERACT to its executable path.", e);
            }
            if (!process.waitFor(30, TimeUnit.SECONDS)) throw new IOException("OCR timed out. Try a smaller image.");
            if (process.exitValue() != 0) throw new IOException("OCR could not read this image. Try a clear PNG or JPEG.");
            return interpret(Files.readString(output));
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(output);
        }
    }

    private static void verifyImageDimensions(Path path) throws IOException {
        try (var input = ImageIO.createImageInputStream(path.toFile())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Choose a valid PNG, JPEG or TIFF label image.");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 25_000_000) {
                    throw new IOException("Choose an image under 25 megapixels.");
                }
            } finally {
                reader.dispose();
            }
        }
    }

    public static Result interpret(String text) {
        if (text.isBlank()) throw new IllegalArgumentException("No text was detected. Try a brighter, sharper image.");
        String[] lines = text.strip().split("\\R+");
        LocalDate expiry = null;
        LocalDate manufactured = null;
        String name = "";
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.matches(".*\\b(exp(?:iry|ires|iration)?|best before|use by)\\b.*")) {
                expiry = nearbyDate(lines, i);
            } else if (lower.matches(".*\\b(mfg|mfd|manufactured|packed)\\b.*")) {
                manufactured = nearbyDate(lines, i);
            } else if (name.isEmpty() && !DATE.matcher(line).find() && !BATCH.matcher(line).find()
                    && line.chars().anyMatch(Character::isLetter) && line.length() <= 200) {
                name = line;
            }
        }
        var batch = BATCH.matcher(text);
        return new Result(name, manufactured, expiry, batch.find() ? batch.group(1) : "", text);
    }

    private static LocalDate nearbyDate(String[] lines, int index) {
        var match = DATE.matcher(lines[index]);
        if (!match.find()) {
            if (index + 1 == lines.length) return null;
            match = DATE.matcher(lines[index + 1].strip());
            if (!match.lookingAt()) return null;
        }
        String value = match.group();
        try {
            if (value.matches("\\d{4}-.*")) return LocalDate.parse(value);
            return LocalDate.parse(value.replace('.', '/').replace('-', '/'), DAY_FIRST);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
