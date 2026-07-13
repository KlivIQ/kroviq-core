package kroviq.wrapper.desktop.wait;

import kroviq.utils.LoadProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.function.Function;

public class DesktopWaitHandler {

    private static final Logger logger = LogManager.getLogger(DesktopWaitHandler.class);

    public enum WaitTier {
        SHORT("desktop.wait.short", 5),
        MEDIUM("desktop.wait.medium", 15),
        LONG("desktop.wait.long", 30);

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

    public static WebElement waitForElement(WebDriver driver, Function<WebDriver, WebElement> condition, WaitTier tier) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(tier.getTimeout()));
        wait.pollingEvery(Duration.ofMillis(getPollingInterval()));
        try {
            WebElement element = wait.until(condition);
            logger.debug("Element found within {} tier ({}s)", tier.name(), tier.getTimeout());
            return element;
        } catch (Exception e) {
            logger.error("Wait for element failed after {}s (tier: {}): {}", tier.getTimeout(), tier.name(), e.getMessage());
            throw new RuntimeException("Desktop element wait failed (tier: " + tier.name() + "): " + e.getMessage(), e);
        }
    }

    public static void waitForElementEnabled(WebDriver driver, Function<WebDriver, WebElement> findElement, WaitTier tier) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(tier.getTimeout()));
        wait.pollingEvery(Duration.ofMillis(getPollingInterval()));
        try {
            wait.until(d -> {
                WebElement el = findElement.apply(d);
                return el != null && el.isEnabled() ? el : null;
            });
            logger.debug("Element enabled within {} tier", tier.name());
        } catch (Exception e) {
            throw new RuntimeException("Wait for element enabled failed (tier: " + tier.name() + "): " + e.getMessage(), e);
        }
    }

    public static boolean waitForElementGone(WebDriver driver, Function<WebDriver, Boolean> goneCondition, WaitTier tier) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(tier.getTimeout()));
        wait.pollingEvery(Duration.ofMillis(getPollingInterval()));
        try {
            return wait.until(goneCondition);
        } catch (Exception e) {
            logger.warn("Wait for element gone timed out (tier: {})", tier.name());
            return false;
        }
    }

    public static void waitForWindow(WebDriver driver, String titleContains, WaitTier tier) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(tier.getTimeout()));
        wait.pollingEvery(Duration.ofMillis(getPollingInterval()));
        try {
            wait.until(d -> {
                String title = d.getTitle();
                return title != null && title.contains(titleContains);
            });
            logger.debug("Window with title containing '{}' found", titleContains);
        } catch (Exception e) {
            throw new RuntimeException("Wait for window '" + titleContains + "' failed (tier: " + tier.name() + "): " + e.getMessage(), e);
        }
    }

    public static void waitForIdle(int milliseconds) {
        int waitMs = milliseconds > 0 ? milliseconds :
                LoadProperties.getInt("desktop.wait.idle.ms", 1000);
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.debug("Idle wait completed: {}ms", waitMs);
    }

    private static int getPollingInterval() {
        return LoadProperties.getInt("desktop.wait.polling", 300);
    }
}
