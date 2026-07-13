package kroviq.wrapper.factory;

import kroviq.utils.LoadProperties;
import kroviq.wrapper.core.LoadingStateDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class GenericLoadingStateDetector implements LoadingStateDetector {

    private static final Logger logger = LogManager.getLogger(GenericLoadingStateDetector.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = LoadProperties.getInt("loading.timeout.seconds", 15);
    private static final int POLL_INTERVAL_MS = LoadProperties.getInt("loading.poll.interval.ms", 200);
    private static final int SETTLE_MS = LoadProperties.getInt("loading.settle.ms", 200);

    private static final String[] LOADING_SELECTORS = {
            // AntD
            ".ant-spin-spinning",
            ".ant-skeleton-active",
            ".ant-table-loading",

            // AG Grid
            ".ag-overlay-loading-wrapper:not([style*='display: none'])",

            // MUI
            ".MuiCircularProgress-root",
            ".MuiLinearProgress-root",
            ".MuiSkeleton-root",
            ".MuiBackdrop-root[class*='open']",
            ".MuiDataGrid-overlay .MuiCircularProgress-root",

            // Angular Material
            "mat-progress-bar",
            "mat-progress-spinner",
            "mat-spinner",
            ".mat-mdc-progress-bar",
            ".mat-mdc-progress-spinner",

            // PrimeNG
            ".p-datatable-loading-overlay",
            ".p-progressSpinner",
            "p-progressSpinner",
            ".p-blockui",

            // ARIA standard
            "[aria-busy='true']",

            // Skeleton screens (generic)
            "[class*='skeleton']",
            "[class*='shimmer']",
            "[class*='placeholder-glow']",

            // Full-page overlays & blockers
            ".overlay:not([style*='display: none'])",
            ".loading-overlay:not([style*='display: none'])",
            ".page-loader:not([style*='display: none'])",
            ".modal-backdrop.show",
            ".blockUI",

            // Generic loading containers
            "[class*='loading']:not([style*='display: none']):not(body):not(html)",
            "[class*='spinner']:not([style*='display: none'])"
    };

    @Override
    public boolean isPageLoading(WebDriver driver) {
        // Check 1: document.readyState
        if (!isDocumentReady(driver)) return true;

        // Check 2: jQuery active requests (if jQuery present)
        if (hasActiveJQueryRequests(driver)) return true;

        // Check 3: Active fetch/XHR (if instrumented)
        if (hasActiveFetchRequests(driver)) return true;

        // Check 4: Visible loading indicators in DOM
        return hasVisibleLoadingIndicators(driver, null);
    }

    @Override
    public void waitForPageReady(WebDriver driver) {
        waitForPageReady(driver, Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
    }

    @Override
    public void waitForPageReady(WebDriver driver, Duration timeout) {
        long start = System.currentTimeMillis();

        try {
            new WebDriverWait(driver, timeout)
                    .pollingEvery(Duration.ofMillis(POLL_INTERVAL_MS))
                    .until(d -> !isPageLoading(d));
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            logger.warn("[LoadingStateDetector] Page not ready after {}ms (timeout: {}ms). Proceeding.",
                    elapsed, timeout.toMillis());
            return;
        }

        // Settle wait for final DOM stabilization
        try { Thread.sleep(SETTLE_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        long elapsed = System.currentTimeMillis() - start;
        logger.debug("[LoadingStateDetector] Page ready in {}ms", elapsed);
    }

    @Override
    public boolean isElementLoading(WebDriver driver, WebElement scope) {
        if (scope == null) return isPageLoading(driver);
        return hasVisibleLoadingIndicators(driver, scope);
    }

    private boolean isDocumentReady(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object state = js.executeScript("return document.readyState");
            return "complete".equals(state);
        } catch (Exception e) {
            return true; // Assume ready on failure
        }
    }

    private boolean hasActiveJQueryRequests(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript(
                    "return (typeof jQuery !== 'undefined' && jQuery.active > 0) ? true : false;");
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasActiveFetchRequests(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript(
                    "return (window.__kroviqActiveFetch > 0) ? true : false;");
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasVisibleLoadingIndicators(WebDriver driver, WebElement scope) {
        for (String selector : LOADING_SELECTORS) {
            try {
                List<WebElement> indicators;
                if (scope != null) {
                    indicators = scope.findElements(By.cssSelector(selector));
                } else {
                    indicators = driver.findElements(By.cssSelector(selector));
                }
                for (WebElement ind : indicators) {
                    try {
                        if (ind.isDisplayed() && ind.getSize().getHeight() > 0) {
                            logger.debug("[LoadingStateDetector] Active indicator: {}", selector);
                            return true;
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}
