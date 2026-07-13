package kroviq.wrapper.upload;

import kroviq.wrapper.core.FileUploadHandler;
import kroviq.wrapper.factory.GenericFileUploadHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GenericFileUploadHandlerTest {

    @Mock
    private WebDriver driver;

    @Mock
    private WebElement targetElement;

    @Mock
    private WebElement fileInputElement;

    @Mock
    private WebElement parentElement;

    @TempDir
    Path tempDir;

    private GenericFileUploadHandler handler;
    private File testFile;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        handler = new GenericFileUploadHandler();
        testFile = tempDir.resolve("test-upload.txt").toFile();
        Files.writeString(testFile.toPath(), "test content");
    }

    // --- Interface contract tests ---

    @Test
    void implementsFileUploadHandler() {
        assertInstanceOf(FileUploadHandler.class, handler);
    }

    // --- uploadFile: Strategy 1 - target is input[type=file] ---

    @Test
    void uploadFile_targetIsFileInput_sendKeysDirectly() {
        when(targetElement.getTagName()).thenReturn("input");
        when(targetElement.getAttribute("type")).thenReturn("file");

        handler.uploadFile(driver, targetElement, testFile.getAbsolutePath());

        verify(targetElement).sendKeys(testFile.getAbsolutePath());
    }

    // --- uploadFile: Strategy 2 - input[type=file] within target ---

    @Test
    void uploadFile_fileInputWithinTarget_findsAndUsesIt() {
        when(targetElement.getTagName()).thenReturn("div");
        when(targetElement.getAttribute("type")).thenReturn(null);
        when(targetElement.findElements(By.cssSelector("input[type='file']")))
                .thenReturn(List.of(fileInputElement));
        when(fileInputElement.getTagName()).thenReturn("input");
        when(fileInputElement.getAttribute("type")).thenReturn("file");

        WebDriver jsDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        when(targetElement.findElements(By.cssSelector("input[type='file']")))
                .thenReturn(List.of(fileInputElement));

        handler.uploadFile(jsDriver, targetElement, testFile.getAbsolutePath());

        verify(fileInputElement).sendKeys(testFile.getAbsolutePath());
    }

    // --- uploadFile: Strategy 3 - ancestor resolution ---

    @Test
    void uploadFile_fileInputInAncestor_findsAndUsesIt() {
        when(targetElement.getTagName()).thenReturn("button");
        when(targetElement.getAttribute("type")).thenReturn(null);
        when(targetElement.findElements(By.cssSelector("input[type='file']")))
                .thenReturn(Collections.emptyList());

        WebElement ancestor = mock(WebElement.class);
        when(targetElement.findElement(By.xpath("./ancestor::*[.//input[@type='file']][1]")))
                .thenReturn(ancestor);
        when(ancestor.findElements(By.cssSelector("input[type='file']")))
                .thenReturn(List.of(fileInputElement));

        WebDriver jsDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));

        handler.uploadFile(jsDriver, targetElement, testFile.getAbsolutePath());

        verify(fileInputElement).sendKeys(testFile.getAbsolutePath());
    }

    // --- uploadFile: file not found ---

    @Test
    void uploadFile_fileNotFound_throwsException() {
        when(targetElement.getTagName()).thenReturn("input");
        when(targetElement.getAttribute("type")).thenReturn("file");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                handler.uploadFile(driver, targetElement, "/nonexistent/path/file.txt"));

        assertTrue(ex.getMessage().contains("Upload file not found"));
    }

    // --- uploadFile: null/empty path ---

    @Test
    void uploadFile_nullPath_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                handler.uploadFile(driver, targetElement, null));
    }

    @Test
    void uploadFile_emptyPath_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                handler.uploadFile(driver, targetElement, "   "));
    }

    // --- uploadFile: no mechanism found ---

    @Test
    void uploadFile_noInputAndNotDropZone_throwsException() {
        when(targetElement.getTagName()).thenReturn("div");
        when(targetElement.getAttribute("type")).thenReturn(null);
        when(targetElement.getAttribute("class")).thenReturn("some-random-class");
        when(targetElement.getAttribute("role")).thenReturn(null);
        when(targetElement.findElements(By.cssSelector("input[type='file']")))
                .thenReturn(Collections.emptyList());
        when(targetElement.findElement(any(By.class)))
                .thenThrow(new org.openqa.selenium.NoSuchElementException("not found"));
        when(driver.findElements(By.cssSelector("input[type='file']")))
                .thenReturn(Collections.emptyList());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                handler.uploadFile(driver, targetElement, testFile.getAbsolutePath()));

        assertTrue(ex.getMessage().contains("Unable to resolve file upload mechanism"));
    }

    // --- uploadFiles: multi-file ---

    @Test
    void uploadFiles_multipleFiles_sendsNewlineSeparatedPaths() throws IOException {
        File file1 = tempDir.resolve("file1.txt").toFile();
        File file2 = tempDir.resolve("file2.txt").toFile();
        Files.writeString(file1.toPath(), "content1");
        Files.writeString(file2.toPath(), "content2");

        when(targetElement.getTagName()).thenReturn("input");
        when(targetElement.getAttribute("type")).thenReturn("file");
        when(targetElement.getAttribute("multiple")).thenReturn("true");

        handler.uploadFiles(driver, targetElement, List.of(file1.getAbsolutePath(), file2.getAbsolutePath()));

        String expected = file1.getAbsolutePath() + "\n" + file2.getAbsolutePath();
        verify(targetElement).sendKeys(expected);
    }

    @Test
    void uploadFiles_singleFile_delegatesToUploadFile() {
        when(targetElement.getTagName()).thenReturn("input");
        when(targetElement.getAttribute("type")).thenReturn("file");

        handler.uploadFiles(driver, targetElement, List.of(testFile.getAbsolutePath()));

        verify(targetElement).sendKeys(testFile.getAbsolutePath());
    }

    @Test
    void uploadFiles_emptyList_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                handler.uploadFiles(driver, targetElement, Collections.emptyList()));
    }

    @Test
    void uploadFiles_nullList_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                handler.uploadFiles(driver, targetElement, null));
    }

    // --- isDropZone ---

    @Test
    void isDropZone_classContainsDropzone_returnsTrue() {
        when(targetElement.getAttribute("class")).thenReturn("custom-dropzone-area");
        assertTrue(handler.isDropZone(targetElement));
    }

    @Test
    void isDropZone_classContainsUploadDrag_returnsTrue() {
        when(targetElement.getAttribute("class")).thenReturn("ant-upload-drag");
        assertTrue(handler.isDropZone(targetElement));
    }

    @Test
    void isDropZone_classContainsFileDrop_returnsTrue() {
        when(targetElement.getAttribute("class")).thenReturn("file-drop-container");
        assertTrue(handler.isDropZone(targetElement));
    }

    @Test
    void isDropZone_classContainsDragDrop_returnsTrue() {
        when(targetElement.getAttribute("class")).thenReturn("drag-drop-area");
        assertTrue(handler.isDropZone(targetElement));
    }

    @Test
    void isDropZone_regularElement_returnsFalse() {
        when(targetElement.getAttribute("class")).thenReturn("regular-div");
        when(targetElement.getAttribute("role")).thenReturn(null);
        assertFalse(handler.isDropZone(targetElement));
    }

    @Test
    void isDropZone_nullElement_returnsFalse() {
        assertFalse(handler.isDropZone(null));
    }

    @Test
    void isDropZone_roleButtonWithDragText_returnsTrue() {
        when(targetElement.getAttribute("class")).thenReturn("upload-area");
        when(targetElement.getAttribute("role")).thenReturn("button");
        when(targetElement.getText()).thenReturn("Drag files here");
        assertTrue(handler.isDropZone(targetElement));
    }

    // --- makeInputVisible (tested indirectly via uploadFile) ---

    @Test
    void uploadFile_hiddenInput_makesVisibleBeforeSendKeys() {
        when(targetElement.getTagName()).thenReturn("input");
        when(targetElement.getAttribute("type")).thenReturn("file");

        WebDriver jsDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));

        handler.uploadFile(jsDriver, targetElement, testFile.getAbsolutePath());

        verify((JavascriptExecutor) jsDriver).executeScript(anyString(), eq(targetElement));
        verify(targetElement).sendKeys(testFile.getAbsolutePath());
    }

    // --- Page-level fallback (single input on page) ---

    @Test
    void uploadFile_singlePageInput_usesAsFallback() {
        when(targetElement.getTagName()).thenReturn("button");
        when(targetElement.getAttribute("type")).thenReturn(null);
        when(targetElement.getAttribute("class")).thenReturn("some-button");
        when(targetElement.getAttribute("role")).thenReturn(null);
        when(targetElement.findElements(By.cssSelector("input[type='file']")))
                .thenReturn(Collections.emptyList());
        when(targetElement.findElement(any(By.class)))
                .thenThrow(new org.openqa.selenium.NoSuchElementException("not found"));

        WebElement pageLevelInput = mock(WebElement.class);
        WebDriver jsDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        when(jsDriver.findElements(By.cssSelector("input[type='file']")))
                .thenReturn(List.of(pageLevelInput));

        handler.uploadFile(jsDriver, targetElement, testFile.getAbsolutePath());

        verify(pageLevelInput).sendKeys(testFile.getAbsolutePath());
    }
}
