package kroviq.wrapper.core;

import kroviq.utils.LoadProperties;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.time.Duration;
import java.util.List;

public final class HandlerUtils {

    private HandlerUtils() {}

    public static int getTimeout(String framework) {
        int timeout = LoadProperties.getInt("wrapper.timeout." + framework, -1);
        if (timeout > 0) return timeout;
        timeout = LoadProperties.getInt(framework + ".timeout", -1);
        if (timeout > 0) return timeout;
        return LoadProperties.getInt("wrapper.timeout.default", 10);
    }

    public static Duration getTimeoutDuration(String framework) {
        return Duration.ofSeconds(getTimeout(framework));
    }

    public static int getMaxScrollAttempts(String framework) {
        int attempts = LoadProperties.getInt("wrapper.maxScrollAttempts." + framework, -1);
        if (attempts > 0) return attempts;
        attempts = LoadProperties.getInt(framework + ".maxScrollAttempts", -1);
        if (attempts > 0) return attempts;
        return LoadProperties.getInt("wrapper.maxScrollAttempts", 20);
    }

    public static WebElement findOptionByText(WebElement container, String selector, String text, boolean allowPartial) {
        List<WebElement> options = container.findElements(By.cssSelector(selector));
        for (WebElement opt : options) {
            String optText = opt.getText().trim();
            if (optText.equals(text) || optText.equalsIgnoreCase(text)) {
                return opt;
            }
        }
        if (allowPartial) {
            for (WebElement opt : options) {
                if (opt.getText().trim().contains(text)) {
                    return opt;
                }
            }
        }
        return null;
    }

    public static void scrollIntoViewAndClick(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'nearest'});", element);
        element.click();
    }

    public static void clickSafe(WebDriver driver, WebElement element) {
        try {
            element.click();
        } catch (ElementNotInteractableException e) {
            jsClick(driver, element);
        }
    }

    public static void jsClick(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    public static void clearAndType(WebDriver driver, WebElement input, String text) {
        input.click();
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", input);
        input.sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END), Keys.DELETE);
        input.sendKeys(text);
    }
}
