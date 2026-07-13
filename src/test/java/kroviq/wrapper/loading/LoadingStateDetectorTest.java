package kroviq.wrapper.loading;

import kroviq.wrapper.core.LoadingStateDetector;
import kroviq.wrapper.factory.GenericLoadingStateDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.Dimension;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoadingStateDetectorTest {

    private WebDriver driver;
    @Mock private WebElement spinnerElement;
    @Mock private WebElement scopeElement;

    private GenericLoadingStateDetector detector;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        detector = new GenericLoadingStateDetector();
        driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        // Default: document.readyState = complete, no jQuery, no indicators
        when(((JavascriptExecutor) driver).executeScript("return document.readyState")).thenReturn("complete");
        when(((JavascriptExecutor) driver).executeScript(contains("jQuery"))).thenReturn(false);
        when(((JavascriptExecutor) driver).executeScript(contains("kroviqActiveFetch"))).thenReturn(false);
        when(driver.findElements(any(By.class))).thenReturn(Collections.emptyList());
    }

    // --- Interface compliance ---

    @Test
    void implementsInterface() {
        assertInstanceOf(LoadingStateDetector.class, detector);
    }

    // --- isPageLoading: document not ready ---

    @Test
    void isPageLoading_documentNotReady_returnsTrue() {
        when(((JavascriptExecutor) driver).executeScript("return document.readyState")).thenReturn("loading");
        assertTrue(detector.isPageLoading(driver));
    }

    @Test
    void isPageLoading_documentComplete_noIndicators_returnsFalse() {
        assertFalse(detector.isPageLoading(driver));
    }

    // --- isPageLoading: jQuery active ---

    @Test
    void isPageLoading_jqueryActive_returnsTrue() {
        when(((JavascriptExecutor) driver).executeScript(contains("jQuery"))).thenReturn(true);
        assertTrue(detector.isPageLoading(driver));
    }

    @Test
    void isPageLoading_antdSpinnerVisible_returnsTrue() {
        when(driver.findElements(By.cssSelector(".ant-spin-spinning"))).thenReturn(List.of(spinnerElement));
        when(spinnerElement.isDisplayed()).thenReturn(true);
        when(spinnerElement.getSize()).thenReturn(new Dimension(50, 50));
        assertTrue(detector.isPageLoading(driver));
    }

    @Test
    void isPageLoading_muiProgressVisible_returnsTrue() {
        when(driver.findElements(By.cssSelector(".MuiCircularProgress-root"))).thenReturn(List.of(spinnerElement));
        when(spinnerElement.isDisplayed()).thenReturn(true);
        when(spinnerElement.getSize()).thenReturn(new Dimension(40, 40));
        assertTrue(detector.isPageLoading(driver));
    }

    @Test
    void isPageLoading_skeletonVisible_returnsTrue() {
        when(driver.findElements(By.cssSelector("[class*='skeleton']"))).thenReturn(List.of(spinnerElement));
        when(spinnerElement.isDisplayed()).thenReturn(true);
        when(spinnerElement.getSize()).thenReturn(new Dimension(200, 30));
        assertTrue(detector.isPageLoading(driver));
    }

    @Test
    void isPageLoading_spinnerHidden_returnsFalse() {
        when(driver.findElements(By.cssSelector(".ant-spin-spinning"))).thenReturn(List.of(spinnerElement));
        when(spinnerElement.isDisplayed()).thenReturn(false);
        assertFalse(detector.isPageLoading(driver));
    }

    // --- isElementLoading: scoped ---

    @Test
    void isElementLoading_scopedSpinnerVisible_returnsTrue() {
        when(scopeElement.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(scopeElement.findElements(By.cssSelector(".ant-spin-spinning"))).thenReturn(List.of(spinnerElement));
        when(spinnerElement.isDisplayed()).thenReturn(true);
        when(spinnerElement.getSize()).thenReturn(new Dimension(50, 50));

        assertTrue(detector.isElementLoading(driver, scopeElement));
    }

    @Test
    void isElementLoading_noScopedSpinner_returnsFalse() {
        when(scopeElement.findElements(any(By.class))).thenReturn(Collections.emptyList());

        assertFalse(detector.isElementLoading(driver, scopeElement));
    }

    @Test
    void isElementLoading_nullScope_delegatesToPageLoading() {
        assertFalse(detector.isElementLoading(driver, null));
    }

    @Test
    void waitForPageReady_alreadyReady_returnsImmediately() {
        long start = System.currentTimeMillis();
        detector.waitForPageReady(driver, Duration.ofSeconds(5));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 2000, "Should complete quickly when page is already ready");
    }

    @Test
    void isPageLoading_ariaBusyTrue_returnsTrue() {
        when(driver.findElements(By.cssSelector("[aria-busy='true']"))).thenReturn(List.of(spinnerElement));
        when(spinnerElement.isDisplayed()).thenReturn(true);
        when(spinnerElement.getSize()).thenReturn(new Dimension(100, 100));
        assertTrue(detector.isPageLoading(driver));
    }
}
