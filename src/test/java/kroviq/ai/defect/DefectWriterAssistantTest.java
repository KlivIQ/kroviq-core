package kroviq.ai.defect;

import kroviq.ai.rca.FailureOwner;
import kroviq.ai.rca.RCAResult;
import kroviq.ai.rca.RootCauseCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DefectWriterAssistant Tests")
class DefectWriterAssistantTest {

    private DefectWriterAssistant writer;

    @BeforeEach
    void setUp() {
        writer = new DefectWriterAssistant();
    }

    // --- Scenario 1: Locator Failure ---

    @Test
    @DisplayName("Locator failure produces AUTOMATION_DEFECT classification")
    void locatorFailure_classifiesAsAutomationDefect() {
        RCAResult rca = buildRCA(RootCauseCategory.LOCATOR_ISSUE, 85,
                "NoSuchElementException: Unable to locate element By.xpath: //button[@id='save']",
                "Element locator is invalid or fragile — DOM structure may have changed.");

        DefectContext context = buildContext(rca, "Click on Save button", "TC_PRODUCT_001", "Product");
        DefectDraft draft = writer.generate(context);

        assertEquals(DefectClassification.AUTOMATION_DEFECT, draft.getDefectClassification());
        assertEquals("Low", draft.getSeverity());
        assertEquals("Automation", draft.getLikelyOwner());
        assertFalse(draft.isDefectRecommended());
        assertFalse(draft.isRetryRecommended());
        assertTrue(draft.getDefectTitle().contains("Product"));
        assertTrue(draft.getDefectTitle().contains("locator issue"));
    }

    @Test
    @DisplayName("Locator failure title format is correct")
    void locatorFailure_titleFormat() {
        RCAResult rca = buildRCA(RootCauseCategory.LOCATOR_ISSUE, 90,
                "NoSuchElementException", "Locator fragile");

        DefectContext context = buildContext(rca, "Click on Submit button", "TC_LOGIN_001", "Login");
        DefectDraft draft = writer.generate(context);

        assertTrue(draft.getDefectTitle().startsWith("Login - "));
        assertTrue(draft.getDefectTitle().contains("failed due to locator issue"));
    }

    // --- Scenario 2: Assertion Failure ---

    @Test
    @DisplayName("Assertion failure produces FUNCTIONAL_DEFECT classification")
    void assertionFailure_classifiesAsFunctionalDefect() {
        RCAResult rca = buildRCA(RootCauseCategory.ASSERTION_FAILURE, 88,
                "AssertionError: expected [Active] but found [Inactive]",
                "Actual application value does not match expected test assertion.");

        DefectContext context = buildContext(rca, "Verify status is Active", "TC_ORDER_005", "Order");
        DefectDraft draft = writer.generate(context);

        assertEquals(DefectClassification.FUNCTIONAL_DEFECT, draft.getDefectClassification());
        assertEquals("Medium", draft.getSeverity());
        assertEquals("Application", draft.getLikelyOwner());
        assertTrue(draft.isDefectRecommended());
        assertFalse(draft.isRetryRecommended());
        assertTrue(draft.getDefectTitle().contains("Order"));
        assertTrue(draft.getDefectTitle().contains("assertion failure"));
    }

    @Test
    @DisplayName("Assertion failure with business-critical action gets High severity")
    void assertionFailure_businessCritical_highSeverity() {
        RCAResult rca = RCAResult.builder(RootCauseCategory.ASSERTION_FAILURE, 90)
                .failureSummary("Assertion Failure at step: Verify save confirmation message")
                .evidenceObserved("AssertionError: expected [Saved] but found [Error]")
                .probableRootCause("Actual value mismatch")
                .likelyOwner(FailureOwner.APPLICATION)
                .retryRecommended(false)
                .defectWorthy(true)
                .suggestedSeverity("Medium")
                .build();

        DefectContext context = buildContext(rca, "Verify save confirmation message", "TC_PRODUCT_002", "Product");
        DefectDraft draft = writer.generate(context);

        assertEquals(DefectClassification.FUNCTIONAL_DEFECT, draft.getDefectClassification());
        assertEquals("High", draft.getSeverity());
    }

    // --- Scenario 3: Timing Issue ---

    @Test
    @DisplayName("Timing issue produces AUTOMATION_DEFECT classification by default")
    void timingIssue_classifiesAsAutomationDefect() {
        RCAResult rca = buildRCA(RootCauseCategory.TIMING_SYNC_ISSUE, 75,
                "StaleElementReferenceException: element is not attached to the page document",
                "DOM refreshed during interaction or element not ready within timeout.");

        DefectContext context = buildContext(rca, "Click on Edit button", "TC_CONFIG_003", "Config");
        DefectDraft draft = writer.generate(context);

        assertEquals(DefectClassification.AUTOMATION_DEFECT, draft.getDefectClassification());
        assertEquals("Low", draft.getSeverity());
        assertEquals("Automation", draft.getLikelyOwner());
        assertTrue(draft.isRetryRecommended());
    }

    @Test
    @DisplayName("Timing issue with loading indicator evidence classifies as FUNCTIONAL_DEFECT")
    void timingIssue_withLoadingEvidence_classifiesAsFunctional() {
        RCAResult rca = buildRCA(RootCauseCategory.TIMING_SYNC_ISSUE, 70,
                "TimeoutException: loading spinner still visible after 30s",
                "DOM refreshed during interaction.");

        DefectContext context = buildContext(rca, "Wait for page load", "TC_DASH_001", "Dashboard");
        DefectDraft draft = writer.generate(context);

        assertEquals(DefectClassification.FUNCTIONAL_DEFECT, draft.getDefectClassification());
    }

    // --- Scenario 4: Environment Issue ---

    @Test
    @DisplayName("Environment issue produces ENVIRONMENT_ISSUE classification")
    void environmentIssue_classifiesAsEnvironmentIssue() {
        RCAResult rca = buildRCA(RootCauseCategory.ENVIRONMENT_ISSUE, 92,
                "WebDriverException: net::ERR_CONNECTION_REFUSED",
                "Test environment is unreachable or returning errors.");

        DefectContext context = buildContext(rca, "Navigate to application", "TC_LOGIN_001", "Login");
        DefectDraft draft = writer.generate(context);

        assertEquals(DefectClassification.ENVIRONMENT_ISSUE, draft.getDefectClassification());
        assertEquals("Critical", draft.getSeverity());
        assertEquals("Environment", draft.getLikelyOwner());
        assertFalse(draft.isDefectRecommended());
    }

    @Test
    @DisplayName("Environment issue below confidence threshold gets High severity")
    void environmentIssue_lowConfidence_highSeverity() {
        RCAResult rca = buildRCA(RootCauseCategory.ENVIRONMENT_ISSUE, 60,
                "Connection timeout", "Environment unreachable");

        DefectContext context = buildContext(rca, "Open URL", "TC_LOGIN_001", "Login");
        DefectDraft draft = writer.generate(context);

        assertEquals(DefectClassification.ENVIRONMENT_ISSUE, draft.getDefectClassification());
        // Below 85 confidence, not Critical
        assertNotEquals("Critical", draft.getSeverity());
    }

    // --- Network/Infrastructure ---

    @Test
    @DisplayName("Network timeout produces INFRASTRUCTURE_ISSUE classification")
    void networkTimeout_classifiesAsInfrastructure() {
        RCAResult rca = buildRCA(RootCauseCategory.NETWORK_TIMEOUT, 80,
                "TimeoutException: page load timeout", "Network timeout");

        DefectContext context = buildContext(rca, "Load dashboard", "TC_DASH_001", "Dashboard");
        DefectDraft draft = writer.generate(context);

        assertEquals(DefectClassification.INFRASTRUCTURE_ISSUE, draft.getDefectClassification());
        assertEquals("Medium", draft.getSeverity());
    }

    // --- Driver/Browser ---

    @Test
    @DisplayName("Driver issue produces Critical severity")
    void driverIssue_criticalSeverity() {
        RCAResult rca = buildRCA(RootCauseCategory.DRIVER_BROWSER_ISSUE, 95,
                "SessionNotCreatedException: session deleted", "Browser crashed");

        DefectContext context = buildContext(rca, "Click button", "TC_TEST_001", "Test");
        DefectDraft draft = writer.generate(context);

        assertEquals(DefectClassification.AUTOMATION_DEFECT, draft.getDefectClassification());
        assertEquals("Critical", draft.getSeverity());
    }

    // --- Test Data ---

    @Test
    @DisplayName("Test data issue produces TEST_DATA_ISSUE classification")
    void testDataIssue_classifiesAsTestData() {
        RCAResult rca = buildRCA(RootCauseCategory.TEST_DATA_ISSUE, 85,
                "MissingDataException: field 'Username' not found", "Test data missing");

        DefectContext context = buildContext(rca, "Enter username", "TC_LOGIN_001", "Login");
        DefectDraft draft = writer.generate(context);

        assertEquals(DefectClassification.TEST_DATA_ISSUE, draft.getDefectClassification());
        assertEquals("Low", draft.getSeverity());
        assertEquals("Test Data", draft.getLikelyOwner());
    }

    // --- Unknown ---

    @Test
    @DisplayName("Unknown category produces UNKNOWN_REVIEW_REQUIRED classification")
    void unknownCategory_classifiesAsUnknown() {
        RCAResult rca = buildRCA(RootCauseCategory.UNKNOWN, 15,
                "Unexpected error occurred", "Unable to determine");

        DefectContext context = buildContext(rca, "Some step", "TC_X_001", "X");
        DefectDraft draft = writer.generate(context);

        assertEquals(DefectClassification.UNKNOWN_REVIEW_REQUIRED, draft.getDefectClassification());
    }

    // --- Output Completeness ---

    @Test
    @DisplayName("All DefectDraft fields are populated")
    void allFieldsPopulated() {
        RCAResult rca = buildRCA(RootCauseCategory.ASSERTION_FAILURE, 85,
                "AssertionError: expected [OK] but found [Error]", "Value mismatch");

        DefectContext context = DefectContext.builder(rca)
                .failedStep("Verify status shows OK")
                .testCaseId("TC_ORDER_001")
                .moduleName("Order")
                .scenarioName("Verify order status")
                .previousSteps(Arrays.asList("Navigate to Orders", "Click on first order"))
                .screenshotPaths(Arrays.asList("screenshots/fail_001.png"))
                .currentUrl("https://app.example.com/orders")
                .environmentName("https://app.example.com")
                .browser("chrome")
                .exceptionMessage("AssertionError: expected [OK] but found [Error]")
                .build();

        DefectDraft draft = writer.generate(context);

        assertNotNull(draft.getDefectTitle());
        assertFalse(draft.getDefectTitle().isEmpty());
        assertNotNull(draft.getExecutiveSummary());
        assertFalse(draft.getExecutiveSummary().isEmpty());
        assertNotNull(draft.getReproductionSteps());
        assertTrue(draft.getReproductionSteps().contains("Navigate to Orders"));
        assertTrue(draft.getReproductionSteps().contains("[FAILED]"));
        assertNotNull(draft.getExpectedResult());
        assertNotNull(draft.getActualResult());
        assertNotNull(draft.getProbableRootCause());
        assertNotNull(draft.getDefectClassification());
        assertNotNull(draft.getLikelyOwner());
        assertNotNull(draft.getSeverity());
        assertEquals("Order", draft.getImpactedModule());
        assertEquals("TC_ORDER_001", draft.getTestCaseId());
        assertEquals(1, draft.getScreenshots().size());
        assertNotNull(draft.getLogs());
        assertNotNull(draft.getEnvironmentDetails());
        assertTrue(draft.getEnvironmentDetails().contains("chrome"));
    }

    // --- Renderer Tests ---

    @Test
    @DisplayName("HTML card renders without errors")
    void htmlCardRendersSuccessfully() {
        DefectDraft draft = buildSampleDraft();
        String html = DefectTemplateRenderer.renderHtmlCard(draft);

        assertNotNull(html);
        assertTrue(html.contains("defect-card"));
        assertTrue(html.contains("AI Defect Draft"));
        assertTrue(html.contains(draft.getDefectTitle()));
        assertTrue(html.contains(draft.getDefectClassification().getDisplayName()));
    }

    @Test
    @DisplayName("Markdown renders copy-paste ready format")
    void markdownRendersSuccessfully() {
        DefectDraft draft = buildSampleDraft();
        String md = DefectTemplateRenderer.renderMarkdown(draft);

        assertNotNull(md);
        assertTrue(md.contains("## "));
        assertTrue(md.contains("**Classification:**"));
        assertTrue(md.contains("**Severity:**"));
        assertTrue(md.contains("### Reproduction Steps"));
        assertTrue(md.contains("### Expected Result"));
        assertTrue(md.contains("### Actual Result"));
    }

    @Test
    @DisplayName("Plain text renders email-friendly format")
    void plainTextRendersSuccessfully() {
        DefectDraft draft = buildSampleDraft();
        String txt = DefectTemplateRenderer.renderPlainText(draft);

        assertNotNull(txt);
        assertTrue(txt.contains("DEFECT DRAFT"));
        assertTrue(txt.contains("Title:"));
        assertTrue(txt.contains("Classification:"));
        assertTrue(txt.contains("REPRODUCTION STEPS"));
    }

    @Test
    @DisplayName("Null draft returns empty string for all renderers")
    void nullDraft_returnsEmpty() {
        assertEquals("", DefectTemplateRenderer.renderHtmlCard(null));
        assertEquals("", DefectTemplateRenderer.renderMarkdown(null));
        assertEquals("", DefectTemplateRenderer.renderPlainText(null));
    }

    // --- Classification Engine Tests ---

    @Test
    @DisplayName("Classification engine handles null RCAResult")
    void classificationEngine_nullResult() {
        DefectClassificationEngine engine = new DefectClassificationEngine();
        assertEquals(DefectClassification.UNKNOWN_REVIEW_REQUIRED, engine.classify(null));
    }

    // --- Title Generator Tests ---

    @Test
    @DisplayName("Title generator extracts module from TC ID when module is empty")
    void titleGenerator_extractsModuleFromTcId() {
        RCAResult rca = buildRCA(RootCauseCategory.LOCATOR_ISSUE, 80, "error", "locator");
        DefectContext context = DefectContext.builder(rca)
                .failedStep("Click save")
                .testCaseId("TC_PRODUCT_CONFIG_001")
                .moduleName("")
                .build();

        DefectTitleGenerator gen = new DefectTitleGenerator();
        String title = gen.generate(context);

        assertTrue(title.contains("ProductConfig"));
    }

    // --- Severity Calculator Tests ---

    @Test
    @DisplayName("Severity calculator returns Medium for null RCAResult")
    void severityCalculator_nullResult() {
        DefectSeverityCalculator calc = new DefectSeverityCalculator();
        assertEquals("Medium", calc.calculate(null, DefectClassification.UNKNOWN_REVIEW_REQUIRED));
    }

    @Test
    @DisplayName("Recurring pattern elevates severity")
    void severityCalculator_recurringElevates() {
        RCAResult rca = RCAResult.builder(RootCauseCategory.UNKNOWN, 30)
                .suggestedSeverity("Low")
                .recurringPattern(true)
                .build();

        DefectSeverityCalculator calc = new DefectSeverityCalculator();
        String severity = calc.calculate(rca, DefectClassification.UNKNOWN_REVIEW_REQUIRED);
        assertEquals("Medium", severity);
    }

    // --- Helpers ---

    private RCAResult buildRCA(RootCauseCategory category, int confidence, String evidence, String rootCause) {
        return RCAResult.builder(category, confidence)
                .failureSummary(category.getDisplayName() + " at step: test step")
                .evidenceObserved(evidence)
                .probableRootCause(rootCause)
                .retryRecommended(category == RootCauseCategory.TIMING_SYNC_ISSUE)
                .defectWorthy(category == RootCauseCategory.ASSERTION_FAILURE && confidence >= 80)
                .suggestedSeverity(category == RootCauseCategory.ENVIRONMENT_ISSUE ? "High" : "Medium")
                .build();
    }

    private DefectContext buildContext(RCAResult rca, String failedStep, String tcId, String module) {
        return DefectContext.builder(rca)
                .failedStep(failedStep)
                .testCaseId(tcId)
                .moduleName(module)
                .previousSteps(Arrays.asList("Step 1: Navigate to page", "Step 2: Enter data"))
                .environmentName("https://qa.example.com")
                .browser("chrome")
                .build();
    }

    private DefectDraft buildSampleDraft() {
        return DefectDraft.builder()
                .defectTitle("Order - Verify status failed due to assertion failure")
                .executiveSummary("Test case TC_ORDER_001 failed during Order module execution.")
                .reproductionSteps("1. Navigate to Orders\n2. Click first order\n3. [FAILED] Verify status")
                .expectedResult("Status should show 'Active'")
                .actualResult("Step failed with: AssertionError: expected [Active] but found [Inactive]")
                .probableRootCause("Actual application value does not match expected test assertion.")
                .defectClassification(DefectClassification.FUNCTIONAL_DEFECT)
                .likelyOwner("Application")
                .severity("Medium")
                .retryRecommended(false)
                .defectRecommended(true)
                .impactedModule("Order")
                .testCaseId("TC_ORDER_001")
                .screenshots(List.of("screenshots/fail.png"))
                .logs("Failed Step: Verify status\nURL: https://app.example.com/orders")
                .environmentDetails("URL: https://qa.example.com | Browser: chrome")
                .build();
    }
}
