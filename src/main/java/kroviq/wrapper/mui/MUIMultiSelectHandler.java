package kroviq.wrapper.mui;

import kroviq.wrapper.core.HandlerUtils;
import kroviq.wrapper.core.MultiSelectHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MUIMultiSelectHandler implements MultiSelectHandler {

    private static final Logger logger = LogManager.getLogger(MUIMultiSelectHandler.class);
    private final WebDriver driver;

    public MUIMultiSelectHandler(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void selectMultiple(WebDriver driver, WebElement trigger, List<String> values) {
        if (values == null || values.isEmpty()) return;

        int timeout = HandlerUtils.getTimeout("mui");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));

        openPanel(driver, trigger, wait);

        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            WebElement option = findOption(driver, value, wait);
            if (option == null) {
                throw new RuntimeException("[MUI MultiSelect] Option '" + value + "' not found");
            }
            HandlerUtils.scrollIntoViewAndClick(driver, option);
            logger.info("[MUI MultiSelect] Selected {}/{}: '{}'", i + 1, values.size(), value);
        }

        closePanel(driver);
        logger.info("[MUI MultiSelect] Completed: {} values selected", values.size());
    }

    @Override
    public void deselectValue(WebDriver driver, WebElement trigger, String value) {
        // MUI chips have a delete icon (SVG or button with data-tag-index)
        List<WebElement> chips = trigger.findElements(By.cssSelector(
                ".MuiChip-root, [class*='MuiChip']"));
        for (WebElement chip : chips) {
            if (chip.getText().trim().contains(value)) {
                List<WebElement> deleteIcons = chip.findElements(By.cssSelector(
                        ".MuiChip-deleteIcon, [data-testid*='Cancel'], svg"));
                if (!deleteIcons.isEmpty()) {
                    deleteIcons.get(0).click();
                    logger.info("[MUI MultiSelect] Deselected: '{}'", value);
                    return;
                }
            }
        }
        // Fallback: open panel and click to deselect (toggle behavior)
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(HandlerUtils.getTimeout("mui")));
        openPanel(driver, trigger, wait);
        WebElement option = findOption(driver, value, wait);
        if (option != null) {
            HandlerUtils.scrollIntoViewAndClick(driver, option);
            logger.info("[MUI MultiSelect] Deselected via toggle: '{}'", value);
        }
        closePanel(driver);
    }

    @Override
    public void deselectAll(WebDriver driver, WebElement trigger) {
        // MUI clear button (endAdornment with clear icon)
        List<WebElement> clearBtns = trigger.findElements(By.cssSelector(
                "[aria-label='Clear'], button[title='Clear'], .MuiAutocomplete-clearIndicator"));
        if (!clearBtns.isEmpty() && clearBtns.get(0).isDisplayed()) {
            clearBtns.get(0).click();
            logger.info("[MUI MultiSelect] Deselected all via clear button");
            return;
        }
        // Fallback: remove chips one by one
        List<WebElement> deleteIcons = trigger.findElements(By.cssSelector(
                ".MuiChip-deleteIcon, [data-testid*='Cancel']"));
        for (WebElement icon : deleteIcons) {
            try { icon.click(); } catch (Exception e) { /* continue */ }
        }
        logger.info("[MUI MultiSelect] Deselected all via chip removal");
    }

    @Override
    public List<String> getSelectedValues(WebDriver driver, WebElement trigger) {
        List<WebElement> chips = trigger.findElements(By.cssSelector(
                ".MuiChip-label, .MuiChip-root span[class*='label']"));
        List<String> values = new ArrayList<>();
        for (WebElement chip : chips) {
            String text = chip.getText().trim();
            if (!text.isEmpty()) values.add(text);
        }
        if (values.isEmpty()) {
            // Fallback: check for tag-style display
            List<WebElement> tags = trigger.findElements(By.cssSelector(
                    "[class*='tag'], [class*='chip'], [role='button']"));
            for (WebElement tag : tags) {
                String text = tag.getText().trim();
                if (!text.isEmpty() && !text.equals("×") && !text.equals("✕")) values.add(text);
            }
        }
        return values;
    }

    private void openPanel(WebDriver driver, WebElement trigger, WebDriverWait wait) {
        // Try clicking the input/trigger area
        List<WebElement> inputs = trigger.findElements(By.cssSelector("input"));
        if (!inputs.isEmpty()) {
            inputs.get(0).click();
        } else {
            trigger.click();
        }
        wait.until(d -> !d.findElements(By.cssSelector(
                "[role='listbox'], .MuiAutocomplete-popper, .MuiPopover-root [role='listbox']")).isEmpty());
    }

    private WebElement findOption(WebDriver driver, String value, WebDriverWait wait) {
        // MUI multi-select options use role="option"
        List<WebElement> options = driver.findElements(By.cssSelector("[role='option']"));
        for (WebElement opt : options) {
            String text = opt.getText().trim();
            if (text.equals(value) || text.equalsIgnoreCase(value)) return opt;
        }
        // Scroll to find (virtualized list support)
        int maxScroll = HandlerUtils.getMaxScrollAttempts("mui");
        List<WebElement> listboxes = driver.findElements(By.cssSelector("[role='listbox']"));
        if (!listboxes.isEmpty()) {
            WebElement listbox = listboxes.get(0);
            for (int i = 0; i < maxScroll; i++) {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollTop += 200;", listbox);
                try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                options = driver.findElements(By.cssSelector("[role='option']"));
                for (WebElement opt : options) {
                    if (opt.getText().trim().equals(value) || opt.getText().trim().equalsIgnoreCase(value)) return opt;
                }
            }
        }
        return null;
    }

    private void closePanel(WebDriver driver) {
        try {
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        } catch (Exception e) {
            logger.debug("[MUI MultiSelect] Escape close failed, panel may already be closed");
        }
    }
}
