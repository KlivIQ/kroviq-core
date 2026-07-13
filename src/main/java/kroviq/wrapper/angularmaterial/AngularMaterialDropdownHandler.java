package kroviq.wrapper.angularmaterial;

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

public class AngularMaterialDropdownHandler implements DropdownHandler {

    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("angularmaterial");
    private static final String OVERLAY_PANEL = ".cdk-overlay-container .mat-mdc-select-panel, .cdk-overlay-container .mat-select-panel";
    private static final String OPTION_SELECTOR = "mat-option, .mat-mdc-option";

    @Override
    public void select(WebDriver driver, WebElement trigger, String visibleText) {
        trigger.click();
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);

        WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(OVERLAY_PANEL)));

        WebElement option = HandlerUtils.findOptionByText(panel, OPTION_SELECTOR, visibleText, false);
        if (option == null) {
            throw new org.openqa.selenium.NoSuchElementException(
                    "Angular Material option '" + visibleText + "' not found");
        }

        HandlerUtils.scrollIntoViewAndClick(driver, option);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(OVERLAY_PANEL)));
    }

    @Override
    public List<String> getOptions(WebDriver driver, WebElement trigger) {
        trigger.click();
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);

        WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(OVERLAY_PANEL)));

        List<WebElement> options = panel.findElements(By.cssSelector(OPTION_SELECTOR));
        List<String> texts = options.stream()
                .map(opt -> opt.getText().trim())
                .collect(Collectors.toList());

        // Close by pressing Escape via JS
        ((JavascriptExecutor) driver).executeScript(
                "document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape'}));");
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(OVERLAY_PANEL)));
        return texts;
    }


}
