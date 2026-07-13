package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.List;

public interface FileUploadHandler {

    void uploadFile(WebDriver driver, WebElement target, String filePath);

    void uploadFiles(WebDriver driver, WebElement target, List<String> filePaths);

    boolean isDropZone(WebElement element);
}
