package kroviq.wrapper.factory;

import kroviq.wrapper.core.HandlerUtils;
import kroviq.wrapper.core.MultiSelectHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GenericMultiSelectHandler implements MultiSelectHandler {

    private static final Logger logger = LogManager.getLogger(GenericMultiSelectHandler.class);
    private final WebDriver driver;

    public GenericMultiSelectHandler(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void selectMultiple(WebDriver driver, WebElement trigger, List<String> values) {
        if (values == null || values.isEmpty()) return;

        // Strategy 1: Native HTML <select multiple>
        if ("select".equalsIgnoreCase(trigger.getTagName())) {
            Select select = new Select(trigger);
            for (String value : values) {
                select.selectByVisibleText(value);
                logger.info("[Generic MultiSelect] Selected via native select: '{}'", value);
            }
            return;
        }

        // Strategy 2: ARIA-based multi-select (role=listbox with multi-select)
        int timeout = HandlerUtils.getTimeout("generic");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));

        trigger.click();
        wait.until(d -> !d.findElements(By.cssSelector("[role='listbox'], [role='option']")).isEmpty());

        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            WebElement option = findAriaOption(driver, value);
            if (option == null) {
                throw new RuntimeException("[Generic MultiSelect] Option '" + value + "' not found");
            }
            HandlerUtils.scrollIntoViewAndClick(driver, option);
            logger.info("[Generic MultiSelect] Selected {}/{}: '{}'", i + 1, values.size(), value);
        }

        // Close by pressing Escape
        driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        logger.info("[Generic MultiSelect] Completed: {} values selected", values.size());
    }

    @Override
    public void deselectValue(WebDriver driver, WebElement trigger, String value) {
        if ("select".equalsIgnoreCase(trigger.getTagName())) {
            Select select = new Select(trigger);
            select.deselectByVisibleText(value);
            logger.info("[Generic MultiSelect] Deselected via native select: '{}'", value);
            return;
        }
        // ARIA: click to toggle
        trigger.click();
        WebElement option = findAriaOption(driver, value);
        if (option != null) {
            HandlerUtils.scrollIntoViewAndClick(driver, option);
            logger.info("[Generic MultiSelect] Deselected via toggle: '{}'", value);
        }
        driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
    }

    @Override
    public void deselectAll(WebDriver driver, WebElement trigger) {
        if ("select".equalsIgnoreCase(trigger.getTagName())) {
            Select select = new Select(trigger);
            select.deselectAll();
            logger.info("[Generic MultiSelect] Deselected all via native select");
            return;
        }
        // ARIA: no standard deselect-all pattern
        logger.warn("[Generic MultiSelect] deselectAll not supported for non-native selects");
    }

    @Override
    public List<String> getSelectedValues(WebDriver driver, WebElement trigger) {
        if ("select".equalsIgnoreCase(trigger.getTagName())) {
            Select select = new Select(trigger);
            return select.getAllSelectedOptions().stream()
                    .map(WebElement::getText)
                    .map(String::trim)
                    .collect(Collectors.toList());
        }
        // ARIA: look for selected options or chips
        List<WebElement> selected = trigger.findElements(By.cssSelector(
                "[aria-selected='true'], [class*='selected'], [class*='chip'], [class*='tag']"));
        List<String> values = new ArrayList<>();
        for (WebElement el : selected) {
            String text = el.getText().trim();
            if (!text.isEmpty()) values.add(text);
        }
        return values;
    }

    private WebElement findAriaOption(WebDriver driver, String value) {
        List<WebElement> options = driver.findElements(By.cssSelector("[role='option']"));
        for (WebElement opt : options) {
            String text = opt.getText().trim();
            if (text.equals(value) || text.equalsIgnoreCase(value)) return opt;
        }
        return null;
    }
}
