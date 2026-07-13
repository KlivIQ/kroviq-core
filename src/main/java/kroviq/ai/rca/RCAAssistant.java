package kroviq.ai.rca;

import kroviq.ai.rca.rules.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RCAAssistant {
    private static final Logger logger = LogManager.getLogger(RCAAssistant.class);
    private static final int HIGH_CONFIDENCE_THRESHOLD = 90;

    private final List<RCARule> ruleChain;
    private final RCARecommendationEngine recommender;

    public RCAAssistant() {
        this.ruleChain = new ArrayList<>();
        this.recommender = new RCARecommendationEngine();
        registerRules();
    }

    private void registerRules() {
        // Priority order: highest priority first
        ruleChain.add(new DriverRules());
        ruleChain.add(new EnvironmentRules());
        ruleChain.add(new LocatorRules());
        ruleChain.add(new TimingRules());
        ruleChain.add(new AssertionRules());
    }

    public RCAResult analyze(RCAContext context) {
        logger.debug("RCA analysis started for step: {}", context.getFailedStep());

        RuleVerdict bestVerdict = null;

        for (RCARule rule : ruleChain) {
            try {
                Optional<RuleVerdict> verdict = rule.evaluate(context);
                if (verdict.isPresent()) {
                    RuleVerdict v = verdict.get();
                    if (bestVerdict == null || v.getConfidence() > bestVerdict.getConfidence()) {
                        bestVerdict = v;
                    }
                    if (bestVerdict.getConfidence() >= HIGH_CONFIDENCE_THRESHOLD) break;
                }
            } catch (Exception e) {
                logger.warn("Rule evaluation failed for {}: {}", rule.getClass().getSimpleName(), e.getMessage());
            }
        }

        if (bestVerdict == null) {
            bestVerdict = RuleVerdict.of(RootCauseCategory.UNKNOWN, 10,
                    "No matching rule pattern for: " + context.getExceptionName());
        }

        // Record to recurrence tracker
        RCARecurrenceTracker tracker = RCARecurrenceTracker.getInstance();
        tracker.recordFailure(context.getTestCaseId(), bestVerdict.getCategory());

        RCAResult result = recommender.buildResult(context, bestVerdict);
        logger.info("RCA complete: {} ({}%) — Owner: {}",
                result.getCategory().getDisplayName(),
                result.getConfidenceScore(),
                result.getLikelyOwner().getDisplayName());

        return result;
    }
}
