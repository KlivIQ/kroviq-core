package kroviq.wrapper.factory;

import kroviq.wrapper.core.DialogHandler;
import kroviq.wrapper.core.HandlerUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;

public class GenericDialogHandler implements DialogHandler {

    @Override
    public boolean isOpen(WebDriver driver) {
        List<WebElement> dialogs = driver.findElements(By.cssSelector("[role='dialog']"));
        return dialogs.stream().anyMatch(WebElement::isDisplayed);
    }

    @Override
    public WebElement getDialogContent(WebDriver driver) {
        Duration timeout = HandlerUtils.getTimeoutDuration("generic");
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[role='dialog']")));
    }

    @Override
    public void close(WebDriver driver) {
        WebElement dialog = getDialogContent(driver);
        dialog.sendKeys(Keys.ESCAPE);
    }

    @Override
    public void clickAction(WebDriver driver, String buttonText) {
        WebElement dialog = getDialogContent(driver);
        WebElement button = dialog.findElement(
                By.xpath(".//button[normalize-space()='" + buttonText + "']"));
        button.click();
    }
}
