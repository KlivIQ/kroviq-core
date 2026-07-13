package kroviq.wrapper.mui;

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

public class MUIDropdownHandler implements DropdownHandler {

    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("mui");

    @Override
    public void select(WebDriver driver, WebElement trigger, String visibleText) {
        trigger.click();
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);

        // MUI Select renders options in a Popover with role="listbox"
        WebElement listbox = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[role='listbox']")));

        WebElement option = HandlerUtils.findOptionByText(listbox, "[role='option']", visibleText, false);
        if (option == null) {
            throw new org.openqa.selenium.NoSuchElementException(
                    "MUI dropdown option '" + visibleText + "' not found");
        }

        HandlerUtils.scrollIntoViewAndClick(driver, option);
        // Wait for popover to close
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector(".MuiPopover-root")));
    }

    @Override
    public List<String> getOptions(WebDriver driver, WebElement trigger) {
        trigger.click();
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);

        WebElement listbox = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[role='listbox']")));

        List<WebElement> options = listbox.findElements(By.cssSelector("[role='option']"));
        List<String> texts = options.stream()
                .map(opt -> opt.getText().trim())
                .collect(Collectors.toList());

        // Close dropdown
        ((JavascriptExecutor) driver).executeScript(
                "document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape'}));");
        return texts;
    }


}
