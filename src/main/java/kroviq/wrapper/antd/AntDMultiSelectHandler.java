package kroviq.wrapper.antd;

import kroviq.wrapper.AntDUtils;
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

public class AntDMultiSelectHandler implements MultiSelectHandler {

    private static final Logger logger = LogManager.getLogger(AntDMultiSelectHandler.class);
    private final WebDriver driver;

    public AntDMultiSelectHandler(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void selectMultiple(WebDriver driver, WebElement trigger, List<String> values) {
        if (values == null || values.isEmpty()) return;

        int timeout = HandlerUtils.getTimeout("antd");
        AntDUtils antDUtils = new AntDUtils(driver, timeout);
        WebElement clickTarget = resolveClickTarget(trigger);

        clickTarget.click();

        try {
            antDUtils.selectAntDOptionByText(values.get(0), "multiselect", trigger);
            logger.info("[AntD MultiSelect] Selected 1/{}: '{}'", values.size(), values.get(0));

            for (int i = 1; i < values.size(); i++) {
                waitForChip(clickTarget, values.get(i - 1));
                ensurePanelOpen(driver, clickTarget);
                WebElement panel = antDUtils.findVisiblePanel();
                antDUtils.selectOptionFromOpenPanel(panel, values.get(i), "multiselect");
                logger.info("[AntD MultiSelect] Selected {}/{}: '{}'", i + 1, values.size(), values.get(i));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("[AntD MultiSelect] Selection interrupted", e);
        }

        logger.info("[AntD MultiSelect] Completed: {} values selected", values.size());
    }

    @Override
    public void deselectValue(WebDriver driver, WebElement trigger, String value) {
        List<WebElement> chips = trigger.findElements(
                By.xpath(".//span[contains(@class,'ant-select-selection-item')]"));
        for (WebElement chip : chips) {
            if (chip.getText().trim().equals(value) || chip.getText().trim().contains(value)) {
                List<WebElement> removeIcons = chip.findElements(
                        By.cssSelector(".ant-select-selection-item-remove, .anticon-close"));
                if (!removeIcons.isEmpty()) {
                    removeIcons.get(0).click();
                    logger.info("[AntD MultiSelect] Deselected: '{}'", value);
                    return;
                }
            }
        }
        logger.warn("[AntD MultiSelect] Chip '{}' not found for deselection", value);
    }

    @Override
    public void deselectAll(WebDriver driver, WebElement trigger) {
        List<WebElement> clearBtns = trigger.findElements(
                By.cssSelector(".ant-select-clear, .anticon-close-circle"));
        if (!clearBtns.isEmpty() && clearBtns.get(0).isDisplayed()) {
            clearBtns.get(0).click();
            logger.info("[AntD MultiSelect] Deselected all via clear button");
            return;
        }
        List<WebElement> chips = trigger.findElements(
                By.cssSelector(".ant-select-selection-item-remove, .anticon-close"));
        for (WebElement chip : chips) {
            try { chip.click(); } catch (Exception e) { /* continue */ }
        }
        logger.info("[AntD MultiSelect] Deselected all via chip removal");
    }

    @Override
    public List<String> getSelectedValues(WebDriver driver, WebElement trigger) {
        // Prefer specific content spans to avoid counting wrapper + content double
        List<WebElement> chips = trigger.findElements(
                By.cssSelector(".ant-select-selection-item-content"));
        if (chips.isEmpty()) {
            chips = trigger.findElements(By.cssSelector(".ant-select-selection-item"));
        }
        List<String> values = new ArrayList<>();
        for (WebElement chip : chips) {
            String text = chip.getText().trim();
            if (!text.isEmpty() && !text.equals("×") && !text.equals("✕")) {
                values.add(text);
            }
        }
        return values;
    }

    private WebElement resolveClickTarget(WebElement trigger) {
        try {
            WebElement selector = trigger.findElement(
                    By.xpath("ancestor-or-self::*[contains(@class,'ant-select')][1]"));
            return selector;
        } catch (Exception e) {
            return trigger;
        }
    }

    private void waitForChip(WebElement container, String value) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2)).until(d -> {
                List<WebElement> chips = container.findElements(
                        By.xpath(".//span[contains(@class,'ant-select-selection-item')]"));
                return chips.stream().anyMatch(c -> c.getText().contains(value));
            });
        } catch (Exception e) {
            logger.debug("[AntD MultiSelect] Chip wait timeout for '{}', proceeding", value);
        }
    }

    private void ensurePanelOpen(WebDriver driver, WebElement trigger) {
        List<WebElement> panels = driver.findElements(
                By.cssSelector(".ant-select-dropdown:not(.ant-select-dropdown-hidden)"));
        boolean anyOpen = panels.stream().anyMatch(p -> {
            try { return p.isDisplayed(); } catch (Exception e) { return false; }
        });
        if (!anyOpen) {
            trigger.click();
            new WebDriverWait(driver, Duration.ofSeconds(2)).until(d ->
                    d.findElements(By.cssSelector(".ant-select-dropdown:not(.ant-select-dropdown-hidden)"))
                            .stream().anyMatch(p -> { try { return p.isDisplayed(); } catch (Exception e) { return false; } }));
        }
    }
}
