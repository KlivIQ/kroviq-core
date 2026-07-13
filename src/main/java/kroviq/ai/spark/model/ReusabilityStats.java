package kroviq.ai.spark.model;

import java.util.Collections;
import java.util.Map;

public class ReusabilityStats {
    private final int matchedSteps;
    private final int unmatchedSteps;
    private final Map<String, Integer> stepUsageCount;

    public ReusabilityStats(int matchedSteps, int unmatchedSteps, Map<String, Integer> stepUsageCount) {
        this.matchedSteps = matchedSteps;
        this.unmatchedSteps = unmatchedSteps;
        this.stepUsageCount = stepUsageCount != null ? Collections.unmodifiableMap(stepUsageCount) : Collections.emptyMap();
    }

    public int getMatchedSteps() { return matchedSteps; }
    public int getUnmatchedSteps() { return unmatchedSteps; }
    public Map<String, Integer> getStepUsageCount() { return stepUsageCount; }
    public int totalSteps() { return matchedSteps + unmatchedSteps; }

    public int reusabilityPercent() {
        return totalSteps() == 0 ? 0 : (matchedSteps * 100) / totalSteps();
    }

    public ReadinessLevel getReadinessLevel() {
        int pct = reusabilityPercent();
        if (pct >= 80) return ReadinessLevel.READY;
        if (pct >= 50) return ReadinessLevel.REVIEW_REQUIRED;
        return ReadinessLevel.LOW_READINESS;
    }

    public enum ReadinessLevel {
        READY("✅ READY"),
        REVIEW_REQUIRED("⚠️ REVIEW REQUIRED"),
        LOW_READINESS("❌ LOW AUTOMATION READINESS");

        private final String display;
        ReadinessLevel(String display) { this.display = display; }
        public String getDisplay() { return display; }
    }
}
