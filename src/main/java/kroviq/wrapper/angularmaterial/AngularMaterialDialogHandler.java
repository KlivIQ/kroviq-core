package kroviq.wrapper.angularmaterial;

import kroviq.wrapper.core.DialogHandler;
import kroviq.wrapper.core.HandlerUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;

public class AngularMaterialDialogHandler implements DialogHandler {

    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("angularmaterial");
    private static final String DIALOG_CONTAINER = "mat-dialog-container, .mat-mdc-dialog-container";
    private static final String DIALOG_CONTENT = "mat-dialog-content, [mat-dialog-content], .mat-mdc-dialog-content";
    private static final String DIALOG_ACTIONS = "mat-dialog-actions, [mat-dialog-actions], .mat-mdc-dialog-actions";

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

        // Fallback: return the dialog container itself
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(DIALOG_CONTAINER)));
    }

    @Override
    public void close(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(DIALOG_CONTAINER)));

        // Try close button first
        List<WebElement> closeButtons = driver.findElements(
                By.cssSelector(DIALOG_CONTAINER + " [aria-label='Close'], " + DIALOG_CONTAINER + " button.close"));
        for (WebElement btn : closeButtons) {
            try {
                if (btn.isDisplayed()) {
                    btn.click();
                    wait.until(d -> !isOpen(d));
                    return;
                }
            } catch (Exception e) { /* try ESC */ }
        }

        // Send ESC via Actions API
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2)).until(d -> !isOpen(d));
            return;
        } catch (Exception e) { /* try JS fallback */ }

        // JS fallback for ESC dispatch
        ((JavascriptExecutor) driver).executeScript(
                "document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', bubbles: true}));");
        wait.until(d -> !isOpen(d));
    }

    @Override
    public void clickAction(WebDriver driver, String buttonText) {
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(DIALOG_CONTAINER)));

        // Search in dialog actions area
        List<WebElement> actionAreas = driver.findElements(By.cssSelector(DIALOG_ACTIONS));
        for (WebElement area : actionAreas) {
            try {
                if (!area.isDisplayed()) continue;
                List<WebElement> buttons = area.findElements(
                        By.xpath(".//button[normalize-space()='" + buttonText + "']"));
                if (!buttons.isEmpty()) {
                    HandlerUtils.clickSafe(driver, buttons.get(0));
                    return;
                }
            } catch (Exception e) { /* try container fallback */ }
        }

        // Fallback: search entire dialog container
        List<WebElement> containers = driver.findElements(By.cssSelector(DIALOG_CONTAINER));
        for (WebElement container : containers) {
            try {
                List<WebElement> buttons = container.findElements(
                        By.xpath(".//button[normalize-space()='" + buttonText + "']"));
                if (!buttons.isEmpty()) {
                    HandlerUtils.clickSafe(driver, buttons.get(0));
                    return;
                }
            } catch (Exception e) { /* try next */ }
        }

        throw new NoSuchElementException("Button '" + buttonText + "' not found in Angular Material dialog");
    }
}
