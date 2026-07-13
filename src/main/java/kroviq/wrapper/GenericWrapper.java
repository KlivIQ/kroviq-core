package kroviq.wrapper;

import kroviq.utils.LoadProperties;
import kroviq.utils.WaitHandler;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;


import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;


public class GenericWrapper {
    
    private static final Logger logger = LogManager.getLogger(GenericWrapper.class);
    private static final ThreadLocal<WebDriver> driverTL = new ThreadLocal<>();
    private static int defaultTimeoutSeconds = 15;
    private static int explicitTimeoutSeconds = 20;
    
    public JavascriptExecutor jsExecutor;
    public Select dropdown;
    public Actions action;
    public Robot robot;
    public Alert alert;
    
    public GenericWrapper() {}
    
    public static void initializeDriver(String browserName) {
        if (driverTL.get() == null) {
            if (browserName.equalsIgnoreCase("chrome")) {
                ChromeOptions options = new ChromeOptions();
                if (LoadProperties.getBool("headlessMode", false)) {
                    options.addArguments("--headless=new");
                }
                driverTL.set(new ChromeDriver(options));
                logger.info("Chrome browser launched successfully.");
            } else if (browserName.equalsIgnoreCase("edge")) {
                driverTL.set(new EdgeDriver());
                logger.info("Edge browser launched successfully.");
            } else if (browserName.equalsIgnoreCase("firefox")) {
                driverTL.set(new FirefoxDriver());
                logger.info("Firefox browser launched successfully.");
            } else {
                logger.error("Unsupported browser: {}", browserName);
                throw new IllegalArgumentException("Unsupported browser: " + browserName);
            }
        } else {
            logger.info("Driver already initialized. Skipping re-initialization.");
        }
    }
    

    public static void setImplicitWait(int implicitWait) {
        WebDriver driver = getDriver();
        if (driver != null) {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
            logger.debug("Implicit wait set to {} seconds.", implicitWait);
        } else {
            logger.warn("Driver is null. Cannot set implicit wait.");
        }
    }

    public static void maximizeWindow() {
        WebDriver driver = getDriver();
        if (driver != null) {
            driver.manage().window().maximize();
            logger.info("Browser window maximized.");
        } else {
            logger.warn("Driver is null. Cannot maximize window.");
        }
    }

    public static void openUrl(String url) {
        WebDriver driver = getDriver();
        if (driver != null) {
            driver.get(url);
            String title = driver.getTitle();
            String normalizedTitle = title == null ? "" : title.toLowerCase();
            String bodyText = "";

            try {
                bodyText = driver.findElement(By.tagName("body")).getText().toLowerCase();
            } catch (Exception e) {
                logger.debug("Unable to read page body text after navigation: {}", e.getMessage());
            }
            String combinedContent = bodyText;
            boolean hasKnownErrorText = bodyText.contains("page not found") || bodyText.contains("whitelabel error page");

            if (hasKnownErrorText ||
                normalizedTitle.contains("404") ||
                normalizedTitle.contains("not found") ||
                normalizedTitle.contains("error") ||
                bodyText.contains("404") ||
                combinedContent.contains("this site can't be reached") ||
                bodyText.contains("this site can't be reached")) {
                logger.error("Application URL not accessible / page not loaded");
                throw new RuntimeException("Application URL not accessible / page not loaded");
            }

            logger.info("Opened URL: {}", url);
        } else {
            logger.warn("Driver is null. Cannot open URL: {}", url);
        }
    }

    public static void quitDriver() {
        WebDriver driver = driverTL.get();
        if (driver != null) {
            try {
                driver.quit();
                logger.info("Browser closed successfully.");
            } catch (WebDriverException e) {
                logger.warn("Browser session was already unavailable during quit: {}", e.getMessage());
            } finally {
                driverTL.remove();
            }
        } else {
            logger.info("Driver was already null or not initialized.");
        }
    }

    public static boolean isDriverSessionActive() {
        WebDriver driver = driverTL.get();
        if (driver == null) {
            return false;
        }

        try {
            driver.getWindowHandles();
            return true;
        } catch (WebDriverException e) {
            logger.warn("WebDriver session is no longer active: {}", e.getMessage());
            driverTL.remove();
            return false;
        }
    }

    public String getCurrentUrl() {
        WebDriver driver = getDriver();
        if (driver != null) {
            return driver.getCurrentUrl();
        }
        logger.warn("Driver is null. Cannot get current URL.");
        return null;
    }

    public String getPageTitle() {
        WebDriver driver = getDriver();
        if (driver != null) {
            return driver.getTitle();
        }
        logger.warn("Driver is null. Cannot get page title.");
        return null;
    }

    public void enterText(WebElement element, String text) {
        try {
            // Element already located, just ensure visibility
            element.sendKeys(text);
            logger.info("Entered text '{}' into element: {}", text, element.toString());
        } catch (Exception e) {
            logger.error("Failed to enter text '{}' into element {}: {}", text, element.toString(), e.getMessage());
            throw e;
        }
    }

    public void clickElement(WebElement element) {
        try {
            element.click();
            logger.info("Clicked element: {}", element.toString());
        } catch (Exception e) {
            logger.error("Failed to click element {}: {}", element.toString(), e.getMessage());
            throw e;
        }
    }
    
    public void takeScreenshot(String fileName) throws IOException {
        WebDriver driver = getDriver();
        if (driver instanceof TakesScreenshot) {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dest = new File("Screenshots/" + fileName + ".jpeg");
            FileUtils.copyFile(src, dest);
            logger.info("Screenshot saved: {}", dest.getAbsolutePath());
        } else {
            logger.warn("Driver does not support taking screenshots.");
        }
    }

    public void scrollToElement(WebElement element) 
    {
        if (element != null)  
        {
            jsExecutor = (JavascriptExecutor) getDriver();
            jsExecutor.executeScript("arguments[0].scrollIntoView(true);", element);
            logger.info("Scrolled to element: {}", element.toString());
        } else {
            logger.warn("Driver does not support JavascriptExecutor for scrolling.");
        }
    }

    public void clickUsingJS(WebElement element) {
        WebDriver driver = getDriver();
        if (driver instanceof JavascriptExecutor) {
            jsExecutor = (JavascriptExecutor) driver;
            jsExecutor.executeScript("arguments[0].click();", element);
            logger.info("Clicked element using JS: {}", element.toString());
        } else {
            logger.warn("Driver does not support JavascriptExecutor for clicking.");
        }
    }

    public void highlightElement(WebElement element) {
        WebDriver driver = getDriver();
        if (driver instanceof JavascriptExecutor) {
            jsExecutor = (JavascriptExecutor) driver;
            jsExecutor.executeScript("arguments[0].setAttribute('style','border: 2px solid red;')", element);
            logger.debug("Highlighted element: {}", element.toString());
        } else {
            logger.warn("Driver does not support JavascriptExecutor for highlighting.");
        }
    }
    
    public String getPageTitleUsingJS() {
        WebDriver driver = getDriver();
        if (driver instanceof JavascriptExecutor) {
            jsExecutor = (JavascriptExecutor) driver;
            Object result = jsExecutor.executeScript("return document.title;");
            if (result != null) {
                String title = result.toString();
                logger.debug("Page title fetched using JS: {}", title);
                return title;
            } else {
                logger.warn("JavaScript execution for document.title returned null.");
                return null;
            }
        }
        logger.warn("Driver does not support JavascriptExecutor for getting page title, or driver is null.");
        return null;
    }

   public void selectDropdownOption(WebElement element, String method, String value) {
        dropdown = new Select(element);
        if (method.equalsIgnoreCase("value")) {
            dropdown.selectByValue(value);
            logger.info("Selected dropdown option by value '{}' for element: {}", value, element.toString());
        } else if (method.equalsIgnoreCase("visible_text")) 
        {
            dropdown.selectByVisibleText(value);
            logger.info("Selected dropdown option by visible text '{}' for element: {}", value, element.toString());
        } else if (method.equalsIgnoreCase("index")) {
            int index = Integer.parseInt(value);
            dropdown.selectByIndex(index);
            logger.info("Selected dropdown option by index '{}' for element: {}", index, element.toString());
        } else {
            logger.warn("Invalid selection method '{}'. Use 'value', 'visible_text', or 'index' for element: {}", method, element.toString());
        }
    }
    
    public WebElement getSelectedOption(WebElement element) {
        dropdown = new Select(element);
        WebElement selectedOption = dropdown.getFirstSelectedOption();
        logger.debug("Fetched selected option '{}' from dropdown: {}", selectedOption.getText(), element.toString());
        return selectedOption;
    }

    public List<WebElement> getAllDropdownOptions(WebElement element) {
        dropdown = new Select(element);
        List<WebElement> options = dropdown.getOptions();
        logger.debug("Fetched {} options from dropdown: {}", options.size(), element.toString());
        return options;
    }

    public void deselectByValue(WebElement element, String value) {
        dropdown = new Select(element);
        dropdown.deselectByValue(value);
        logger.info("Deselected dropdown option by value '{}' for element: {}", value, element.toString());
    }

    public void deselectAllOptions(WebElement element) {
        dropdown = new Select(element);
        dropdown.deselectAll();
        logger.info("Deselected all options for dropdown: {}", element.toString());
    }
    
    public void performMouseAction(String actionType, WebElement element) {
        WebDriver driver = getDriver();
        if (driver != null) {
            action = new Actions(driver);
            if (actionType.equalsIgnoreCase("hover")) {
                action.moveToElement(element).perform();
                logger.info("Performed hover action on element: {}", element.toString());
            } 
            else if (actionType.equalsIgnoreCase("double-click")) {
                action.doubleClick(element).perform();
                logger.info("Performed double click action on element: {}", element.toString());
            }
            else if (actionType.equalsIgnoreCase("hold")) {
                action.clickAndHold(element).perform();
                logger.info("Performed click and hold action on element: {}", element.toString());
            } else {
                logger.warn("Unsupported mouse action: {}", actionType);
            }
        } else
        {
            logger.warn("Driver is null. Cannot perform mouse action.");
        }
    }
    
    public void rightClickElement(WebElement element) {
        WebDriver driver = getDriver();
        if (driver != null) {
            action = new Actions(driver);
            action.contextClick(element).perform();
            logger.info("Performed right click on element: {}", element.toString());
        } else 
        {
            logger.warn("Driver is null. Cannot perform right click.");
        }
    }

    public void dragAndDropElement(WebElement source, WebElement target) {
        WebDriver driver = getDriver();
        if (driver != null) {
            action = new Actions(driver);
            action.dragAndDrop(source, target).perform();
            logger.info("Performed drag and drop from {} to {}", source.toString(), target.toString());
        } else {
            logger.warn("Driver is null. Cannot perform drag and drop.");
        }
    }

    public void handleAlert(String actionType, String inputText) {
        WebDriver driver = getDriver();
        if (driver != null) {
            try {
                waitForAlertPresence(); 
                alert = driver.switchTo().alert();
                if (actionType.equalsIgnoreCase("OK")) {
                    alert.accept();
                    logger.info("Accepted alert.");
                } 
                else if (actionType.equalsIgnoreCase("Cancel")) {
                    alert.dismiss();
                    logger.info("Dismissed alert.");
                } 
                else if (actionType.equalsIgnoreCase("Input")) {
                    if (inputText != null) {
                        alert.sendKeys(inputText);
                        alert.accept();
                        logger.info("Sent text '{}' to alert and accepted.", inputText);
                    } 
                    else {
                        logger.warn("Input text is null for 'Input' alert action.");
                    }
                } 
                else {
                    logger.warn("Invalid alert action: {}", actionType);
                }
            } 
            catch (NoAlertPresentException e) {
                logger.error("No alert present to handle: {}", e.getMessage());
            } catch (Exception e) {
                logger.error("Error handling alert: {}", e.getMessage());
            }
        } 
        else {
            logger.warn("Driver is null. Cannot handle alert.");
        }
    }

    public void pressEnterKey() throws AWTException {
        robot = new Robot();
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
        logger.info("Pressed Enter key.");
    }
    
    public void pressTabKey() throws AWTException {
        robot = new Robot();
        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);
        logger.info("Pressed Tab key.");
    }

    public void pressDownArrowKey() throws AWTException {
        robot = new Robot();
        robot.keyPress(KeyEvent.VK_DOWN);
        robot.keyRelease(KeyEvent.VK_DOWN);
        logger.info("Pressed Down Arrow key.");
    }

    public void pressUpArrowKey() throws AWTException {
        robot = new Robot();
        robot.keyPress(KeyEvent.VK_UP);
        robot.keyRelease(KeyEvent.VK_UP);
        logger.info("Pressed Up Arrow key.");
    }

    protected int getTimeoutSeconds() {
        return defaultTimeoutSeconds;
    }

    public void selectDropdownByIndex(By triggerLocator, int optionIndex) {
        try {
            WebDriver driver = getDriver();
            WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeoutSeconds));
            WebElement trigger = w.until(ExpectedConditions.elementToBeClickable(triggerLocator));
            trigger.click();

            action = new Actions(driver);
            for (int i = 1; i <= optionIndex; i++) {
                action.sendKeys(Keys.ARROW_DOWN).perform();
                new WebDriverWait(driver, Duration.ofMillis(500)).until(d -> true);
            }

            action.sendKeys(Keys.ENTER).perform();
            logger.info("Selected dropdown option at index: {}", optionIndex);

        } catch (Exception e) {
            logger.error("Failed to select option: {}", e.getMessage());
        }
    }
    
    
    
    public void waitForElementToBeClickable(WebElement element) {
        WebDriver driver = getDriver();
        if (driver != null) {
            new WebDriverWait(driver, Duration.ofSeconds(getTimeoutSeconds()))
                .until(ExpectedConditions.elementToBeClickable(element));
            logger.debug("Element is clickable: {}", element.toString());
        } 
        else {
            logger.warn("Driver is null. Cannot wait for element to be clickable.");
        }
    }

    public void ElementToBeVisibleWait(WebElement element) {
        new WebDriverWait(getDriver(), Duration.ofSeconds(explicitTimeoutSeconds))
            .until(ExpectedConditions.visibilityOf(element));
    }


    
    public void waitForAlertPresence() {
        WebDriver driver = getDriver();
        if (driver != null) {
            new WebDriverWait(driver, Duration.ofSeconds(getTimeoutSeconds()))
                .until(ExpectedConditions.alertIsPresent());
            logger.debug("Alert is present.");
        } 
        else {
            logger.warn("Driver is null. Cannot wait for alert presence.");
        }
    }

    public void webdriverWaitUntilUrlContains(String partialUrl) {
        WebDriver driver = getDriver();
        if (driver != null) {
            new WebDriverWait(driver, Duration.ofSeconds(getTimeoutSeconds()))
                .until(ExpectedConditions.urlContains(partialUrl));
            logger.debug("URL contains: {}", partialUrl);
        } else 
        {
            logger.warn("Driver is null. Cannot wait until URL contains: {}", partialUrl);
        }
    }
    
    public static WebDriver getDriver() {
        WebDriver driver = driverTL.get();
        if (driver == null) {
            logger.warn("WebDriver is null. Did you forget to call initializeDriver()?");
        }
        return driver;
    }
    
    public static void setDriver(WebDriver webDriver) {
        driverTL.set(webDriver);
        logger.info("WebDriver instance set externally");
    }
    
    public static WebElement waitUntil(org.openqa.selenium.support.ui.ExpectedCondition<WebElement> condition) {
        if (driverTL.get() != null) {
            return WaitHandler.waitForCustomCondition(condition::apply, WaitHandler.WaitTier.MEDIUM);
        } else {
            logger.warn("Driver is null. Cannot wait for condition.");
            return null;
        }
    }
    
    }
