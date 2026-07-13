package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.time.Duration;

public interface LoadingStateDetector {

    boolean isPageLoading(WebDriver driver);

    void waitForPageReady(WebDriver driver);

    void waitForPageReady(WebDriver driver, Duration timeout);

    boolean isElementLoading(WebDriver driver, WebElement scope);
}
