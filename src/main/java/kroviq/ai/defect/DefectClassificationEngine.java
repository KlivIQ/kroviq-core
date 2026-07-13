package kroviq.ai.defect;

import kroviq.ai.rca.RCAResult;
import kroviq.ai.rca.RootCauseCategory;

public class DefectClassificationEngine {

    public DefectClassification classify(RCAResult rcaResult) {
        if (rcaResult == null) return DefectClassification.UNKNOWN_REVIEW_REQUIRED;

        RootCauseCategory category = rcaResult.getCategory();
        return switch (category) {
            case LOCATOR_ISSUE -> DefectClassification.AUTOMATION_DEFECT;
            case TIMING_SYNC_ISSUE -> classifyTimingIssue(rcaResult);
            case ASSERTION_FAILURE -> DefectClassification.FUNCTIONAL_DEFECT;
            case APPLICATION_DEFECT -> DefectClassification.FUNCTIONAL_DEFECT;
            case ENVIRONMENT_ISSUE -> DefectClassification.ENVIRONMENT_ISSUE;
            case NETWORK_TIMEOUT -> DefectClassification.INFRASTRUCTURE_ISSUE;
            case DRIVER_BROWSER_ISSUE -> DefectClassification.AUTOMATION_DEFECT;
            case API_DEPENDENCY_FAILURE -> DefectClassification.INFRASTRUCTURE_ISSUE;
            case TEST_DATA_ISSUE -> DefectClassification.TEST_DATA_ISSUE;
            case UNKNOWN -> DefectClassification.UNKNOWN_REVIEW_REQUIRED;
        };
    }

    private DefectClassification classifyTimingIssue(RCAResult rcaResult) {
        String evidence = rcaResult.getEvidenceObserved();
        if (evidence != null && (evidence.contains("loading") || evidence.contains("spinner")
                || evidence.contains("slow response") || evidence.contains("performance"))) {
            return DefectClassification.FUNCTIONAL_DEFECT;
        }
        return DefectClassification.AUTOMATION_DEFECT;
    }
}
