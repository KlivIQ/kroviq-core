package kroviq.ai.rca;

import kroviq.ai.rca.rules.RuleVerdict;

public class RCARecommendationEngine {

    public RCAResult buildResult(RCAContext context, RuleVerdict verdict) {
        RootCauseCategory category = verdict.getCategory();
        int confidence = verdict.getConfidence();
        String evidence = verdict.getEvidence();

        String fix = getRecommendedFix(category, context);
        boolean retry = isRetryRecommended(category, confidence);
        boolean defect = isDefectWorthy(category, confidence);
        String severity = getSuggestedSeverity(category, confidence);
        String summary = buildSummary(context, category);

        // Recurrence check
        RCARecurrenceTracker tracker = RCARecurrenceTracker.getInstance();
        String tcId = context.getTestCaseId();
        boolean seenBefore = tracker.isSeenBefore(tcId, category);
        String frequency = tracker.getFailureFrequency(tcId, category);
        boolean recurring = tracker.isRecurringPattern(tcId, category);

        // Recurring failures elevate defect-worthiness
        if (recurring && !defect) {
            defect = true;
            severity = elevate(severity);
        }

        return RCAResult.builder(category, confidence)
                .failureSummary(summary)
                .evidenceObserved(evidence)
                .probableRootCause(getRootCauseDescription(category, evidence))
                .recommendedFix(fix)
                .retryRecommended(retry)
                .defectWorthy(defect)
                .suggestedSeverity(severity)
                .seenBefore(seenBefore)
                .failureFrequency(frequency)
                .recurringPattern(recurring)
                .build();
    }

    private String getRecommendedFix(RootCauseCategory category, RCAContext context) {
        return switch (category) {
            case LOCATOR_ISSUE -> "Review and stabilize the locator. Use semantic attributes (data-testid, aria-label, id) instead of positional XPaths.";
            case TIMING_SYNC_ISSUE -> "Add explicit wait or increase wait timeout. Consider using WaitHandler with appropriate tier.";
            case ASSERTION_FAILURE -> "Verify expected value against current application behavior. Check if requirement changed.";
            case APPLICATION_DEFECT -> "Investigate application behavior. Element/value expected by test is not present — raise defect if confirmed.";
            case ENVIRONMENT_ISSUE -> "Check environment availability. Verify URL, VPN, and service health.";
            case DRIVER_BROWSER_ISSUE -> "Check WebDriver/browser compatibility. Restart browser session. Verify WebDriverManager config.";
            case API_DEPENDENCY_FAILURE -> "Check dependent API/service health. Verify mock/stub availability if applicable.";
            case NETWORK_TIMEOUT -> "Check network connectivity. Verify server response times. Consider increasing page load timeout.";
            case TEST_DATA_ISSUE -> "Verify test data completeness. Check JSON/Excel data source for missing fields.";
            case UNKNOWN -> "Manual investigation required. Review screenshot and step context for clues.";
        };
    }

    private boolean isRetryRecommended(RootCauseCategory category, int confidence) {
        return switch (category) {
            case TIMING_SYNC_ISSUE -> true;
            case NETWORK_TIMEOUT -> true;
            case ENVIRONMENT_ISSUE -> confidence < 85;
            case DRIVER_BROWSER_ISSUE -> false;
            case LOCATOR_ISSUE -> false;
            case ASSERTION_FAILURE -> false;
            case APPLICATION_DEFECT -> false;
            case TEST_DATA_ISSUE -> false;
            case API_DEPENDENCY_FAILURE -> true;
            case UNKNOWN -> true;
        };
    }

    private boolean isDefectWorthy(RootCauseCategory category, int confidence) {
        return switch (category) {
            case APPLICATION_DEFECT -> true;
            case ASSERTION_FAILURE -> confidence >= 80;
            case ENVIRONMENT_ISSUE -> false;
            case TIMING_SYNC_ISSUE -> false;
            case LOCATOR_ISSUE -> false;
            case DRIVER_BROWSER_ISSUE -> false;
            case NETWORK_TIMEOUT -> false;
            case API_DEPENDENCY_FAILURE -> false;
            case TEST_DATA_ISSUE -> false;
            case UNKNOWN -> false;
        };
    }

    private String getSuggestedSeverity(RootCauseCategory category, int confidence) {
        return switch (category) {
            case APPLICATION_DEFECT -> confidence >= 80 ? "High" : "Medium";
            case ASSERTION_FAILURE -> "Medium";
            case ENVIRONMENT_ISSUE -> "High";
            case DRIVER_BROWSER_ISSUE -> "Critical";
            case TIMING_SYNC_ISSUE -> "Low";
            case LOCATOR_ISSUE -> "Low";
            case NETWORK_TIMEOUT -> "Medium";
            case API_DEPENDENCY_FAILURE -> "Medium";
            case TEST_DATA_ISSUE -> "Low";
            case UNKNOWN -> "Medium";
        };
    }

    private String getRootCauseDescription(RootCauseCategory category, String evidence) {
        return switch (category) {
            case LOCATOR_ISSUE -> "Element locator is invalid or fragile — DOM structure may have changed.";
            case TIMING_SYNC_ISSUE -> "DOM refreshed during interaction or element not ready within timeout.";
            case ASSERTION_FAILURE -> "Actual application value does not match expected test assertion.";
            case APPLICATION_DEFECT -> "Application behavior deviates from expected — element/value missing from stable page.";
            case ENVIRONMENT_ISSUE -> "Test environment is unreachable or returning errors.";
            case DRIVER_BROWSER_ISSUE -> "Browser session terminated or WebDriver connection lost.";
            case API_DEPENDENCY_FAILURE -> "Dependent API/service is unavailable or returning errors.";
            case NETWORK_TIMEOUT -> "Network-level timeout — server did not respond within expected time.";
            case TEST_DATA_ISSUE -> "Test data is missing or incomplete for this test case.";
            case UNKNOWN -> "Unable to determine root cause from available evidence.";
        };
    }

    private String buildSummary(RCAContext context, RootCauseCategory category) {
        String step = context.getFailedStep();
        if (step.length() > 60) step = step.substring(0, 60) + "...";
        return category.getDisplayName() + " at step: " + step;
    }

    private String elevate(String severity) {
        return switch (severity) {
            case "Low" -> "Medium";
            case "Medium" -> "High";
            default -> severity;
        };
    }
}
