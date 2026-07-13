package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public interface CascaderHandler {

    void selectPath(WebDriver driver, WebElement cascaderElement, String path);

    String getSelectedPath(WebDriver driver, WebElement cascaderElement);

    void clearSelection(WebDriver driver, WebElement cascaderElement);
}
