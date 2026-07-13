package kroviq.wrapper.primeng;

import kroviq.wrapper.core.DialogHandler;
import kroviq.wrapper.core.HandlerUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;

public class PrimeNGDialogHandler implements DialogHandler {

    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("primeng");
    private static final String DIALOG_CONTAINER = ".p-dialog, p-dialog .p-dialog";
    private static final String DIALOG_CONTENT = ".p-dialog-content";
    private static final String DIALOG_FOOTER = ".p-dialog-footer";
    private static final String CLOSE_BUTTON = ".p-dialog-header-close, .p-dialog-header-icon";

    @Override
    public boolean isOpen(WebDriver driver) {
        List<WebElement> dialogs = driver.findElements(By.cssSelector(DIALOG_CONTAINER));
        return dialogs.stream().anyMatch(d -> {
            try { return d.isDisplayed(); }
            catch (Exception e) { return false; }
        });
    }

    @Override
    public WebElement getDialogContent(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(DIALOG_CONTAINER)));

        List<WebElement> contents = driver.findElements(By.cssSelector(DIALOG_CONTENT));
        for (WebElement content : contents) {
            try { if (content.isDisplayed()) return content; }
            catch (StaleElementReferenceException e) { /* try next */ }
        }

        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(DIALOG_CONTAINER)));
    }

    @Override
    public void close(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(DIALOG_CONTAINER)));

        // Try close button in header
        List<WebElement> closeButtons = driver.findElements(By.cssSelector(CLOSE_BUTTON));
        for (WebElement btn : closeButtons) {
            try {
                if (btn.isDisplayed()) {
                    btn.click();
                    wait.until(d -> !isOpen(d));
                    return;
                }
            } catch (Exception e) { /* try ESC */ }
        }

        // ESC key
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2)).until(d -> !isOpen(d));
            return;
        } catch (Exception e) { /* JS fallback */ }

        ((JavascriptExecutor) driver).executeScript(
                "document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', bubbles: true}));");
        wait.until(d -> !isOpen(d));
    }

    @Override
    public void clickAction(WebDriver driver, String buttonText) {
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(DIALOG_CONTAINER)));

        // Search in dialog footer first
        List<WebElement> footers = driver.findElements(By.cssSelector(DIALOG_FOOTER));
        for (WebElement footer : footers) {
            try {
                if (!footer.isDisplayed()) continue;
                List<WebElement> buttons = footer.findElements(
                        By.xpath(".//button[normalize-space()='" + buttonText + "']"));
                if (!buttons.isEmpty()) {
                    HandlerUtils.clickSafe(driver, buttons.get(0));
                    return;
                }
            } catch (Exception e) { /* try container */ }
        }

        // Fallback: search entire dialog
        List<WebElement> containers = driver.findElements(By.cssSelector(DIALOG_CONTAINER));
        for (WebElement container : containers) {
            try {
                if (!container.isDisplayed()) continue;
                List<WebElement> buttons = container.findElements(
                        By.xpath(".//button[normalize-space()='" + buttonText + "']"));
                if (!buttons.isEmpty()) {
                    HandlerUtils.clickSafe(driver, buttons.get(0));
                    return;
                }
            } catch (Exception e) { /* try next */ }
        }

        throw new NoSuchElementException("Button '" + buttonText + "' not found in PrimeNG dialog");
    }
}
