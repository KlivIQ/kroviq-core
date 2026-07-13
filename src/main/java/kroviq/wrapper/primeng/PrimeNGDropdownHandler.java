package kroviq.wrapper.primeng;

import kroviq.wrapper.core.DropdownHandler;
import kroviq.wrapper.core.HandlerUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class PrimeNGDropdownHandler implements DropdownHandler {

    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("primeng");
    private static final String OVERLAY_PANEL = ".p-dropdown-panel[style*='display: block'], .p-dropdown-panel[style*='display:block'], .p-dropdown-panel.p-dropdown-panel-visible, .p-overlay-panel, p-dropdown-panel";
    private static final String ITEMS_WRAPPER = ".p-dropdown-items";
    private static final String OPTION_SELECTOR = ".p-dropdown-item, li.p-dropdown-item, p-dropdownitem li";
    private static final String FILTER_INPUT = ".p-dropdown-filter";

    @Override
    public void select(WebDriver driver, WebElement trigger, String visibleText) {
        openDropdown(driver, trigger);
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);

        WebElement panel = waitForPanel(driver, trigger, wait);

        // Use filter if available
        List<WebElement> filterInputs = panel.findElements(By.cssSelector(FILTER_INPUT));
        if (!filterInputs.isEmpty() && filterInputs.get(0).isDisplayed()) {
            filterInputs.get(0).clear();
            filterInputs.get(0).sendKeys(visibleText);
            wait.until(d -> !panel.findElements(By.cssSelector(OPTION_SELECTOR)).isEmpty());
        }

        WebElement option = HandlerUtils.findOptionByText(panel, OPTION_SELECTOR, visibleText, false);
        if (option == null) {
            throw new org.openqa.selenium.NoSuchElementException(
                    "PrimeNG dropdown option '" + visibleText + "' not found");
        }

        HandlerUtils.scrollIntoViewAndClick(driver, option);
    }

    @Override
    public List<String> getOptions(WebDriver driver, WebElement trigger) {
        openDropdown(driver, trigger);
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);

        WebElement panel = waitForPanel(driver, trigger, wait);
        wait.until(d -> !panel.findElements(By.cssSelector(OPTION_SELECTOR)).isEmpty());

        List<WebElement> options = panel.findElements(By.cssSelector(OPTION_SELECTOR));
        List<String> texts = options.stream()
                .map(opt -> opt.getText().trim())
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());

        // Close by clicking trigger again
        trigger.click();
        return texts;
    }

    private WebElement waitForPanel(WebDriver driver, WebElement trigger, WebDriverWait wait) {
        // Try global panel first (PrimeNG appends overlay to body in real apps)
        try {
            return wait.withTimeout(Duration.ofSeconds(2)).until(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector(OVERLAY_PANEL)));
        } catch (Exception e) { /* try within trigger */ }

        // Fallback: panel nested inside the trigger element
        return wait.withTimeout(TIMEOUT).until(d -> {
            List<WebElement> panels = trigger.findElements(By.cssSelector(".p-dropdown-panel"));
            for (WebElement p : panels) {
                try { if (p.isDisplayed()) return p; } catch (Exception ex) { /* next */ }
            }
            return null;
        });
    }

    private void openDropdown(WebDriver driver, WebElement trigger) {
        String tagName = trigger.getTagName().toLowerCase();
        if ("p-dropdown".equals(tagName)) {
            List<WebElement> innerTriggers = trigger.findElements(By.cssSelector(".p-dropdown-trigger, .p-dropdown-label, .p-dropdown"));
            if (!innerTriggers.isEmpty()) {
                // Click the first visible inner element
                for (WebElement inner : innerTriggers) {
                    try {
                        if (inner.isDisplayed()) {
                            inner.click();
                            return;
                        }
                    } catch (Exception e) { /* try next */ }
                }
            }
        }
        trigger.click();
    }


}
