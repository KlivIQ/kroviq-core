package kroviq.ai.defect;

import kroviq.ai.rca.RCAResult;
import kroviq.ai.rca.RootCauseCategory;

public class DefectSeverityCalculator {

    public String calculate(RCAResult rcaResult, DefectClassification classification) {
        if (rcaResult == null) return "Medium";

        RootCauseCategory category = rcaResult.getCategory();

        // Critical: environment unavailable, crash, complete blocker
        if (category == RootCauseCategory.DRIVER_BROWSER_ISSUE) return "Critical";
        if (category == RootCauseCategory.ENVIRONMENT_ISSUE && rcaResult.getConfidenceScore() >= 85) return "Critical";

        // High: core business action failure (save/submit/payment)
        if (classification == DefectClassification.FUNCTIONAL_DEFECT) {
            if (isBusinessCriticalAction(rcaResult)) return "High";
            return "Medium";
        }

        // Medium: validation mismatch, intermittent
        if (category == RootCauseCategory.ASSERTION_FAILURE) return "Medium";
        if (category == RootCauseCategory.NETWORK_TIMEOUT) return "Medium";
        if (category == RootCauseCategory.API_DEPENDENCY_FAILURE) return "Medium";

        // Low: cosmetic, automation-only issues
        if (classification == DefectClassification.AUTOMATION_DEFECT) return "Low";
        if (classification == DefectClassification.TEST_DATA_ISSUE) return "Low";

        // Recurring patterns elevate severity
        if (rcaResult.isRecurringPattern()) {
            return elevate(rcaResult.getSuggestedSeverity());
        }

        return rcaResult.getSuggestedSeverity() != null ? rcaResult.getSuggestedSeverity() : "Medium";
    }

    private boolean isBusinessCriticalAction(RCAResult rcaResult) {
        String summary = rcaResult.getFailureSummary();
        if (summary == null) return false;
        String lower = summary.toLowerCase();
        return lower.contains("save") || lower.contains("submit") || lower.contains("payment")
                || lower.contains("create") || lower.contains("delete") || lower.contains("approve")
                || lower.contains("login") || lower.contains("checkout");
    }

    private String elevate(String severity) {
        if (severity == null) return "Medium";
        return switch (severity) {
            case "Low" -> "Medium";
            case "Medium" -> "High";
            default -> severity;
        };
    }
}
