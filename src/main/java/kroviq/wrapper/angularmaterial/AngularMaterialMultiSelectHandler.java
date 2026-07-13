package kroviq.wrapper.angularmaterial;

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

public class AngularMaterialMultiSelectHandler implements MultiSelectHandler {

    private static final Logger logger = LogManager.getLogger(AngularMaterialMultiSelectHandler.class);
    private static final String OVERLAY_PANEL = ".cdk-overlay-container .mat-mdc-select-panel, .cdk-overlay-container .mat-select-panel, .mat-mdc-select-panel.open, .mat-mdc-select-panel";
    private static final String OPTION_SELECTOR = "mat-option, .mat-mdc-option";
    private final WebDriver driver;

    public AngularMaterialMultiSelectHandler(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void selectMultiple(WebDriver driver, WebElement trigger, List<String> values) {
        if (values == null || values.isEmpty()) return;

        int timeout = HandlerUtils.getTimeout("angularmaterial");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));

        trigger.click();
        WebElement panel = waitForPanel(driver, wait);

        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            WebElement option = findOptionInPanel(panel, value);
            if (option == null) {
                throw new RuntimeException("[Angular Material MultiSelect] Option '" + value + "' not found");
            }
            HandlerUtils.scrollIntoViewAndClick(driver, option);
            logger.info("[Angular Material MultiSelect] Selected {}/{}: '{}'", i + 1, values.size(), value);
        }

        closePanel(driver, panel);
        logger.info("[Angular Material MultiSelect] Completed: {} values selected", values.size());
    }

    @Override
    public void deselectValue(WebDriver driver, WebElement trigger, String value) {
        int timeout = HandlerUtils.getTimeout("angularmaterial");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));

        trigger.click();
        WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(OVERLAY_PANEL)));

        WebElement option = findOptionInPanel(panel, value);
        if (option != null) {
            HandlerUtils.scrollIntoViewAndClick(driver, option);
            logger.info("[Angular Material MultiSelect] Deselected: '{}'", value);
        }
        closePanel(driver, panel);
    }

    @Override
    public void deselectAll(WebDriver driver, WebElement trigger) {
        int timeout = HandlerUtils.getTimeout("angularmaterial");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));

        trigger.click();
        WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(OVERLAY_PANEL)));

        List<WebElement> selectedOptions = panel.findElements(By.cssSelector(
                "mat-option[aria-selected='true'], .mat-mdc-option[aria-selected='true']"));
        for (WebElement opt : selectedOptions) {
            HandlerUtils.scrollIntoViewAndClick(driver, opt);
        }
        closePanel(driver, panel);
        logger.info("[Angular Material MultiSelect] Deselected all ({} items)", selectedOptions.size());
    }

    @Override
    public List<String> getSelectedValues(WebDriver driver, WebElement trigger) {
        // Angular Material renders selected values as mat-chip or comma-separated text
        List<WebElement> chips = trigger.findElements(By.cssSelector(
                "mat-chip, .mat-mdc-chip, .mat-chip"));
        List<String> values = new ArrayList<>();
        for (WebElement chip : chips) {
            String text = chip.getText().trim();
            if (!text.isEmpty() && !text.equals("×") && !text.equals("cancel")) values.add(text);
        }
        if (values.isEmpty()) {
            // Fallback: read trigger text and split on comma
            String triggerText = trigger.getText().trim();
            if (!triggerText.isEmpty()) {
                for (String part : triggerText.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) values.add(trimmed);
                }
            }
        }
        return values;
    }

    private WebElement waitForPanel(WebDriver driver, WebDriverWait wait) {
        return wait.until(d -> {
            List<WebElement> panels = d.findElements(By.cssSelector(OVERLAY_PANEL));
            for (WebElement p : panels) {
                try {
                    if (p.isDisplayed()) return p;
                } catch (Exception e) { /* next */ }
            }
            return null;
        });
    }

    private WebElement findOptionInPanel(WebElement panel, String value) {
        List<WebElement> options = panel.findElements(By.cssSelector(OPTION_SELECTOR));
        for (WebElement opt : options) {
            String text = opt.getText().trim();
            if (text.equals(value) || text.equalsIgnoreCase(value)) return opt;
        }
        return null;
    }

    private void closePanel(WebDriver driver, WebElement panel) {
        try {
            // Click backdrop to close
            List<WebElement> backdrops = driver.findElements(By.cssSelector(".cdk-overlay-backdrop"));
            if (!backdrops.isEmpty() && backdrops.get(0).isDisplayed()) {
                backdrops.get(0).click();
            } else {
                driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
            }
        } catch (Exception e) {
            logger.debug("[Angular Material MultiSelect] Panel close fallback triggered");
        }
    }
}
