package kroviq.wrapper.mui;

import kroviq.wrapper.core.DialogHandler;
import kroviq.wrapper.core.HandlerUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;

public class MUIDialogHandler implements DialogHandler {

    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("mui");
    private static final String ACTIVE_DIALOG_SELECTOR = ".MuiDialog-root.MuiModal-root:not(.MuiModal-hidden)";
    private static final String CONTENT_PRIMARY = ".MuiDialogContent-root";
    private static final String CONTENT_FALLBACK = ".MuiDialog-container [role='dialog']";
    private static final String ACTIONS_SELECTOR = ".MuiDialogActions-root";

    @Override
    public boolean isOpen(WebDriver driver) {
        List<WebElement> dialogs = driver.findElements(By.cssSelector(ACTIVE_DIALOG_SELECTOR));
        return dialogs.stream().anyMatch(d -> {
            try {
                return d.isDisplayed();
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    public WebElement getDialogContent(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(ACTIVE_DIALOG_SELECTOR)));

        // Try primary content selector
        List<WebElement> contents = driver.findElements(By.cssSelector(CONTENT_PRIMARY));
        for (WebElement content : contents) {
            try {
                if (content.isDisplayed()) return content;
            } catch (StaleElementReferenceException e) { /* try next */ }
        }

        // Fallback: dialog container with role='dialog'
        List<WebElement> fallbacks = driver.findElements(By.cssSelector(CONTENT_FALLBACK));
        for (WebElement fb : fallbacks) {
            try {
                if (fb.isDisplayed()) return fb;
            } catch (StaleElementReferenceException e) { /* try next */ }
        }

        // Last resort: return the active dialog root itself
        return wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(ACTIVE_DIALOG_SELECTOR)));
    }

    @Override
    public void close(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(ACTIVE_DIALOG_SELECTOR)));

        // Try close button first
        List<WebElement> closeButtons = driver.findElements(
                By.cssSelector("[aria-label='close'], [data-testid='CloseIcon']"));
        for (WebElement btn : closeButtons) {
            try {
                if (btn.isDisplayed()) {
                    HandlerUtils.jsClick(driver, btn);
                    wait.until(d -> !isOpen(d));
                    return;
                }
            } catch (Exception e) { /* try next strategy */ }
        }

        // Send ESC via Actions API — more reliable than element.sendKeys for MUI
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        wait.until(d -> !isOpen(d));
    }

    @Override
    public void clickAction(WebDriver driver, String buttonText) {
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(ACTIVE_DIALOG_SELECTOR)));

        WebElement button = findActionButton(driver, buttonText);

        try {
            button.click();
        } catch (ElementNotInteractableException e) {
            HandlerUtils.jsClick(driver, button);
        }
    }

    private WebElement findActionButton(WebDriver driver, String buttonText) {
        // Search in dialog actions area
        List<WebElement> actionAreas = driver.findElements(By.cssSelector(ACTIONS_SELECTOR));
        for (WebElement area : actionAreas) {
            try {
                if (!area.isDisplayed()) continue;
                List<WebElement> buttons = area.findElements(
                        By.xpath(".//button[normalize-space()='" + buttonText + "']"));
                if (!buttons.isEmpty()) return buttons.get(0);
            } catch (Exception e) { /* try next */ }
        }

        // Fallback: search in any active dialog
        List<WebElement> dialogs = driver.findElements(By.cssSelector(ACTIVE_DIALOG_SELECTOR));
        for (WebElement dialog : dialogs) {
            try {
                List<WebElement> buttons = dialog.findElements(
                        By.xpath(".//button[normalize-space()='" + buttonText + "']"));
                if (!buttons.isEmpty()) return buttons.get(0);
            } catch (Exception e) { /* try next */ }
        }

        throw new NoSuchElementException("Button '" + buttonText + "' not found in MUI dialog");
    }
}
