package kroviq.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;
import java.util.function.Function;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import kroviq.wrapper.GenericWrapper;
import kroviq.reporting.StepReportingWrapper;
import kroviq.utils.ActionType;

public class WaitHandler {
    
    private static final Logger logger = LogManager.getLogger(WaitHandler.class);
    private static volatile boolean fatalExecutionStopped = false;

    public static boolean isFatalExecutionStopped() {
        return fatalExecutionStopped;
    }
    
    public enum WaitTier {
        SHORT("wait.short", 5),
        MEDIUM("wait.medium", 15), 
        LONG("wait.long", 30);
        
        private final String configKey;
        private final int defaultTimeout;
        
        WaitTier(String configKey, int defaultTimeout) {
            this.configKey = configKey;
            this.defaultTimeout = defaultTimeout;
        }
        
        public int getTimeout() {
            try {
                return Integer.parseInt(LoadProperties.get(configKey, String.valueOf(defaultTimeout)));
            } catch (Exception e) {
                return defaultTimeout;
            }
        }
    }
    
    
    private static int getRetryAttempts() {
        return Integer.parseInt(LoadProperties.get("wait.retry.attempts", "3"));
    }
    
    private static int getRetryDelay() {
        return Integer.parseInt(LoadProperties.get("wait.retry.delay", "500"));
    }
    
    private static int getPollingInterval() {
        return Integer.parseInt(LoadProperties.get("wait.polling", "200"));
    }
    
    public static WebElement waitForVisibility(By locator, ActionType actionType) {
        return waitForVisibility(locator, actionType.getTier());
    }
    
    public static WebElement waitForVisibility(By locator, WaitTier tier) {
        return executeWithRetry("visibility", () -> {
            WebDriver driver = GenericWrapper.getDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(tier.getTimeout()));
            wait.pollingEvery(Duration.ofMillis(getPollingInterval()));
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        }, tier);
    }
    
    public static WebElement waitForClickable(By locator, ActionType actionType) {
        return waitForClickable(locator, actionType.getTier());
    }
    
    public static WebElement waitForClickable(By locator, WaitTier tier) {
        return executeWithRetry("clickable", () -> {
            WebDriver driver = GenericWrapper.getDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(tier.getTimeout()));
            wait.pollingEvery(Duration.ofMillis(getPollingInterval()));
            return wait.until(ExpectedConditions.elementToBeClickable(locator));
        }, tier);
    }
    
    public static WebElement waitForPresence(By locator, ActionType actionType) {
        return waitForPresence(locator, actionType.getTier());
    }
    
    public static WebElement waitForPresence(By locator, WaitTier tier) {
        return executeWithRetry("presence", () -> {
            WebDriver driver = GenericWrapper.getDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(tier.getTimeout()));
            wait.pollingEvery(Duration.ofMillis(getPollingInterval()));
            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        }, tier);
    }
    
    public static boolean waitForInvisibility(By locator, WaitTier tier) {
        return executeWithRetry("invisibility", () -> {
            WebDriver driver = GenericWrapper.getDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(tier.getTimeout()));
            wait.pollingEvery(Duration.ofMillis(getPollingInterval()));
            return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
        }, tier);
    }
    
    public static <T> T waitForCustomCondition(Function<WebDriver, T> condition, WaitTier tier) {
        return executeWithRetry("custom", () -> {
            WebDriver driver = GenericWrapper.getDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(tier.getTimeout()));
            wait.pollingEvery(Duration.ofMillis(getPollingInterval()));
            return wait.until(condition);
        }, tier);
    }
    
    private static <T> T executeWithRetry(String waitType, WaitOperation<T> operation, WaitTier tier) {
        int attempts = getRetryAttempts();
        int retryDelay = getRetryDelay();
        
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                if (!GenericWrapper.isDriverSessionActive()) {
                    String sessionError = "WebDriver session is unavailable during " + waitType + " wait";
                    logger.error(sessionError);
                    StepReportingWrapper.recordManualStep(sessionError, "Fail");
                    throw new RuntimeException(sessionError);
                }

                long startTime = System.currentTimeMillis();
                T result = operation.execute();
                long duration = System.currentTimeMillis() - startTime;
                
                logger.debug("Wait {} successful on attempt {} in {}ms (tier: {})", 
                    waitType, attempt, duration, tier.name());
                return result;
                
            } catch (StaleElementReferenceException e) {
                logger.warn("Stale element on attempt {} for {} wait (tier: {})", attempt, waitType, tier.name());
                handleRetry(attempt, attempts, retryDelay, waitType, "Stale element reference");
                
            } catch (TimeoutException e) {
                logger.warn("Timeout on attempt {} for {} wait (tier: {}, timeout: {}s)", 
                    attempt, waitType, tier.name(), tier.getTimeout());
                handleRetry(attempt, attempts, retryDelay, waitType, "Timeout after " + tier.getTimeout() + "s");
                
            } catch (NoSuchElementException e) {
                logger.warn("Element not found on attempt {} for {} wait (tier: {})", attempt, waitType, tier.name());
                handleRetry(attempt, attempts, retryDelay, waitType, "Element not found");
                
            } catch (NoSuchSessionException | NoSuchWindowException e) {
                handleFatalSessionLoss(waitType, e);

            } catch (Exception e) {
                if (isSessionLossException(e)) {
                    handleFatalSessionLoss(waitType, e);
                }
                logger.error("Unexpected error on attempt {} for {} wait: {}", attempt, waitType, e.getMessage());
                handleRetry(attempt, attempts, retryDelay, waitType, e.getMessage());
            }
        }
        
        String errorMsg = String.format("Wait %s failed after %d attempts (tier: %s, timeout: %ds)", 
            waitType, attempts, tier.name(), tier.getTimeout());
        logger.error(errorMsg);
        
        // Report to StepReportingWrapper for visibility in reports
        StepReportingWrapper.recordManualStep(errorMsg, "Fail");
        throw new RuntimeException(errorMsg);
    }

    private static void handleFatalSessionLoss(String waitType, Exception e) {
        if (RunManager.isFailFastEnabled()) {
            fatalExecutionStopped = true;
        }
        String errorMsg = "[FATAL] Browser/session lost during " + waitType + " wait. Aborting further execution. Cause: " + e.getMessage();
        logger.error(errorMsg);
        StepReportingWrapper.recordManualStep(errorMsg, "Fail");
        throw new FatalFrameworkException(errorMsg, e);
    }

    private static boolean isSessionLossException(Exception exception) {
        if (!(exception instanceof WebDriverException)) {
            return false;
        }

        String message = exception.getMessage();
        if (message == null) {
            return false;
        }

        String normalized = message.toLowerCase();
        return normalized.contains("invalid session id") ||
               normalized.contains("session deleted") ||
               normalized.contains("target window already closed") ||
               normalized.contains("web view not found") ||
               normalized.contains("disconnected") ||
               normalized.contains("target frame detached") ||
               normalized.contains("session not found");
    }
    
    private static void handleRetry(int attempt, int maxAttempts, int retryDelay, String waitType, String reason) {
        if (attempt < maxAttempts) {
            logger.info("Retrying {} wait in {}ms (attempt {}/{}) - Reason: {}", 
                waitType, retryDelay, attempt + 1, maxAttempts, reason);
            try {
                Thread.sleep(retryDelay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Wait retry interrupted", ie);
            }
        }
    }
    
    // Backward compatibility methods
    public static WebElement waitForElement(By locator, int timeoutSeconds) {
        return executeWithRetry("visibility", () -> {
            WebDriver driver = GenericWrapper.getDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            wait.pollingEvery(Duration.ofMillis(getPollingInterval()));
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        }, WaitTier.MEDIUM);
    }
    
    public static WebElement waitForClickableElement(By locator, int timeoutSeconds) {
        return executeWithRetry("clickable", () -> {
            WebDriver driver = GenericWrapper.getDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            wait.pollingEvery(Duration.ofMillis(getPollingInterval()));
            return wait.until(ExpectedConditions.elementToBeClickable(locator));
        }, WaitTier.MEDIUM);
    }
    
    // Healing-enabled wait methods
    public static WebElement waitForVisibilityWithHealing(By locator, ActionType actionType, String elementName, String pageName) {
        if (!isHealingEnabled(actionType)) {
            return waitForVisibility(locator, convertActionType(actionType));
        }
        
        try {
            return waitForVisibility(locator, convertActionType(actionType));
        } catch (RuntimeException e) {
            logger.debug("Primary locator failed, attempting healing for: {}", locator);
            return attemptHealing(locator, actionType, elementName, pageName, "visibility");
        }
    }
    
    public static WebElement waitForClickableWithHealing(By locator, ActionType actionType, String elementName, String pageName) {
        if (!isHealingEnabled(actionType)) {
            return waitForClickable(locator, convertActionType(actionType));
        }
        
        try {
            return waitForClickable(locator, convertActionType(actionType));
        } catch (RuntimeException e) {
            logger.debug("Primary locator failed, attempting healing for: {}", locator);
            return attemptHealing(locator, actionType, elementName, pageName, "clickable");
        }
    }
    
    public static WebElement waitForPresenceWithHealing(By locator, ActionType actionType, String elementName, String pageName) {
        if (!isHealingEnabled(actionType)) {
            return waitForPresence(locator, convertActionType(actionType));
        }
        
        try {
            return waitForPresence(locator, convertActionType(actionType));
        } catch (RuntimeException e) {
            logger.debug("Primary locator failed, attempting healing for: {}", locator);
            return attemptHealing(locator, actionType, elementName, pageName, "presence");
        }
    }
    
    private static WebElement attemptHealing(By primaryLocator, ActionType actionType, String elementName, String pageName, String waitType) {
        String primaryXPath = primaryLocator.toString().replace("By.xpath: ", "");
        List<String> alternates = SmartLocator.generateAlternateLocators(primaryXPath, actionType, elementName);
        
        if (alternates.isEmpty()) {
            HealingLogger.logHealingDisabled(primaryXPath, actionType, "No alternates generated");
            throw new RuntimeException("Primary locator failed and no healing alternates available: " + primaryXPath);
        }
        
        WaitTier tier = convertActionType(actionType).getTier();
        double timeoutMultiplier = getHealingTimeoutMultiplier();
        int healingTimeout = (int) (tier.getTimeout() * timeoutMultiplier);
        
        for (String alternate : alternates) {
            try {
                By alternateLocator = By.xpath(alternate);
                WebElement element = null;
                
                switch (waitType) {
                    case "visibility":
                        element = executeWithRetry("visibility", () -> {
                            WebDriver driver = GenericWrapper.getDriver();
                            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(healingTimeout));
                            wait.pollingEvery(Duration.ofMillis(getPollingInterval()));
                            return wait.until(ExpectedConditions.visibilityOfElementLocated(alternateLocator));
                        }, tier);
                        break;
                    case "clickable":
                        element = executeWithRetry("clickable", () -> {
                            WebDriver driver = GenericWrapper.getDriver();
                            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(healingTimeout));
                            wait.pollingEvery(Duration.ofMillis(getPollingInterval()));
                            return wait.until(ExpectedConditions.elementToBeClickable(alternateLocator));
                        }, tier);
                        break;
                    case "presence":
                        element = executeWithRetry("presence", () -> {
                            WebDriver driver = GenericWrapper.getDriver();
                            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(healingTimeout));
                            wait.pollingEvery(Duration.ofMillis(getPollingInterval()));
                            return wait.until(ExpectedConditions.presenceOfElementLocated(alternateLocator));
                        }, tier);
                        break;
                }
                
                if (element != null) {
                    HealingLogger.logHealingAttempt(primaryXPath, alternate, true, actionType);
                    logger.info("Healing successful: {} -> {}", primaryXPath, alternate);
                    return element;
                }
                
            } catch (Exception e) {
                HealingLogger.logHealingAttempt(primaryXPath, alternate, false, actionType);
                logger.debug("Healing attempt failed for alternate: {}", alternate);
            }
        }
        
        HealingLogger.logHealingFailure(primaryXPath, actionType, alternates.size());
        throw new RuntimeException("Primary locator failed and all healing attempts exhausted: " + primaryXPath);
    }
    
    private static boolean isHealingEnabled(ActionType actionType) {
        if (!Boolean.parseBoolean(LoadProperties.get("healing.enabled", "false"))) {
            return false;
        }
        
        String actionKey = "healing." + actionType.name().toLowerCase() + ".enabled";
        return Boolean.parseBoolean(LoadProperties.get(actionKey, "false"));
    }
    
    private static double getHealingTimeoutMultiplier() {
        return Double.parseDouble(LoadProperties.get("healing.timeout.multiplier", "2.0"));
    }
    
    private static ActionType convertActionType(ActionType actionType) {
        // No conversion needed - using same ActionType
        return actionType;
    }
    
    @FunctionalInterface
    private interface WaitOperation<T> {
        T execute() throws Exception;
    }
}
