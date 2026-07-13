package kroviq.ai.rca.rules;

import kroviq.ai.rca.RootCauseCategory;

public class RuleVerdict {
    private final RootCauseCategory category;
    private final int confidence;
    private final String evidence;

    private RuleVerdict(RootCauseCategory category, int confidence, String evidence) {
        this.category = category;
        this.confidence = Math.max(0, Math.min(100, confidence));
        this.evidence = evidence;
    }

    public static RuleVerdict of(RootCauseCategory category, int confidence, String evidence) {
        return new RuleVerdict(category, confidence, evidence);
    }

    public RootCauseCategory getCategory() { return category; }
    public int getConfidence() { return confidence; }
    public String getEvidence() { return evidence; }
}
