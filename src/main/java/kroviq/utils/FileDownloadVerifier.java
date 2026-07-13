package kroviq.utils;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.regex.Pattern;

public class FileDownloadVerifier {

    private static final Logger logger = LogManager.getLogger(FileDownloadVerifier.class);
    private static final ThreadLocal<Path> downloadDirectory = new ThreadLocal<>();

    private static final int DEFAULT_TIMEOUT_SECONDS = LoadProperties.getInt("download.timeout.seconds", 30);
    private static final int POLL_INTERVAL_MS = LoadProperties.getInt("download.poll.interval.ms", 500);

    private FileDownloadVerifier() {}

    // --- Download Directory Management ---

    public static Path initDownloadDirectory() {
        String basePath = LoadProperties.get("download.directory", "target/downloads");
        String threadId = Thread.currentThread().getName() + "_" + Thread.currentThread().getId();
        Path dir = Paths.get(basePath, threadId).toAbsolutePath();

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create download directory: " + dir, e);
        }

        downloadDirectory.set(dir);
        logger.info("[Download] Directory initialized: {}", dir);
        return dir;
    }

    public static Path getDownloadDirectory() {
        Path dir = downloadDirectory.get();
        if (dir == null) {
            dir = initDownloadDirectory();
        }
        return dir;
    }

    public static String getDownloadDirectoryString() {
        return getDownloadDirectory().toString();
    }

    // --- Download Completion Detection ---

    public static File waitForDownload(String expectedFilename) {
        return waitForDownload(expectedFilename, Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
    }

    public static File waitForDownload(String expectedFilename, Duration timeout) {
        Path dir = getDownloadDirectory();
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        logger.info("[Download] Waiting for file '{}' in {}", expectedFilename, dir);

        while (System.currentTimeMillis() < deadline) {
            File target = dir.resolve(expectedFilename).toFile();
            if (target.exists() && !isDownloadInProgress(target)) {
                logger.info("[Download] File ready: {} ({}bytes)", target.getName(), target.length());
                return target;
            }
            sleep(POLL_INTERVAL_MS);
        }

        throw new RuntimeException("[Download] File '" + expectedFilename + "' not found within "
                + timeout.toSeconds() + "s in: " + dir);
    }

    public static File waitForDownloadMatching(String filenamePattern) {
        return waitForDownloadMatching(filenamePattern, Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
    }

    public static File waitForDownloadMatching(String filenamePattern, Duration timeout) {
        Path dir = getDownloadDirectory();
        Pattern pattern = Pattern.compile(filenamePattern);
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        logger.info("[Download] Waiting for file matching '{}' in {}", filenamePattern, dir);

        while (System.currentTimeMillis() < deadline) {
            File[] files = dir.toFile().listFiles();
            if (files != null) {
                for (File f : files) {
                    if (pattern.matcher(f.getName()).matches() && !isDownloadInProgress(f)) {
                        logger.info("[Download] File matched: {} ({}bytes)", f.getName(), f.length());
                        return f;
                    }
                }
            }
            sleep(POLL_INTERVAL_MS);
        }

        throw new RuntimeException("[Download] No file matching '" + filenamePattern + "' found within "
                + timeout.toSeconds() + "s in: " + dir);
    }

    // --- File Validation ---

    public static void verifyFileExists(String filename) {
        Path dir = getDownloadDirectory();
        File file = dir.resolve(filename).toFile();
        if (!file.exists()) {
            throw new AssertionError("[Download] File does not exist: " + file.getAbsolutePath());
        }
        logger.info("[Download] File exists: {}", filename);
    }

    public static void verifyFilename(File file, String expectedName) {
        if (!file.getName().equals(expectedName)) {
            throw new AssertionError(String.format(
                    "[Download] Filename mismatch: expected '%s', actual '%s'", expectedName, file.getName()));
        }
        logger.info("[Download] Filename verified: {}", expectedName);
    }

    public static void verifyFilenameContains(File file, String substring) {
        if (!file.getName().contains(substring)) {
            throw new AssertionError(String.format(
                    "[Download] Filename '%s' does not contain '%s'", file.getName(), substring));
        }
    }

    public static void verifyExtension(File file, String expectedExtension) {
        String ext = expectedExtension.startsWith(".") ? expectedExtension : "." + expectedExtension;
        if (!file.getName().toLowerCase().endsWith(ext.toLowerCase())) {
            throw new AssertionError(String.format(
                    "[Download] Extension mismatch: expected '%s', actual file '%s'", ext, file.getName()));
        }
        logger.info("[Download] Extension verified: {}", ext);
    }

    public static void verifyFileSize(File file, long minBytes, long maxBytes) {
        long size = file.length();
        if (size < minBytes) {
            throw new AssertionError(String.format(
                    "[Download] File too small: %d bytes (minimum: %d)", size, minBytes));
        }
        if (maxBytes > 0 && size > maxBytes) {
            throw new AssertionError(String.format(
                    "[Download] File too large: %d bytes (maximum: %d)", size, maxBytes));
        }
        logger.info("[Download] File size verified: {} bytes (range: {}-{})", size, minBytes, maxBytes > 0 ? maxBytes : "unlimited");
    }

    public static void verifyFileSizeMinimum(File file, long minBytes) {
        verifyFileSize(file, minBytes, -1);
    }

    public static void verifyFileNotEmpty(File file) {
        if (file.length() == 0) {
            throw new AssertionError("[Download] File is empty: " + file.getName());
        }
        logger.info("[Download] File is not empty: {} ({} bytes)", file.getName(), file.length());
    }

    // --- Cleanup ---

    public static void cleanDownloadDirectory() {
        Path dir = downloadDirectory.get();
        if (dir == null) return;

        try {
            File dirFile = dir.toFile();
            if (dirFile.exists()) {
                FileUtils.cleanDirectory(dirFile);
                logger.info("[Download] Directory cleaned: {}", dir);
            }
        } catch (IOException e) {
            logger.warn("[Download] Failed to clean directory: {}", e.getMessage());
        }
    }

    public static void deleteDownloadDirectory() {
        Path dir = downloadDirectory.get();
        if (dir == null) return;

        try {
            File dirFile = dir.toFile();
            if (dirFile.exists()) {
                FileUtils.deleteDirectory(dirFile);
                logger.info("[Download] Directory deleted: {}", dir);
            }
        } catch (IOException e) {
            logger.warn("[Download] Failed to delete directory: {}", e.getMessage());
        } finally {
            downloadDirectory.remove();
        }
    }

    // --- Helpers ---

    private static boolean isDownloadInProgress(File file) {
        String name = file.getName().toLowerCase();
        // Chrome uses .crdownload, Firefox uses .part, Edge uses .partial
        return name.endsWith(".crdownload") || name.endsWith(".part")
                || name.endsWith(".partial") || name.endsWith(".tmp");
    }

    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
