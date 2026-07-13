package kroviq.utils.download;

import kroviq.utils.FileDownloadVerifier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FileDownloadVerifierTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        System.setProperty("download.directory", tempDir.toString());
        // Reset ThreadLocal by initializing fresh
        FileDownloadVerifier.initDownloadDirectory();
    }

    @AfterEach
    void tearDown() {
        FileDownloadVerifier.deleteDownloadDirectory();
        System.clearProperty("download.directory");
    }

    // --- Directory Management ---

    @Test
    void initDownloadDirectory_createsDirectory() {
        Path dir = FileDownloadVerifier.getDownloadDirectory();
        assertTrue(Files.exists(dir));
        assertTrue(Files.isDirectory(dir));
    }

    @Test
    void getDownloadDirectory_returnsSameForSameThread() {
        Path dir1 = FileDownloadVerifier.getDownloadDirectory();
        Path dir2 = FileDownloadVerifier.getDownloadDirectory();
        assertEquals(dir1, dir2);
    }

    @Test
    void parallelThreads_getDifferentDirectories() throws Exception {
        AtomicReference<Path> thread1Dir = new AtomicReference<>();
        AtomicReference<Path> thread2Dir = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            System.setProperty("download.directory", tempDir.toString());
            thread1Dir.set(FileDownloadVerifier.initDownloadDirectory());
            latch.countDown();
        });
        Thread t2 = new Thread(() -> {
            System.setProperty("download.directory", tempDir.toString());
            thread2Dir.set(FileDownloadVerifier.initDownloadDirectory());
            latch.countDown();
        });

        t1.start();
        t2.start();
        latch.await();

        assertNotNull(thread1Dir.get());
        assertNotNull(thread2Dir.get());
        assertNotEquals(thread1Dir.get(), thread2Dir.get(),
                "Parallel threads must have isolated download directories");
    }

    // --- waitForDownload ---

    @Test
    void waitForDownload_fileExists_returnsImmediately() throws IOException {
        Path dir = FileDownloadVerifier.getDownloadDirectory();
        File testFile = dir.resolve("report.pdf").toFile();
        Files.writeString(testFile.toPath(), "PDF content");

        File result = FileDownloadVerifier.waitForDownload("report.pdf", Duration.ofSeconds(2));
        assertEquals("report.pdf", result.getName());
        assertTrue(result.exists());
    }

    @Test
    void waitForDownload_fileAppearsLater_waitsAndReturns() throws Exception {
        Path dir = FileDownloadVerifier.getDownloadDirectory();
        File testFile = dir.resolve("delayed.xlsx").toFile();

        // Simulate file appearing after 500ms
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException e) { return; }
            try { Files.writeString(testFile.toPath(), "Excel data"); } catch (IOException e) { throw new RuntimeException(e); }
        }).start();

        File result = FileDownloadVerifier.waitForDownload("delayed.xlsx", Duration.ofSeconds(5));
        assertEquals("delayed.xlsx", result.getName());
    }

    @Test
    void waitForDownload_fileNotFound_throwsException() {
        assertThrows(RuntimeException.class, () ->
                FileDownloadVerifier.waitForDownload("nonexistent.pdf", Duration.ofSeconds(1)));
    }

    @Test
    void waitForDownload_inProgressFile_waitsForCompletion() throws Exception {
        Path dir = FileDownloadVerifier.getDownloadDirectory();
        // Create .crdownload file first (in-progress)
        File crdownload = dir.resolve("data.csv.crdownload").toFile();
        Files.writeString(crdownload.toPath(), "partial");

        // After 500ms, rename to final file
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException e) { return; }
            crdownload.delete();
            try { Files.writeString(dir.resolve("data.csv"), "complete data"); } catch (IOException e) { throw new RuntimeException(e); }
        }).start();

        File result = FileDownloadVerifier.waitForDownload("data.csv", Duration.ofSeconds(5));
        assertEquals("data.csv", result.getName());
    }

    // --- waitForDownloadMatching ---

    @Test
    void waitForDownloadMatching_regexMatch_returnsFile() throws IOException {
        Path dir = FileDownloadVerifier.getDownloadDirectory();
        Files.writeString(dir.resolve("report_20260610_143000.pdf"), "content");

        File result = FileDownloadVerifier.waitForDownloadMatching("report_\\d+_\\d+\\.pdf", Duration.ofSeconds(2));
        assertTrue(result.getName().startsWith("report_"));
        assertTrue(result.getName().endsWith(".pdf"));
    }

    @Test
    void waitForDownloadMatching_noMatch_throwsException() {
        assertThrows(RuntimeException.class, () ->
                FileDownloadVerifier.waitForDownloadMatching("nomatch_.*\\.zip", Duration.ofSeconds(1)));
    }

    // --- Validation Methods ---

    @Test
    void verifyFileExists_existingFile_passes() throws IOException {
        Path dir = FileDownloadVerifier.getDownloadDirectory();
        Files.writeString(dir.resolve("exists.txt"), "content");
        assertDoesNotThrow(() -> FileDownloadVerifier.verifyFileExists("exists.txt"));
    }

    @Test
    void verifyFileExists_missingFile_throwsAssertion() {
        assertThrows(AssertionError.class, () ->
                FileDownloadVerifier.verifyFileExists("missing.txt"));
    }

    @Test
    void verifyFilename_exactMatch_passes() throws IOException {
        File file = tempDir.resolve("test.pdf").toFile();
        Files.writeString(file.toPath(), "x");
        assertDoesNotThrow(() -> FileDownloadVerifier.verifyFilename(file, "test.pdf"));
    }

    @Test
    void verifyFilename_mismatch_throwsAssertion() throws IOException {
        File file = tempDir.resolve("actual.pdf").toFile();
        Files.writeString(file.toPath(), "x");
        assertThrows(AssertionError.class, () ->
                FileDownloadVerifier.verifyFilename(file, "expected.pdf"));
    }

    @Test
    void verifyExtension_pdf_passes() throws IOException {
        File file = tempDir.resolve("report.pdf").toFile();
        Files.writeString(file.toPath(), "x");
        assertDoesNotThrow(() -> FileDownloadVerifier.verifyExtension(file, "pdf"));
        assertDoesNotThrow(() -> FileDownloadVerifier.verifyExtension(file, ".pdf"));
    }

    @Test
    void verifyExtension_wrong_throwsAssertion() throws IOException {
        File file = tempDir.resolve("data.csv").toFile();
        Files.writeString(file.toPath(), "x");
        assertThrows(AssertionError.class, () ->
                FileDownloadVerifier.verifyExtension(file, "xlsx"));
    }

    @Test
    void verifyFileSize_withinRange_passes() throws IOException {
        File file = tempDir.resolve("sized.bin").toFile();
        Files.write(file.toPath(), new byte[1024]);
        assertDoesNotThrow(() -> FileDownloadVerifier.verifyFileSize(file, 100, 2048));
    }

    @Test
    void verifyFileSize_tooSmall_throwsAssertion() throws IOException {
        File file = tempDir.resolve("tiny.bin").toFile();
        Files.write(file.toPath(), new byte[10]);
        assertThrows(AssertionError.class, () ->
                FileDownloadVerifier.verifyFileSize(file, 100, 2048));
    }

    @Test
    void verifyFileSize_tooLarge_throwsAssertion() throws IOException {
        File file = tempDir.resolve("big.bin").toFile();
        Files.write(file.toPath(), new byte[5000]);
        assertThrows(AssertionError.class, () ->
                FileDownloadVerifier.verifyFileSize(file, 100, 2048));
    }

    @Test
    void verifyFileNotEmpty_nonEmpty_passes() throws IOException {
        File file = tempDir.resolve("notempty.txt").toFile();
        Files.writeString(file.toPath(), "content");
        assertDoesNotThrow(() -> FileDownloadVerifier.verifyFileNotEmpty(file));
    }

    @Test
    void verifyFileNotEmpty_empty_throwsAssertion() throws IOException {
        File file = tempDir.resolve("empty.txt").toFile();
        file.createNewFile();
        assertThrows(AssertionError.class, () -> FileDownloadVerifier.verifyFileNotEmpty(file));
    }

    // --- Cleanup ---

    @Test
    void cleanDownloadDirectory_removesFiles() throws IOException {
        Path dir = FileDownloadVerifier.getDownloadDirectory();
        Files.writeString(dir.resolve("file1.txt"), "a");
        Files.writeString(dir.resolve("file2.txt"), "b");

        FileDownloadVerifier.cleanDownloadDirectory();

        File[] remaining = dir.toFile().listFiles();
        assertNotNull(remaining);
        assertEquals(0, remaining.length);
    }

    @Test
    void deleteDownloadDirectory_removesDirectory() {
        Path dir = FileDownloadVerifier.getDownloadDirectory();
        assertTrue(Files.exists(dir));

        FileDownloadVerifier.deleteDownloadDirectory();
        assertFalse(Files.exists(dir));
    }
}
