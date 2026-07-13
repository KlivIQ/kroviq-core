package kroviq.wrapper.factory;

import kroviq.wrapper.core.FileUploadHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class GenericFileUploadHandler implements FileUploadHandler {

    private static final Logger logger = LogManager.getLogger(GenericFileUploadHandler.class);

    @Override
    public void uploadFile(WebDriver driver, WebElement target, String filePath) {
        String resolvedPath = resolveFilePath(filePath);
        validateFileExists(resolvedPath);

        WebElement fileInput = resolveFileInput(driver, target);
        if (fileInput != null) {
            makeInputVisible(driver, fileInput);
            fileInput.sendKeys(resolvedPath);
            logger.info("File uploaded via input[type=file]: {}", resolvedPath);
            return;
        }

        if (isDropZone(target)) {
            uploadViaDropZone(driver, target, resolvedPath);
            logger.info("File uploaded via drop zone: {}", resolvedPath);
            return;
        }

        throw new RuntimeException("Unable to resolve file upload mechanism for element. "
                + "No input[type=file] found and element is not a recognized drop zone.");
    }

    @Override
    public void uploadFiles(WebDriver driver, WebElement target, List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            throw new IllegalArgumentException("File path list cannot be null or empty");
        }

        if (filePaths.size() == 1) {
            uploadFile(driver, target, filePaths.get(0));
            return;
        }

        WebElement fileInput = resolveFileInput(driver, target);
        if (fileInput == null) {
            throw new RuntimeException("Multi-file upload requires an input[type=file] element. "
                    + "Drop zone multi-file upload is not supported.");
        }

        String multipleAttr = fileInput.getAttribute("multiple");
        if (multipleAttr == null) {
            logger.warn("input[type=file] does not have 'multiple' attribute. Multi-file upload may fail.");
        }

        StringBuilder combinedPaths = new StringBuilder();
        for (String path : filePaths) {
            String resolved = resolveFilePath(path);
            validateFileExists(resolved);
            if (combinedPaths.length() > 0) {
                combinedPaths.append("\n");
            }
            combinedPaths.append(resolved);
        }

        makeInputVisible(driver, fileInput);
        fileInput.sendKeys(combinedPaths.toString());
        logger.info("Uploaded {} files via input[type=file]", filePaths.size());
    }

    @Override
    public boolean isDropZone(WebElement element) {
        if (element == null) return false;
        try {
            String className = element.getAttribute("class");
            if (className != null) {
                String lower = className.toLowerCase();
                if (lower.contains("dropzone") || lower.contains("drop-zone")
                        || lower.contains("upload-drag") || lower.contains("ant-upload-drag")
                        || lower.contains("file-drop") || lower.contains("drag-drop")) {
                    return true;
                }
            }

            String role = element.getAttribute("role");
            if ("button".equals(role) || "presentation".equals(role)) {
                String text = element.getText();
                if (text != null) {
                    String lower = text.toLowerCase();
                    if (lower.contains("drag") || lower.contains("drop") || lower.contains("browse")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("isDropZone check failed: {}", e.getMessage());
        }
        return false;
    }

    private WebElement resolveFileInput(WebDriver driver, WebElement target) {
        // Strategy 1: Target itself is input[type=file]
        if (isFileInput(target)) {
            return target;
        }

        // Strategy 2: Find input[type=file] within the target element
        List<WebElement> inputs = target.findElements(By.cssSelector("input[type='file']"));
        if (!inputs.isEmpty()) {
            return inputs.get(0);
        }

        // Strategy 3: Look in parent/ancestor wrappers (AntD, MUI, PrimeNG pattern)
        try {
            WebElement ancestor = target.findElement(
                    By.xpath("./ancestor::*[.//input[@type='file']][1]"));
            List<WebElement> ancestorInputs = ancestor.findElements(By.cssSelector("input[type='file']"));
            if (!ancestorInputs.isEmpty()) {
                return ancestorInputs.get(0);
            }
        } catch (Exception e) {
            // No ancestor with file input found
        }

        // Strategy 4: Search siblings and nearby DOM (upload button pattern)
        try {
            WebElement parent = target.findElement(By.xpath("./.."));
            List<WebElement> siblingInputs = parent.findElements(By.cssSelector("input[type='file']"));
            if (!siblingInputs.isEmpty()) {
                return siblingInputs.get(0);
            }
        } catch (Exception e) {
            // No sibling file input found
        }

        // Strategy 5: Page-level hidden input associated via label/for or recent DOM addition
        List<WebElement> pageInputs = driver.findElements(By.cssSelector("input[type='file']"));
        if (pageInputs.size() == 1) {
            logger.debug("Found single input[type=file] on page, using it as fallback");
            return pageInputs.get(0);
        }

        return null;
    }

    private boolean isFileInput(WebElement element) {
        try {
            String tagName = element.getTagName();
            if (!"input".equalsIgnoreCase(tagName)) return false;
            String type = element.getAttribute("type");
            return "file".equalsIgnoreCase(type);
        } catch (Exception e) {
            return false;
        }
    }

    private void makeInputVisible(WebDriver driver, WebElement fileInput) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                    "arguments[0].style.display = 'block';" +
                    "arguments[0].style.visibility = 'visible';" +
                    "arguments[0].style.opacity = '1';" +
                    "arguments[0].style.height = 'auto';" +
                    "arguments[0].style.width = 'auto';" +
                    "arguments[0].style.position = 'relative';",
                    fileInput);
        } catch (Exception e) {
            logger.debug("Could not make file input visible: {}", e.getMessage());
        }
    }

    private void uploadViaDropZone(WebDriver driver, WebElement dropZone, String filePath) {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Create a temporary input[type=file], set file, dispatch drop event
        String script =
                "var input = document.createElement('input');" +
                "input.type = 'file';" +
                "input.style.display = 'none';" +
                "document.body.appendChild(input);" +
                "input.setAttribute('data-kroviq-temp-upload', 'true');" +
                "return input;";

        WebElement tempInput = (WebElement) js.executeScript(script);
        tempInput.sendKeys(filePath);

        // Dispatch drag/drop events with the file
        String dropScript =
                "var input = arguments[0];" +
                "var target = arguments[1];" +
                "var file = input.files[0];" +
                "var dataTransfer = new DataTransfer();" +
                "dataTransfer.items.add(file);" +
                "var dragEnter = new DragEvent('dragenter', {dataTransfer: dataTransfer, bubbles: true});" +
                "var dragOver = new DragEvent('dragover', {dataTransfer: dataTransfer, bubbles: true});" +
                "var drop = new DragEvent('drop', {dataTransfer: dataTransfer, bubbles: true});" +
                "target.dispatchEvent(dragEnter);" +
                "target.dispatchEvent(dragOver);" +
                "target.dispatchEvent(drop);" +
                "input.remove();";

        js.executeScript(dropScript, tempInput, dropZone);
    }

    private String resolveFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        Path path = Paths.get(filePath.trim());

        // If absolute, return as-is
        if (path.isAbsolute()) {
            return path.toAbsolutePath().toString();
        }

        // Resolve relative paths against working directory
        return path.toAbsolutePath().toString();
    }

    private void validateFileExists(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("Upload file not found: " + filePath);
        }
        if (!file.isFile()) {
            throw new RuntimeException("Upload path is not a file: " + filePath);
        }
        if (!file.canRead()) {
            throw new RuntimeException("Upload file is not readable: " + filePath);
        }
    }
}
