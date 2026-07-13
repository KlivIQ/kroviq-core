package kroviq.ai.rca;

import kroviq.ai.rca.rules.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.openqa.selenium.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RCAAssistantTest {

    private RCAAssistant assistant;

    @BeforeEach
    void setUp() {
        assistant = new RCAAssistant();
    }

    // --- Driver Rules ---

    @Test
    @DisplayName("SessionNotCreatedException → DRIVER_BROWSER_ISSUE")
    void sessionNotCreated() {
        RCAContext ctx = contextWithExName("Click login", "SessionNotCreatedException", "Could not start a new session");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.DRIVER_BROWSER_ISSUE, result.getCategory());
        assertTrue(result.getConfidenceScore() >= 90);
        assertEquals(FailureOwner.INFRASTRUCTURE, result.getLikelyOwner());
        assertFalse(result.isRetryRecommended());
    }

    @Test
    @DisplayName("WebDriverException with session deleted → DRIVER_BROWSER_ISSUE")
    void webDriverSessionDeleted() {
        RCAContext ctx = contextWithExName("Navigate to page", "WebDriverException", "session deleted because of page crash");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.DRIVER_BROWSER_ISSUE, result.getCategory());
        assertTrue(result.getConfidenceScore() >= 85);
    }

    @Test
    @DisplayName("Browser crashed → DRIVER_BROWSER_ISSUE")
    void browserCrashed() {
        RCAContext ctx = contextWithExName("Click button", "WebDriverException", "chrome not reachable");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.DRIVER_BROWSER_ISSUE, result.getCategory());
    }

    // --- Environment Rules ---

    @Test
    @DisplayName("ConnectException → ENVIRONMENT_ISSUE")
    void connectionRefused() {
        RCAContext ctx = contextWithExName("Navigate to app", "ConnectException", "Connection refused: connect");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.ENVIRONMENT_ISSUE, result.getCategory());
        assertTrue(result.getConfidenceScore() >= 90);
        assertEquals(FailureOwner.ENVIRONMENT, result.getLikelyOwner());
    }

    @Test
    @DisplayName("SSL certificate error → ENVIRONMENT_ISSUE")
    void sslError() {
        RCAContext ctx = contextWithExName("Open page", "WebDriverException", "ERR_CERT_AUTHORITY_INVALID ssl certificate problem");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.ENVIRONMENT_ISSUE, result.getCategory());
    }

    @Test
    @DisplayName("DNS resolution failure → ENVIRONMENT_ISSUE")
    void dnsFailure() {
        RCAContext ctx = contextWithExName("Navigate", "WebDriverException", "ERR_NAME_NOT_RESOLVED");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.ENVIRONMENT_ISSUE, result.getCategory());
        assertTrue(result.getConfidenceScore() >= 88);
    }

    @Test
    @DisplayName("HTTP 503 in error → ENVIRONMENT_ISSUE")
    void http503() {
        RCAContext ctx = contextWithExName("Load page", "RuntimeException", "Server returned 503 service unavailable");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.ENVIRONMENT_ISSUE, result.getCategory());
    }

    @Test
    @DisplayName("Timeout on login page → ENVIRONMENT_ISSUE")
    void timeoutOnLogin() {
        RCAContext ctx = RCAContext.builder("Wait for dashboard", new FakeException("TimeoutException", "Expected condition failed"))
                .currentUrl("https://app.example.com/login/sso")
                .build();
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.ENVIRONMENT_ISSUE, result.getCategory());
    }

    // --- Locator Rules ---

    @Test
    @DisplayName("NoSuchElementException + brittle XPath → LOCATOR_ISSUE")
    void noSuchElementBrittle() {
        RCAContext ctx = RCAContext.builder("Click submit", new FakeException("NoSuchElementException",
                "Unable to locate element: {method: xpath, selector: //div[3]/div[2]/div[1]/span/button}"))
                .locatorUsed("//div[3]/div[2]/div[1]/span/button")
                .build();
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.LOCATOR_ISSUE, result.getCategory());
        assertTrue(result.getConfidenceScore() >= 75);
        assertEquals(FailureOwner.AUTOMATION, result.getLikelyOwner());
        assertFalse(result.isRetryRecommended());
    }

    @Test
    @DisplayName("NoSuchElementException + stable locator → APPLICATION_DEFECT")
    void noSuchElementStable() {
        RCAContext ctx = RCAContext.builder("Click save", new FakeException("NoSuchElementException",
                "Unable to locate element: {method: xpath, selector: //button[@data-testid='save-btn']}"))
                .locatorUsed("//button[@data-testid='save-btn']")
                .build();
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.APPLICATION_DEFECT, result.getCategory());
        assertTrue(result.isDefectWorthy());
    }

    @Test
    @DisplayName("InvalidSelectorException → LOCATOR_ISSUE high confidence")
    void invalidSelector() {
        RCAContext ctx = contextWithExName("Find element", "InvalidSelectorException", "invalid selector: //div[");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.LOCATOR_ISSUE, result.getCategory());
        assertTrue(result.getConfidenceScore() >= 95);
    }

    @Test
    @DisplayName("NoSuchFrameException → LOCATOR_ISSUE")
    void noSuchFrame() {
        RCAContext ctx = contextWithExName("Switch to frame", "NoSuchFrameException", "No frame found");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.LOCATOR_ISSUE, result.getCategory());
    }

    // --- Timing Rules ---

    @Test
    @DisplayName("StaleElementReferenceException → TIMING_SYNC_ISSUE")
    void staleElement() {
        RCAContext ctx = contextWithExName("Click row action", "StaleElementReferenceException",
                "stale element reference: element is not attached to the page document");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.TIMING_SYNC_ISSUE, result.getCategory());
        assertTrue(result.getConfidenceScore() >= 90);
        assertTrue(result.isRetryRecommended());
        assertFalse(result.isDefectWorthy());
        assertEquals("Low", result.getSuggestedSeverity());
    }

    @Test
    @DisplayName("ElementClickInterceptedException → TIMING_SYNC_ISSUE")
    void clickIntercepted() {
        RCAContext ctx = contextWithExName("Click button", "ElementClickInterceptedException",
                "Other element would receive the click: <div class=\"ant-modal-mask\">");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.TIMING_SYNC_ISSUE, result.getCategory());
        assertTrue(result.getConfidenceScore() >= 80);
        assertTrue(result.isRetryRecommended());
    }

    @Test
    @DisplayName("TimeoutException + loading spinner in DOM → TIMING_SYNC_ISSUE high confidence")
    void timeoutWithSpinner() {
        RCAContext ctx = RCAContext.builder("Wait for table", new FakeException("TimeoutException", "Expected condition failed"))
                .pageSource("<div class=\"ant-spin ant-spin-spinning\"><span class=\"ant-spin-dot\"></span></div>")
                .currentUrl("https://app.example.com/dashboard")
                .build();
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.TIMING_SYNC_ISSUE, result.getCategory());
        assertTrue(result.getConfidenceScore() >= 85);
    }

    @Test
    @DisplayName("TimeoutException generic → TIMING_SYNC_ISSUE")
    void timeoutGeneric() {
        RCAContext ctx = RCAContext.builder("Wait for element", new FakeException("TimeoutException", "waiting for visibility of element"))
                .currentUrl("https://app.example.com/products")
                .build();
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.TIMING_SYNC_ISSUE, result.getCategory());
    }

    @Test
    @DisplayName("ElementNotInteractableException → TIMING_SYNC_ISSUE")
    void elementNotInteractable() {
        RCAContext ctx = contextWithExName("Enter text", "ElementNotInteractableException", "element not interactable");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.TIMING_SYNC_ISSUE, result.getCategory());
    }

    // --- Assertion Rules ---

    @Test
    @DisplayName("AssertionError with expected/actual → ASSERTION_FAILURE")
    void assertionMismatch() {
        RCAContext ctx = contextWithExName("Verify status", "AssertionError",
                "expected: [Active] but was: [Inactive]");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.ASSERTION_FAILURE, result.getCategory());
        assertTrue(result.getConfidenceScore() >= 75);
    }

    @Test
    @DisplayName("ComparisonFailure → ASSERTION_FAILURE")
    void comparisonFailure() {
        RCAContext ctx = contextWithExName("Verify count", "ComparisonFailure",
                "expected:<[5]> but was:<[3]>");
        RCAResult result = assistant.analyze(ctx);
        assertTrue(result.getCategory() == RootCauseCategory.ASSERTION_FAILURE
                || result.getCategory() == RootCauseCategory.APPLICATION_DEFECT);
    }

    // --- Unknown ---

    @Test
    @DisplayName("Unknown exception → UNKNOWN category")
    void unknownException() {
        RCAContext ctx = contextWithExName("Do something", "SomeRandomException", "something went wrong");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.UNKNOWN, result.getCategory());
        assertTrue(result.getConfidenceScore() <= 20);
    }

    @Test
    @DisplayName("Null exception → UNKNOWN category")
    void nullException() {
        RCAContext ctx = RCAContext.builder("Failed step", null).build();
        RCAResult result = assistant.analyze(ctx);
        assertEquals(RootCauseCategory.UNKNOWN, result.getCategory());
    }

    // --- Ownership Classification ---

    @Test
    @DisplayName("Locator issues owned by Automation")
    void locatorOwnership() {
        RCAContext ctx = contextWithExName("Click", "InvalidSelectorException", "bad selector");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(FailureOwner.AUTOMATION, result.getLikelyOwner());
    }

    @Test
    @DisplayName("Environment issues owned by Environment")
    void environmentOwnership() {
        RCAContext ctx = contextWithExName("Navigate", "ConnectException", "Connection refused");
        RCAResult result = assistant.analyze(ctx);
        assertEquals(FailureOwner.ENVIRONMENT, result.getLikelyOwner());
    }

    // --- Recurrence Tracking ---

    @Test
    @DisplayName("Recurrence tracker records and detects patterns")
    void recurrenceTracking() {
        RCARecurrenceTracker tracker = RCARecurrenceTracker.getInstance();
        tracker.reset();

        String tcId = "TC_TEST_001";
        RootCauseCategory cat = RootCauseCategory.TIMING_SYNC_ISSUE;

        // First failure — not seen before
        tracker.recordExecution(tcId);
        tracker.recordFailure(tcId, cat);
        assertFalse(tracker.isSeenBefore(tcId, cat));

        // Second failure — now seen before
        tracker.recordExecution(tcId);
        tracker.recordFailure(tcId, cat);
        assertTrue(tracker.isSeenBefore(tcId, cat));
        assertEquals("2/2 runs", tracker.getFailureFrequency(tcId, cat));

        // Third execution with failure — recurring pattern
        tracker.recordExecution(tcId);
        tracker.recordFailure(tcId, cat);
        assertTrue(tracker.isRecurringPattern(tcId, cat));
    }

    // --- RCA Result Builder ---

    @Test
    @DisplayName("RCAResult confidence clamped to 0-100")
    void confidenceClamping() {
        RCAResult result = RCAResult.builder(RootCauseCategory.UNKNOWN, 150).build();
        assertEquals(100, result.getConfidenceScore());

        RCAResult result2 = RCAResult.builder(RootCauseCategory.UNKNOWN, -10).build();
        assertEquals(0, result2.getConfidenceScore());
    }

    // --- Report Renderer ---

    @Test
    @DisplayName("RCA report card renders valid HTML")
    void reportCardRendering() {
        RCAResult result = RCAResult.builder(RootCauseCategory.TIMING_SYNC_ISSUE, 90)
                .failureSummary("Timing issue at step: Click button")
                .evidenceObserved("DOM refreshed during interaction")
                .probableRootCause("Stale element reference")
                .recommendedFix("Add explicit wait")
                .retryRecommended(true)
                .defectWorthy(false)
                .suggestedSeverity("Low")
                .build();

        String html = RCAReportRenderer.renderHtmlCard(result);
        assertNotNull(html);
        assertTrue(html.contains("Failure RCA"));
        assertTrue(html.contains("Timing/Sync Issue"));
        assertTrue(html.contains("90% confidence"));
        assertTrue(html.contains("Automation"));
        assertTrue(html.contains("Retry: Yes"));
        assertTrue(html.contains("Defect: No"));
    }

    @Test
    @DisplayName("RCA CSS styles are non-empty")
    void cssStyles() {
        String css = RCAReportRenderer.getRcaCssStyles();
        assertNotNull(css);
        assertTrue(css.contains(".rca-card"));
        assertTrue(css.contains(".rca-header"));
    }

    // --- Helper methods ---

    private RCAContext context(String step, Throwable ex) {
        return RCAContext.builder(step, ex).build();
    }

    private RCAContext contextWithExName(String step, String exceptionClassName, String message) {
        return RCAContext.builder(step, new FakeException(exceptionClassName, message)).build();
    }

    /**
     * Fake exception that allows controlling the class simple name for testing.
     */
    private static class FakeException extends RuntimeException {
        private final String fakeName;

        FakeException(String fakeName, String message) {
            super(message);
            this.fakeName = fakeName;
        }

        @Override
        public String toString() {
            return fakeName + ": " + getMessage();
        }
    }
}
