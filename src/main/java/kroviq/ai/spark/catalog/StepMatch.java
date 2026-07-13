package kroviq.ai.spark.catalog;

public class StepMatch {
    private final StepEntry entry;
    private final int confidence;
    private final String renderedStep;

    public StepMatch(StepEntry entry, int confidence, String renderedStep) {
        this.entry = entry;
        this.confidence = confidence;
        this.renderedStep = renderedStep;
    }

    public StepEntry getEntry() { return entry; }
    public int getConfidence() { return confidence; }
    public String getRenderedStep() { return renderedStep; }

    public boolean isHighConfidence() { return confidence >= 85; }
    public boolean isAcceptable() { return confidence >= 60; }
}
