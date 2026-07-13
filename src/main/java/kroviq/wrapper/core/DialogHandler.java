package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public interface DialogHandler {

    boolean isOpen(WebDriver driver);

    WebElement getDialogContent(WebDriver driver);

    void close(WebDriver driver);

    void clickAction(WebDriver driver, String buttonText);
}
