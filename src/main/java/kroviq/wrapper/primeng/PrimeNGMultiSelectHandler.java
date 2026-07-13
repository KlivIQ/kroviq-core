package kroviq.wrapper.primeng;

import kroviq.wrapper.core.HandlerUtils;
import kroviq.wrapper.core.MultiSelectHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PrimeNGMultiSelectHandler implements MultiSelectHandler {

    private static final Logger logger = LogManager.getLogger(PrimeNGMultiSelectHandler.class);
    private static final String PANEL_SELECTOR = ".p-multiselect-panel, .p-multiselect-items-wrapper";
    private static final String ITEM_SELECTOR = ".p-multiselect-item, li.p-multiselect-item";
    private final WebDriver driver;

    public PrimeNGMultiSelectHandler(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void selectMultiple(WebDriver driver, WebElement trigger, List<String> values) {
        if (values == null || values.isEmpty()) return;

        int timeout = HandlerUtils.getTimeout("primeng");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));

        openPanel(driver, trigger, wait);
        WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(PANEL_SELECTOR)));

        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            WebElement item = findItem(panel, value);
            if (item == null) {
                throw new RuntimeException("[PrimeNG MultiSelect] Option '" + value + "' not found");
            }
            HandlerUtils.scrollIntoViewAndClick(driver, item);
            logger.info("[PrimeNG MultiSelect] Selected {}/{}: '{}'", i + 1, values.size(), value);
        }

        closePanel(driver, trigger);
        logger.info("[PrimeNG MultiSelect] Completed: {} values selected", values.size());
    }

    @Override
    public void deselectValue(WebDriver driver, WebElement trigger, String value) {
        // PrimeNG renders chips with close icons
        List<WebElement> chips = trigger.findElements(By.cssSelector(
                ".p-multiselect-token, .p-chip"));
        for (WebElement chip : chips) {
            if (chip.getText().trim().contains(value)) {
                List<WebElement> closeIcons = chip.findElements(By.cssSelector(
                        ".p-multiselect-token-icon, .p-chip-remove-icon, .pi-times-circle"));
                if (!closeIcons.isEmpty()) {
                    closeIcons.get(0).click();
                    logger.info("[PrimeNG MultiSelect] Deselected: '{}'", value);
                    return;
                }
            }
        }
        // Fallback: open panel and uncheck
        int timeout = HandlerUtils.getTimeout("primeng");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        openPanel(driver, trigger, wait);
        WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(PANEL_SELECTOR)));
        WebElement item = findItem(panel, value);
        if (item != null) {
            HandlerUtils.scrollIntoViewAndClick(driver, item);
            logger.info("[PrimeNG MultiSelect] Deselected via toggle: '{}'", value);
        }
        closePanel(driver, trigger);
    }

    @Override
    public void deselectAll(WebDriver driver, WebElement trigger) {
        int timeout = HandlerUtils.getTimeout("primeng");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        openPanel(driver, trigger, wait);

        // PrimeNG multiselect has a "select all" checkbox in header — uncheck it
        List<WebElement> headerCheckboxes = driver.findElements(By.cssSelector(
                ".p-multiselect-header .p-checkbox, .p-multiselect-select-all"));
        if (!headerCheckboxes.isEmpty()) {
            WebElement checkbox = headerCheckboxes.get(0);
            // If checked, click to uncheck (deselect all)
            String ariaChecked = checkbox.getAttribute("aria-checked");
            if ("true".equals(ariaChecked) || checkbox.getAttribute("class").contains("p-highlight")) {
                checkbox.click();
                logger.info("[PrimeNG MultiSelect] Deselected all via header checkbox");
            }
        }
        closePanel(driver, trigger);
    }

    @Override
    public List<String> getSelectedValues(WebDriver driver, WebElement trigger) {
        // Prefer token-label (inner text only) to avoid counting token wrapper
        List<WebElement> tokens = trigger.findElements(By.cssSelector(".p-multiselect-token-label"));
        List<String> values = new ArrayList<>();
        for (WebElement token : tokens) {
            String text = token.getText().trim();
            if (!text.isEmpty()) values.add(text);
        }
        if (values.isEmpty()) {
            // Fallback: p-chip-text or token wrapper
            tokens = trigger.findElements(By.cssSelector(".p-chip-text, .p-multiselect-token"));
            for (WebElement token : tokens) {
                String text = token.getText().trim();
                if (!text.isEmpty() && !text.equals("×") && !text.equals("✕")) values.add(text);
            }
        }
        if (values.isEmpty()) {
            List<WebElement> labels = trigger.findElements(By.cssSelector(".p-multiselect-label"));
            if (!labels.isEmpty()) {
                String labelText = labels.get(0).getText().trim();
                if (!labelText.isEmpty() && !labelText.contains("Select")) {
                    for (String part : labelText.split(",")) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty()) values.add(trimmed);
                    }
                }
            }
        }
        return values;
    }

    private void openPanel(WebDriver driver, WebElement trigger, WebDriverWait wait) {
        List<WebElement> triggerBtns = trigger.findElements(By.cssSelector(
                ".p-multiselect-trigger, .p-multiselect-label"));
        if (!triggerBtns.isEmpty()) {
            triggerBtns.get(0).click();
        } else {
            trigger.click();
        }
    }

    private WebElement findItem(WebElement panel, String value) {
        List<WebElement> items = panel.findElements(By.cssSelector(ITEM_SELECTOR));
        for (WebElement item : items) {
            String text = item.getText().trim();
            if (text.equals(value) || text.equalsIgnoreCase(value)) return item;
        }
        // Partial match fallback
        for (WebElement item : items) {
            if (item.getText().trim().contains(value)) return item;
        }
        return null;
    }

    private void closePanel(WebDriver driver, WebElement trigger) {
        try {
            List<WebElement> closeButtons = driver.findElements(By.cssSelector(
                    ".p-multiselect-close, .p-multiselect-header .p-multiselect-close-icon"));
            if (!closeButtons.isEmpty() && closeButtons.get(0).isDisplayed()) {
                closeButtons.get(0).click();
            } else {
                driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
            }
        } catch (Exception e) {
            logger.debug("[PrimeNG MultiSelect] Panel close fallback triggered");
        }
    }
}
