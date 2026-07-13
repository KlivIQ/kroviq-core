package kroviq.ai.rca;

public class RCAResult {
    private final String failureSummary;
    private final RootCauseCategory category;
    private final int confidenceScore;
    private final FailureOwner likelyOwner;
    private final String evidenceObserved;
    private final String probableRootCause;
    private final String recommendedFix;
    private final boolean retryRecommended;
    private final boolean defectWorthy;
    private final String suggestedSeverity;
    // Recurrence signal
    private final boolean seenBefore;
    private final String failureFrequency;
    private final boolean recurringPattern;

    private RCAResult(Builder builder) {
        this.failureSummary = builder.failureSummary;
        this.category = builder.category;
        this.confidenceScore = builder.confidenceScore;
        this.likelyOwner = builder.likelyOwner;
        this.evidenceObserved = builder.evidenceObserved;
        this.probableRootCause = builder.probableRootCause;
        this.recommendedFix = builder.recommendedFix;
        this.retryRecommended = builder.retryRecommended;
        this.defectWorthy = builder.defectWorthy;
        this.suggestedSeverity = builder.suggestedSeverity;
        this.seenBefore = builder.seenBefore;
        this.failureFrequency = builder.failureFrequency;
        this.recurringPattern = builder.recurringPattern;
    }

    public String getFailureSummary() { return failureSummary; }
    public RootCauseCategory getCategory() { return category; }
    public int getConfidenceScore() { return confidenceScore; }
    public FailureOwner getLikelyOwner() { return likelyOwner; }
    public String getEvidenceObserved() { return evidenceObserved; }
    public String getProbableRootCause() { return probableRootCause; }
    public String getRecommendedFix() { return recommendedFix; }
    public boolean isRetryRecommended() { return retryRecommended; }
    public boolean isDefectWorthy() { return defectWorthy; }
    public String getSuggestedSeverity() { return suggestedSeverity; }
    public boolean isSeenBefore() { return seenBefore; }
    public String getFailureFrequency() { return failureFrequency; }
    public boolean isRecurringPattern() { return recurringPattern; }

    public static Builder builder(RootCauseCategory category, int confidence) {
        return new Builder(category, confidence);
    }

    public static class Builder {
        private final RootCauseCategory category;
        private final int confidenceScore;
        private String failureSummary = "";
        private FailureOwner likelyOwner = FailureOwner.UNKNOWN;
        private String evidenceObserved = "";
        private String probableRootCause = "";
        private String recommendedFix = "";
        private boolean retryRecommended = false;
        private boolean defectWorthy = false;
        private String suggestedSeverity = "Medium";
        private boolean seenBefore = false;
        private String failureFrequency = "N/A";
        private boolean recurringPattern = false;

        private Builder(RootCauseCategory category, int confidence) {
            this.category = category;
            this.confidenceScore = Math.max(0, Math.min(100, confidence));
            this.likelyOwner = resolveOwner(category);
        }

        private static FailureOwner resolveOwner(RootCauseCategory category) {
            return switch (category) {
                case LOCATOR_ISSUE, TIMING_SYNC_ISSUE -> FailureOwner.AUTOMATION;
                case ASSERTION_FAILURE, APPLICATION_DEFECT -> FailureOwner.APPLICATION;
                case ENVIRONMENT_ISSUE, NETWORK_TIMEOUT -> FailureOwner.ENVIRONMENT;
                case TEST_DATA_ISSUE -> FailureOwner.TEST_DATA;
                case DRIVER_BROWSER_ISSUE, API_DEPENDENCY_FAILURE -> FailureOwner.INFRASTRUCTURE;
                case UNKNOWN -> FailureOwner.UNKNOWN;
            };
        }

        public Builder failureSummary(String summary) { this.failureSummary = summary; return this; }
        public Builder likelyOwner(FailureOwner owner) { this.likelyOwner = owner; return this; }
        public Builder evidenceObserved(String evidence) { this.evidenceObserved = evidence; return this; }
        public Builder probableRootCause(String cause) { this.probableRootCause = cause; return this; }
        public Builder recommendedFix(String fix) { this.recommendedFix = fix; return this; }
        public Builder retryRecommended(boolean retry) { this.retryRecommended = retry; return this; }
        public Builder defectWorthy(boolean defect) { this.defectWorthy = defect; return this; }
        public Builder suggestedSeverity(String severity) { this.suggestedSeverity = severity; return this; }
        public Builder seenBefore(boolean seen) { this.seenBefore = seen; return this; }
        public Builder failureFrequency(String freq) { this.failureFrequency = freq; return this; }
        public Builder recurringPattern(boolean recurring) { this.recurringPattern = recurring; return this; }

        public RCAResult build() { return new RCAResult(this); }
    }
}
