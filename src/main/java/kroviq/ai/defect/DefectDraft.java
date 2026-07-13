package kroviq.ai.defect;

import java.util.Collections;
import java.util.List;

public class DefectDraft {
    private final String defectTitle;
    private final String executiveSummary;
    private final String reproductionSteps;
    private final String expectedResult;
    private final String actualResult;
    private final String probableRootCause;
    private final DefectClassification defectClassification;
    private final String likelyOwner;
    private final String severity;
    private final boolean retryRecommended;
    private final boolean defectRecommended;
    private final String impactedModule;
    private final String testCaseId;
    private final List<String> screenshots;
    private final String logs;
    private final String environmentDetails;

    private DefectDraft(Builder builder) {
        this.defectTitle = builder.defectTitle;
        this.executiveSummary = builder.executiveSummary;
        this.reproductionSteps = builder.reproductionSteps;
        this.expectedResult = builder.expectedResult;
        this.actualResult = builder.actualResult;
        this.probableRootCause = builder.probableRootCause;
        this.defectClassification = builder.defectClassification;
        this.likelyOwner = builder.likelyOwner;
        this.severity = builder.severity;
        this.retryRecommended = builder.retryRecommended;
        this.defectRecommended = builder.defectRecommended;
        this.impactedModule = builder.impactedModule;
        this.testCaseId = builder.testCaseId;
        this.screenshots = Collections.unmodifiableList(builder.screenshots);
        this.logs = builder.logs;
        this.environmentDetails = builder.environmentDetails;
    }

    public String getDefectTitle() { return defectTitle; }
    public String getExecutiveSummary() { return executiveSummary; }
    public String getReproductionSteps() { return reproductionSteps; }
    public String getExpectedResult() { return expectedResult; }
    public String getActualResult() { return actualResult; }
    public String getProbableRootCause() { return probableRootCause; }
    public DefectClassification getDefectClassification() { return defectClassification; }
    public String getLikelyOwner() { return likelyOwner; }
    public String getSeverity() { return severity; }
    public boolean isRetryRecommended() { return retryRecommended; }
    public boolean isDefectRecommended() { return defectRecommended; }
    public String getImpactedModule() { return impactedModule; }
    public String getTestCaseId() { return testCaseId; }
    public List<String> getScreenshots() { return screenshots; }
    public String getLogs() { return logs; }
    public String getEnvironmentDetails() { return environmentDetails; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String defectTitle = "";
        private String executiveSummary = "";
        private String reproductionSteps = "";
        private String expectedResult = "";
        private String actualResult = "";
        private String probableRootCause = "";
        private DefectClassification defectClassification = DefectClassification.UNKNOWN_REVIEW_REQUIRED;
        private String likelyOwner = "Unknown";
        private String severity = "Medium";
        private boolean retryRecommended = false;
        private boolean defectRecommended = false;
        private String impactedModule = "";
        private String testCaseId = "";
        private List<String> screenshots = Collections.emptyList();
        private String logs = "";
        private String environmentDetails = "";

        public Builder defectTitle(String v) { this.defectTitle = v; return this; }
        public Builder executiveSummary(String v) { this.executiveSummary = v; return this; }
        public Builder reproductionSteps(String v) { this.reproductionSteps = v; return this; }
        public Builder expectedResult(String v) { this.expectedResult = v; return this; }
        public Builder actualResult(String v) { this.actualResult = v; return this; }
        public Builder probableRootCause(String v) { this.probableRootCause = v; return this; }
        public Builder defectClassification(DefectClassification v) { this.defectClassification = v; return this; }
        public Builder likelyOwner(String v) { this.likelyOwner = v; return this; }
        public Builder severity(String v) { this.severity = v; return this; }
        public Builder retryRecommended(boolean v) { this.retryRecommended = v; return this; }
        public Builder defectRecommended(boolean v) { this.defectRecommended = v; return this; }
        public Builder impactedModule(String v) { this.impactedModule = v; return this; }
        public Builder testCaseId(String v) { this.testCaseId = v; return this; }
        public Builder screenshots(List<String> v) { this.screenshots = v != null ? v : Collections.emptyList(); return this; }
        public Builder logs(String v) { this.logs = v; return this; }
        public Builder environmentDetails(String v) { this.environmentDetails = v; return this; }

        public DefectDraft build() { return new DefectDraft(this); }
    }
}
