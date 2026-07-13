package kroviq.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;
import kroviq.wrapper.GenericWrapper;
import kroviq.wrapper.core.DatePickerHandler;
import kroviq.wrapper.core.DropdownHandler;
import kroviq.wrapper.core.FileUploadHandler;
import kroviq.wrapper.core.MultiSelectHandler;
import kroviq.wrapper.factory.ComponentHandlerFactory;
import kroviq.wrapper.factory.UIFrameworkDetector;
import kroviq.wrapper.factory.UIFrameworkDetector.UIFramework;
import kroviq.utils.ActionType;
import kroviq.utils.ConstantsResolver;
import kroviq.utils.WaitHandler;
import kroviq.utils.DatePickerUtils;
import kroviq.reporting.StepReportingWrapper;
import org.openqa.selenium.Keys;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.StaleElementReferenceException;
import java.util.List;

public class GenericActionHandler {
    
    private static final Logger logger = LogManager.getLogger(GenericActionHandler.class);
    private static final int MAX_SYNC_RETRIES = 2;
    
    private static void ensureElementInViewport(WebElement element) {
        try {
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) GenericWrapper.getDriver();
            Boolean inViewport = (Boolean) js.executeScript(
                "var rect = arguments[0].getBoundingClientRect();" +
                "var centerX = rect.left + rect.width / 2;" +
                "var centerY = rect.top + rect.height / 2;" +
                "return centerX >= 0 && centerX <= window.innerWidth && centerY >= 0 && centerY <= window.innerHeight;",
                element
            );
            if (!inViewport) {
                js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
                shortSettleWait();
            }
        } catch (Exception e) {
            logger.debug("Viewport alignment skipped: {}", e.getMessage());
        }
    }
    
    private static void shortSettleWait() {
        try {
            new WebDriverWait(GenericWrapper.getDriver(), Duration.ofMillis(300))
                .until(d -> ((org.openqa.selenium.JavascriptExecutor) d)
                    .executeScript("return document.readyState").equals("complete"));
        } catch (Exception e) {
            // settle wait is best-effort
        }
    }
    
    public static void handleElementClick(String pageName, String elementName, String section) {
        logger.debug("Attempting click: element='{}', page='{}', section='{}'", elementName, pageName, section);
        
        try {
            ConstantsResolver.ElementInfo elementInfo = ConstantsResolver.resolve(pageName, elementName);
            By locator = By.xpath(elementInfo.getXpath());
            logger.info("Resolved '{}' on '{}': {}", elementName, pageName, elementInfo.getXpath());
            
            for (int attempt = 1; attempt <= MAX_SYNC_RETRIES; attempt++) {
                try {
                    WebElement element = WaitHandler.waitForClickableWithHealing(locator, elementInfo.getActionType(), elementName, pageName);
                    ensureElementInViewport(element);
                    
                    try {
                        element.click();
                    } catch (ElementClickInterceptedException e) {
                        logger.debug("Click intercepted on '{}', using JS fallback", elementName);
                        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) GenericWrapper.getDriver();
                        js.executeScript("arguments[0].scrollIntoView({block:'center'});", element);
                        shortSettleWait();
                        js.executeScript("arguments[0].click();", element);
                    }
                    logger.info("Successfully clicked '{}'", elementName);
                    return;
                } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                    logger.debug("[Sync] Retrying action (attempt {}) for element {}: {}", attempt, elementName, e.getClass().getSimpleName());
                    if (attempt == MAX_SYNC_RETRIES) throw e;
                }
            }
        } catch (Exception e) {
            String errorContext = String.format("element='%s', page='%s'%s", 
                elementName, pageName, section != null ? ", section='" + section + "'" : "");
            StepReportingWrapper.recordManualStep("Failed to click " + errorContext + ": " + e.getMessage(), "FAIL");
            logger.error("Click failed for {}: {}", errorContext, e.getMessage());
            throw new RuntimeException("Failed to click " + errorContext + ": " + e.getMessage(), e);
        }
    }
    
    public static void handleElementInput(String pageName, String elementName, String value) {
        if (value == null || value.trim().isEmpty()) {
            logger.info("Skipping input for '{}' on '{}' - empty test data", elementName, pageName);
            return;
        }
        
        try {
            ConstantsResolver.ElementInfo elementInfo = ConstantsResolver.resolve(pageName, elementName);
            By locator = By.xpath(elementInfo.getXpath());
            WebElement element = WaitHandler.waitForVisibilityWithHealing(locator, elementInfo.getActionType(), elementName, pageName);
            
            String tagName = element.getTagName().toLowerCase();
            if (!tagName.equals("input") && !tagName.equals("textarea")) {
                try {
                    WebElement actualInput = element.findElement(By.xpath(".//input | .//textarea"));
                    logger.debug("Resolved wrapper element to actual input node for '{}'", elementName);
                    element = actualInput;
                } catch (Exception e) {
                    logger.debug("No input/textarea found inside wrapper for '{}', using original element", elementName);
                }
            }
            
            if (elementInfo.getActionType() == ActionType.DATE_PICKER) {
                logger.info("Element '{}' is DATE_PICKER type, using factory-dispatched handling", elementName);
                DatePickerHandler dateHandler = ComponentHandlerFactory.getDatePickerHandler(element, GenericWrapper.getDriver());
                dateHandler.setDateValue(GenericWrapper.getDriver(), element, value);
                return;
            }
            
            ensureElementInViewport(element);
            
            element.clear();
            element.sendKeys(value);
            
            // Verify value was committed
            final WebElement inputRef = element;
            try {
                new WebDriverWait(GenericWrapper.getDriver(), Duration.ofSeconds(2)).until(d -> {
                    try {
                        String actual = inputRef.getAttribute("value");
                        return actual != null && !actual.isEmpty();
                    } catch (StaleElementReferenceException e) { return false; }
                });
            } catch (TimeoutException te) {
                logger.debug("Input value verification timed out for '{}', proceeding", elementName);
            }
            logger.info("Successfully entered '{}' in '{}'", value, elementName);
        } catch (InvalidElementStateException e) {
            ConstantsResolver.ElementInfo elementInfo = ConstantsResolver.resolve(pageName, elementName);
            if (elementInfo.getActionType() == ActionType.DATE_PICKER) {
                logger.warn("Invalid element state for DATE_PICKER '{}', using fallback", elementName);
                handleDatePickerFallback(pageName, elementName, value);
                logger.info("Successfully entered date '{}' in '{}' using DatePicker fallback", value, elementName);
            } else {
                throw new RuntimeException("Cannot enter text in readonly/invalid element: " + elementName + " (type: " + elementInfo.getActionType() + ")", e);
            }
        } catch (Exception e) {
            StepReportingWrapper.recordManualStep("Failed to enter text in element '" + elementName + "' on page '" + pageName + "': " + e.getMessage(), "FAIL");
            throw new RuntimeException("Failed to enter text in element: " + elementName + " on page: " + pageName, e);
        }
    }
    
    public static void handleElementDropdown(String pageName, String elementName, String optionValue) {
        if (optionValue == null || optionValue.trim().isEmpty()) {
            logger.info("Skipping dropdown '{}' on '{}' - empty test data", elementName, pageName);
            return;
        }
        
        logger.info("Dropdown '{}' resolved value = '{}'", elementName, optionValue);
        
        String[] tokens = parseMultiSelectValue(optionValue);
        boolean isMultiSelect = tokens.length > 1;
        
        if (isMultiSelect) {
            logger.info("Multi-select detected: {} values to select", tokens.length);
        }
        
        try {
            ConstantsResolver.ElementInfo elementInfo = ConstantsResolver.resolve(pageName, elementName);
            By locator = By.xpath(elementInfo.getXpath());
            
            WebElement element = WaitHandler.waitForPresence(locator, ActionType.DROPDOWN);
            UIFramework framework = UIFrameworkDetector.detect(element);
            logger.debug("Detected framework '{}' for dropdown '{}'", framework, elementName);
            
            if (framework == UIFramework.ANTD) {
                handleAntDDropdown(element, elementName, pageName, tokens);
            } else {
                handleFactoryDropdown(element, elementName, pageName, tokens);
            }
        } catch (Exception e) {
            StepReportingWrapper.recordManualStep("Failed to handle dropdown '" + elementName + "' on page '" + pageName + "': " + e.getMessage(), "FAIL");
            throw new RuntimeException("Failed to handle dropdown: " + elementName + " on page: " + pageName, e);
        }
    }
    
    private static void handleAntDDropdown(WebElement element, String elementName, String pageName, String[] tokens) throws Exception {
        boolean isMultiSelect = tokens.length > 1;
        
        WebElement resolvedElement = resolveDropdownControl(element);
        
        WebElement waitAndClickTarget = resolvedElement;
        String role = resolvedElement.getAttribute("role");
        if ("combobox".equals(role)) {
            try {
                WebElement antSelectAncestor = resolvedElement.findElement(By.xpath("./ancestor::*[contains(@class, 'ant-select')][1]"));
                waitAndClickTarget = antSelectAncestor;
            } catch (org.openqa.selenium.NoSuchElementException e) {
                // use resolvedElement
            }
        }
        
        WebDriverWait wait = new WebDriverWait(GenericWrapper.getDriver(), Duration.ofSeconds(3));
        wait.until(ExpectedConditions.elementToBeClickable(waitAndClickTarget));
        
        ensureElementInViewport(waitAndClickTarget);
        waitForNoSpinner();
        
        waitAndClickTarget.click();
        logger.info("Clicked dropdown '{}' on '{}'", elementName, pageName);
        
        kroviq.wrapper.AntDUtils antDUtils = new kroviq.wrapper.AntDUtils(GenericWrapper.getDriver(), 10);
        
        if (isMultiSelect) {
            antDUtils.selectAntDOptionByText(tokens[0], elementName, resolvedElement);
            logger.info("Selected option 1/{}: '{}'", tokens.length, tokens[0]);
            
            for (int i = 1; i < tokens.length; i++) {
                String token = tokens[i];
                logger.info("Selecting option {}/{}: '{}'", i + 1, tokens.length, token);
                
                waitForSelectionCommit(waitAndClickTarget, tokens[i - 1]);
                ensureDropdownOpen(waitAndClickTarget);
                
                WebElement panel = antDUtils.findVisiblePanel();
                antDUtils.selectOptionFromOpenPanel(panel, token, elementName);
            }
        } else {
            antDUtils.selectAntDOptionByText(tokens[0], elementName, resolvedElement);
        }
        
        logger.info("Successfully selected {} option(s) from '{}'", tokens.length, elementName);
    }
    
    private static void handleFactoryDropdown(WebElement element, String elementName, String pageName, String[] tokens) {
        WebDriver driver = GenericWrapper.getDriver();
        
        if (tokens.length > 1) {
            MultiSelectHandler multiHandler = ComponentHandlerFactory.getMultiSelectHandler(element, driver);
            multiHandler.selectMultiple(driver, element, java.util.Arrays.asList(tokens));
            logger.info("Selected {} option(s) from '{}' via MultiSelectHandler", tokens.length, elementName);
            return;
        }
        
        DropdownHandler handler = ComponentHandlerFactory.getDropdownHandler(element, driver);
        handler.select(driver, element, tokens[0]);
        logger.info("Selected '{}' from '{}' via factory handler", tokens[0], elementName);
    }
    
    public static void handleElementDropdownInModal(String pageName, String elementName, String optionValue) {
        if (optionValue == null || optionValue.trim().isEmpty()) {
            logger.info("Skipping dropdown '{}' in modal on '{}' - empty test data", elementName, pageName);
            return;
        }
        
        String[] tokens = parseMultiSelectValue(optionValue);
        
        try {
            ConstantsResolver.ElementInfo elementInfo = ConstantsResolver.resolve(pageName, elementName);
            By locator = By.xpath(elementInfo.getXpath());
            
            WebElement element = WaitHandler.waitForPresence(locator, ActionType.DROPDOWN);
            UIFramework framework = UIFrameworkDetector.detect(element);
            logger.debug("Detected framework '{}' for modal dropdown '{}'", framework, elementName);
            
            if (framework == UIFramework.ANTD) {
                handleAntDDropdownInModal(element, elementName, pageName, tokens);
            } else {
                handleFactoryDropdown(element, elementName, pageName, tokens);
            }
        } catch (Exception e) {
            StepReportingWrapper.recordManualStep("Failed to handle dropdown '" + elementName + "' in modal on page '" + pageName + "': " + e.getMessage(), "FAIL");
            throw new RuntimeException("Failed to handle dropdown in modal: " + elementName + " on page: " + pageName, e);
        }
    }
    
    private static void handleAntDDropdownInModal(WebElement element, String elementName, String pageName, String[] tokens) throws Exception {
        boolean isMultiSelect = tokens.length > 1;
        
        WebElement resolvedElement = resolveDropdownControl(element);
        
        WebElement waitAndClickTarget = resolvedElement;
        String role = resolvedElement.getAttribute("role");
        if ("combobox".equals(role)) {
            try {
                WebElement antSelectAncestor = resolvedElement.findElement(By.xpath("./ancestor::*[contains(@class, 'ant-select')][1]"));
                waitAndClickTarget = antSelectAncestor;
            } catch (org.openqa.selenium.NoSuchElementException e) {
                // use resolvedElement
            }
        }
        
        WebDriverWait wait = new WebDriverWait(GenericWrapper.getDriver(), Duration.ofSeconds(3));
        wait.until(ExpectedConditions.elementToBeClickable(waitAndClickTarget));
        
        ensureElementInViewport(waitAndClickTarget);
        waitForNoSpinner();
        
        waitAndClickTarget.click();
        
        kroviq.wrapper.AntDUtils antDUtils = new kroviq.wrapper.AntDUtils(GenericWrapper.getDriver(), 10);
        
        if (isMultiSelect) {
            antDUtils.selectAntDOptionByTextNoKeys(tokens[0], elementName, resolvedElement);
            
            for (int i = 1; i < tokens.length; i++) {
                String token = tokens[i];
                
                waitForSelectionCommit(waitAndClickTarget, tokens[i - 1]);
                ensureDropdownOpen(waitAndClickTarget);
                
                WebElement panel = antDUtils.findVisiblePanel();
                antDUtils.selectOptionFromOpenPanel(panel, token, elementName);
            }
        } else {
            antDUtils.selectAntDOptionByTextNoKeys(tokens[0], elementName, resolvedElement);
        }
        
        logger.info("Successfully selected {} option(s) from '{}' in modal", tokens.length, elementName);
    }
    
    public static void handleElementToggle(String pageName, String elementName, boolean targetState) {
        try {
            ConstantsResolver.ElementInfo elementInfo = ConstantsResolver.resolve(pageName, elementName);
            By locator = By.xpath(elementInfo.getXpath());
            
            for (int attempt = 1; attempt <= MAX_SYNC_RETRIES; attempt++) {
                try {
                    WebElement element = WaitHandler.waitForClickableWithHealing(locator, elementInfo.getActionType(), elementName, pageName);
                    
                    org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) GenericWrapper.getDriver();
                    js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'center'});", element);
                    shortSettleWait();
                    
                    boolean currentState = "true".equals(element.getAttribute("aria-checked"));
                    
                    if (currentState == targetState) {
                        logger.info("Toggle '{}' already in desired state '{}' on '{}'", elementName, targetState, pageName);
                        return;
                    }
                    
                    try {
                        element.click();
                    } catch (ElementClickInterceptedException e) {
                        js.executeScript("arguments[0].click();", element);
                    }
                    
                    // Verify state actually changed
                    final WebElement toggleRef = element;
                    try {
                        new WebDriverWait(GenericWrapper.getDriver(), Duration.ofSeconds(2)).until(d -> {
                            try {
                                return String.valueOf(targetState).equals(toggleRef.getAttribute("aria-checked"));
                            } catch (StaleElementReferenceException e) { return false; }
                        });
                    } catch (TimeoutException te) {
                        logger.debug("Toggle state verification timed out for '{}', proceeding", elementName);
                    }
                    
                    logger.info("Successfully toggled '{}' to '{}' on '{}'", elementName, targetState, pageName);
                    return;
                } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                    logger.debug("[Sync] Retrying action (attempt {}) for element {}: {}", attempt, elementName, e.getClass().getSimpleName());
                    if (attempt == MAX_SYNC_RETRIES) throw e;
                }
            }
        } catch (Exception e) {
            StepReportingWrapper.recordManualStep("Failed to handle toggle '" + elementName + "' on page '" + pageName + "': " + e.getMessage(), "FAIL");
            throw new RuntimeException("Failed to handle toggle: " + elementName + " on page: " + pageName, e);
        }
    }
    
    public static void handleElementCheckbox(String pageName, String elementName, String action) {
        if (action == null || action.trim().isEmpty()) {
            logger.info("Skipping checkbox '{}' on '{}' - empty test data", elementName, pageName);
            return;
        }
        
        try {
            boolean targetState = "yes".equalsIgnoreCase(action);
            ConstantsResolver.ElementInfo elementInfo = ConstantsResolver.resolve(pageName, elementName);
            By locator = By.xpath(elementInfo.getXpath());
            
            for (int attempt = 1; attempt <= MAX_SYNC_RETRIES; attempt++) {
                try {
                    WebElement inputElement = WaitHandler.waitForPresence(locator, elementInfo.getActionType());
                    
                    WebElement clickableElement = inputElement;
                    try {
                        WebElement wrapper = inputElement.findElement(By.xpath("./ancestor::label[contains(@class, 'ant-checkbox-wrapper')][1] | ./ancestor::span[contains(@class, 'ant-checkbox')][1]"));
                        if (wrapper != null && wrapper.isDisplayed()) {
                            clickableElement = wrapper;
                        }
                    } catch (Exception e) {
                        // use input directly
                    }
                    
                    ensureElementInViewport(clickableElement);
                    shortSettleWait();
                    
                    boolean currentState = isCheckboxChecked(inputElement);
                    
                    if (currentState == targetState) {
                        logger.info("Checkbox '{}' already in desired state '{}' on '{}'", elementName, action, pageName);
                        return;
                    }
                    
                    try {
                        clickableElement.click();
                    } catch (ElementClickInterceptedException e) {
                        ((org.openqa.selenium.JavascriptExecutor) GenericWrapper.getDriver())
                            .executeScript("arguments[0].click();", clickableElement);
                    }
                    
                    // Verify state actually changed
                    final WebElement cbRef = inputElement;
                    try {
                        new WebDriverWait(GenericWrapper.getDriver(), Duration.ofSeconds(2)).until(d -> {
                            try {
                                return isCheckboxChecked(cbRef) == targetState;
                            } catch (StaleElementReferenceException e) { return false; }
                        });
                    } catch (TimeoutException te) {
                        logger.debug("Checkbox state verification timed out for '{}', proceeding", elementName);
                    }
                    
                    logger.info("Successfully set checkbox '{}' to '{}' on '{}'", elementName, action, pageName);
                    return;
                } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                    logger.debug("[Sync] Retrying action (attempt {}) for element {}: {}", attempt, elementName, e.getClass().getSimpleName());
                    if (attempt == MAX_SYNC_RETRIES) throw e;
                }
            }
        } catch (Exception e) {
            StepReportingWrapper.recordManualStep("Failed to handle checkbox '" + elementName + "' on page '" + pageName + "': " + e.getMessage(), "FAIL");
            throw new RuntimeException("Failed to handle checkbox: " + elementName + " on page: " + pageName, e);
        }
    }
    
    private static boolean isCheckboxChecked(WebElement inputElement) {
        return inputElement.isSelected() ||
               (inputElement.getAttribute("class") != null && inputElement.getAttribute("class").contains("ant-checkbox-checked")) ||
               "true".equals(inputElement.getAttribute("aria-checked"));
    }

    public static boolean handleElementVerification(String pageName, String elementName, String expectedValue) {
        try {
            ConstantsResolver.ElementInfo elementInfo = ConstantsResolver.resolve(pageName, elementName);
            By locator = By.xpath(elementInfo.getXpath());
            WebElement element = WaitHandler.waitForVisibilityWithHealing(locator, elementInfo.getActionType(), elementName, pageName);
            return (expectedValue == null || expectedValue.isEmpty()) ? 
                element.isDisplayed() : element.getText().equals(expectedValue);
        } catch (Exception e) {
            // Generic fallback via PageFactory reflection
            return handlePageSpecificVerification(pageName, elementName);
        }
    }
    
    private static boolean handlePageSpecificVerification(String pageName, String elementName) {
        Object pageInstance = PageFactory.getPage(pageName);
        if (pageInstance == null) return false;
        
        try {
            // Try reflection-based verification methods
            String methodName = "is" + elementName.substring(0, 1).toUpperCase() + elementName.substring(1) + "Displayed";
            java.lang.reflect.Method method = pageInstance.getClass().getMethod(methodName);
            return (Boolean) method.invoke(pageInstance);
        } catch (Exception e) {
            logger.debug("No page-specific verification for '{}' on '{}'", elementName, pageName);
            return false;
        }
    }
    
    private static void waitForSelectionCommit(WebElement dropdownContainer, String selectedValue) {
        WebDriverWait wait = new WebDriverWait(GenericWrapper.getDriver(), Duration.ofSeconds(2));
        try {
            wait.until(driver -> {
                try {
                    List<WebElement> chips = dropdownContainer.findElements(
                        By.xpath(".//span[contains(@class,'ant-select-selection-item')]"));
                    for (WebElement chip : chips) {
                        if (chip.getText().contains(selectedValue)) return true;
                    }
                    List<WebElement> inputs = dropdownContainer.findElements(By.xpath(".//input"));
                    for (WebElement input : inputs) {
                        String value = input.getAttribute("value");
                        if (value != null && value.contains(selectedValue)) return true;
                    }
                } catch (Exception e) { /* ignore */ }
                return false;
            });
        } catch (TimeoutException e) {
            logger.debug("Selection commit timeout for '{}', proceeding", selectedValue);
        }
    }
    
    private static void ensureDropdownOpen(WebElement dropdownTrigger) {
        try {
            WebDriver driver = GenericWrapper.getDriver();
            List<WebElement> panels = driver.findElements(
                By.cssSelector(".ant-select-dropdown:not([style*='display: none'])"));
            
            boolean panelOpen = panels.stream().anyMatch(p -> {
                try { return p.isDisplayed(); } catch (Exception e) { return false; }
            });
            
            if (!panelOpen) {
                dropdownTrigger.click();
                new WebDriverWait(driver, Duration.ofSeconds(2)).until(d -> {
                    List<WebElement> reopenedPanels = d.findElements(
                        By.cssSelector(".ant-select-dropdown:not([style*='display: none'])"));
                    return reopenedPanels.stream().anyMatch(p -> {
                        try { return p.isDisplayed(); } catch (Exception e) { return false; }
                    });
                });
            }
        } catch (Exception e) {
            logger.debug("Panel state check failed, continuing: {}", e.getMessage());
        }
    }
    
    private static String[] parseMultiSelectValue(String value) {
        if (value == null || value.trim().isEmpty()) return new String[0];
        String separator = value.contains(",") ? "," : "\\|";
        return java.util.Arrays.stream(value.split(separator))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);
    }
    
    private static WebElement resolveDropdownControl(WebElement element) {
        try {
            WebElement selector = element.findElement(
                By.xpath("ancestor-or-self::*[contains(@class,'ant-select-selector')][1]"));
            if (selector != null) return selector;
        } catch (Exception e) { /* try next */ }
        
        try {
            WebElement container = element.findElement(
                By.xpath("ancestor-or-self::*[contains(@class,'ant-select')][1]"));
            if (container != null) return container;
        } catch (Exception e) { /* fallback */ }
        
        return element;
    }
    
    private static void dismissBlockingModals() {
        try {
            WebDriver driver = GenericWrapper.getDriver();
            var modals = driver.findElements(By.xpath("//div[contains(@class,'ant-modal-wrap')]"));
            if (!modals.isEmpty() && modals.get(0).isDisplayed()) {
                modals.get(0).sendKeys(Keys.ESCAPE);
                shortSettleWait();
            }
        } catch (Exception e) {
            // No action needed
        }
    }
    
    private static void waitForNoSpinner() {
        WebDriver driver = GenericWrapper.getDriver();
        kroviq.wrapper.core.LoadingStateDetector detector = new kroviq.wrapper.factory.GenericLoadingStateDetector();
        detector.waitForPageReady(driver, java.time.Duration.ofSeconds(3));
    }
    
    private static void handleDatePickerFallback(String pageName, String elementName, String value) {
        try {
            ConstantsResolver.ElementInfo elementInfo = ConstantsResolver.resolve(pageName, elementName);
            By locator = By.xpath(elementInfo.getXpath());
            WebElement element = WaitHandler.waitForVisibilityWithHealing(locator, elementInfo.getActionType(), elementName, pageName);
            DatePickerUtils.setDatePickerValue(element, value);
        } catch (Exception e) {
            throw new RuntimeException("DatePicker fallback failed for element: " + elementName, e);
        }
    }
    
    public static void handleElementUpload(String pageName, String elementName, String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            logger.info("Skipping upload for '{}' on '{}' - empty file path", elementName, pageName);
            return;
        }
        
        try {
            ConstantsResolver.ElementInfo elementInfo = ConstantsResolver.resolve(pageName, elementName);
            By locator = By.xpath(elementInfo.getXpath());
            WebElement element = WaitHandler.waitForPresence(locator, ActionType.UPLOAD);
            
            WebDriver driver = GenericWrapper.getDriver();
            FileUploadHandler handler = ComponentHandlerFactory.getFileUploadHandler(element, driver);
            handler.uploadFile(driver, element, filePath);
            
            logger.info("Successfully uploaded file '{}' via '{}' on '{}'", filePath, elementName, pageName);
        } catch (Exception e) {
            StepReportingWrapper.recordManualStep("Failed to upload file '" + filePath + "' via '" + elementName + "' on page '" + pageName + "': " + e.getMessage(), "FAIL");
            throw new RuntimeException("Failed to upload file via element: " + elementName + " on page: " + pageName, e);
        }
    }
}
