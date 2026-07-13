package kroviq.wrapper;

import kroviq.utils.LoadProperties;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;

public class BasePageWrapper {
    
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected GenericWrapper wrapper;
    protected AntDScroller antDScroller;
    protected AntDUtils antDUtils;
    
    public BasePageWrapper(WebDriver driver) {
        this.driver = driver;
        int explicitTimeout = LoadProperties.getInt("explicitTimeOut", 15);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(explicitTimeout));
        this.wrapper = new GenericWrapper();
        this.antDScroller = new AntDScroller(driver, explicitTimeout);
        this.antDUtils = new AntDUtils(driver, explicitTimeout);
    }
    
    protected AntDScroller getAntDScroller() {
        if (this.antDScroller == null) {
            int timeout = LoadProperties.getInt("explicitTimeOut", 15);
            this.antDScroller = new AntDScroller(driver, timeout);
        }
        return this.antDScroller;
    }

    protected AntDUtils getAntDUtils() {
        if (this.antDUtils == null) {
            int timeout = LoadProperties.getInt("explicitTimeOut", 15);
            this.antDUtils = new AntDUtils(driver, timeout);
        }
        return this.antDUtils;
    }
    
    // Common wait methods using GenericWrapper
    protected WebElement waitForElement(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }
    
    protected WebElement waitForClickableElement(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }
    
    protected void waitAndClick(WebElement element) {
        wrapper.waitForElementToBeClickable(element);
        wrapper.clickElement(element);
    }
    
    protected void waitAndEnterText(WebElement element, String text) {
        wrapper.ElementToBeVisibleWait(element);
        wrapper.enterText(element, text);
    }
    
    protected void waitAndClickJS(WebElement element) {
        wrapper.ElementToBeVisibleWait(element);
        wrapper.clickUsingJS(element);
    }
    
    // AntD specific methods (lazy-loaded)
    protected void selectAntDOption(By selectRoot, String visibleText) {
        getAntDScroller().selectWithScroll(selectRoot, visibleText);
    }
    
    protected void selectAntDOptionByText(String visibleText) throws Exception {
        getAntDUtils().selectAntDOptionByText(visibleText);
    }
    
    protected void scanForAntDOption(By optionBy, WebElement panel) {
        getAntDUtils().scanFor(optionBy, panel);
    }
    
    // Enhanced dropdown selection with AntD support
    protected void selectDropdownOption(WebElement dropdownElement, String optionText) throws Exception {
        try {
            // First try standard dropdown
            wrapper.selectDropdownOption(dropdownElement, "visible_text", optionText);
        } catch (Exception e) {
            // If standard fails, try AntD approach
            dropdownElement.click();
            selectAntDOptionByText(optionText);
        }
    }
    
    // Scroll to element using GenericWrapper
    protected void scrollToElement(WebElement element) {
        wrapper.scrollToElement(element);
    }
    
    // Highlight element for debugging
    protected void highlightElement(WebElement element) {
        wrapper.highlightElement(element);
    }
}