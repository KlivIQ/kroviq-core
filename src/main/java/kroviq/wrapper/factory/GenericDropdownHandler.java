package kroviq.wrapper.factory;

import kroviq.wrapper.core.DropdownHandler;
import kroviq.wrapper.core.HandlerUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class GenericDropdownHandler implements DropdownHandler {

    @Override
    public void select(WebDriver driver, WebElement trigger, String visibleText) {
        // Try native HTML <select> first
        if ("select".equalsIgnoreCase(trigger.getTagName())) {
            new Select(trigger).selectByVisibleText(visibleText);
            return;
        }

        // Fallback: click trigger, then find option by text in any visible listbox
        trigger.click();
        Duration timeout = HandlerUtils.getTimeoutDuration("generic");
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        WebElement option = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//*[@role='option' and normalize-space()='" + visibleText + "']")));
        option.click();
    }

    @Override
    public List<String> getOptions(WebDriver driver, WebElement trigger) {
        if ("select".equalsIgnoreCase(trigger.getTagName())) {
            return new Select(trigger).getOptions().stream()
                    .map(WebElement::getText)
                    .collect(Collectors.toList());
        }

        trigger.click();
        Duration timeout = HandlerUtils.getTimeoutDuration("generic");
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[role='option']")));
        List<WebElement> options = driver.findElements(By.cssSelector("[role='option']"));
        List<String> texts = options.stream().map(WebElement::getText).collect(Collectors.toList());
        // Close by pressing Escape
        trigger.sendKeys(org.openqa.selenium.Keys.ESCAPE);
        return texts;
    }
}
