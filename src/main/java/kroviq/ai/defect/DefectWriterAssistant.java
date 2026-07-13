package kroviq.ai.defect;

import kroviq.ai.rca.RCAResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DefectWriterAssistant {
    private static final Logger logger = LogManager.getLogger(DefectWriterAssistant.class);

    private final DefectClassificationEngine classificationEngine;
    private final DefectSeverityCalculator severityCalculator;
    private final DefectTitleGenerator titleGenerator;

    public DefectWriterAssistant() {
        this.classificationEngine = new DefectClassificationEngine();
        this.severityCalculator = new DefectSeverityCalculator();
        this.titleGenerator = new DefectTitleGenerator();
    }

    public DefectDraft generate(DefectContext context) {
        logger.debug("Generating defect draft for: {}", context.getTestCaseId());

        RCAResult rca = context.getRcaResult();
        DefectClassification classification = classificationEngine.classify(rca);
        String severity = severityCalculator.calculate(rca, classification);
        String title = titleGenerator.generate(context);

        DefectDraft draft = DefectDraft.builder()
                .defectTitle(title)
                .executiveSummary(buildExecutiveSummary(context, classification))
                .reproductionSteps(buildReproductionSteps(context))
                .expectedResult(buildExpectedResult(context))
                .actualResult(buildActualResult(context))
                .probableRootCause(rca != null ? rca.getProbableRootCause() : "Unable to determine")
                .defectClassification(classification)
                .likelyOwner(resolveOwner(rca, classification))
                .severity(severity)
                .retryRecommended(rca != null && rca.isRetryRecommended())
                .defectRecommended(rca != null && rca.isDefectWorthy())
                .impactedModule(context.getModuleName())
                .testCaseId(context.getTestCaseId())
                .screenshots(context.getScreenshotPaths())
                .logs(buildLogs(context))
                .environmentDetails(buildEnvironmentDetails(context))
                .build();

        logger.info("Defect draft generated: {} [{}] [{}]", title, classification.getDisplayName(), severity);
        return draft;
    }

    private String buildExecutiveSummary(DefectContext context, DefectClassification classification) {
        RCAResult rca = context.getRcaResult();
        StringBuilder summary = new StringBuilder();

        summary.append("Test case ").append(context.getTestCaseId());
        summary.append(" failed during execution of module '").append(context.getModuleName()).append("'.");
        summary.append(" Classification: ").append(classification.getDisplayName()).append(".");

        if (rca != null) {
            summary.append(" Root cause identified as ").append(rca.getCategory().getDisplayName().toLowerCase());
            summary.append(" with ").append(rca.getConfidenceScore()).append("% confidence.");
        }

        return summary.toString();
    }

    private String buildReproductionSteps(DefectContext context) {
        StringBuilder steps = new StringBuilder();
        List<String> previousSteps = context.getPreviousSteps();

        int stepNum = 1;
        for (String step : previousSteps) {
            steps.append(stepNum).append(". ").append(step).append("\n");
            stepNum++;
        }

        // Add the failed step
        steps.append(stepNum).append(". [FAILED] ").append(context.getFailedStep()).append("\n");

        return steps.toString();
    }

    private String buildExpectedResult(DefectContext context) {
        String step = context.getFailedStep();
        if (step == null || step.isEmpty()) return "Step should complete successfully";

        // Derive expected from step description
        return "Step '" + truncate(step, 80) + "' should complete successfully without errors";
    }

    private String buildActualResult(DefectContext context) {
        RCAResult rca = context.getRcaResult();
        StringBuilder actual = new StringBuilder();

        actual.append("Step failed with: ");
        if (rca != null && rca.getEvidenceObserved() != null && !rca.getEvidenceObserved().isEmpty()) {
            actual.append(rca.getEvidenceObserved());
        } else if (context.getExceptionMessage() != null && !context.getExceptionMessage().isEmpty()) {
            actual.append(context.getExceptionMessage());
        } else {
            actual.append("Unknown error");
        }

        return actual.toString();
    }

    private String resolveOwner(RCAResult rca, DefectClassification classification) {
        if (rca != null) return rca.getLikelyOwner().getDisplayName();
        return switch (classification) {
            case FUNCTIONAL_DEFECT -> "Application Team";
            case AUTOMATION_DEFECT -> "Automation Team";
            case ENVIRONMENT_ISSUE -> "DevOps/Environment Team";
            case TEST_DATA_ISSUE -> "Test Data Team";
            case INFRASTRUCTURE_ISSUE -> "Infrastructure Team";
            case PRODUCT_ENHANCEMENT -> "Product Team";
            case UNKNOWN_REVIEW_REQUIRED -> "Unknown";
        };
    }

    private String buildLogs(DefectContext context) {
        StringBuilder logs = new StringBuilder();
        logs.append("Failed Step: ").append(context.getFailedStep()).append("\n");
        if (context.getCurrentUrl() != null && !context.getCurrentUrl().isEmpty()) {
            logs.append("URL: ").append(context.getCurrentUrl()).append("\n");
        }
        if (context.getExceptionMessage() != null && !context.getExceptionMessage().isEmpty()) {
            logs.append("Exception: ").append(context.getExceptionMessage()).append("\n");
        }
        return logs.toString();
    }

    private String buildEnvironmentDetails(DefectContext context) {
        StringBuilder env = new StringBuilder();
        if (context.getEnvironmentName() != null && !context.getEnvironmentName().isEmpty()) {
            env.append("URL: ").append(context.getEnvironmentName());
        }
        if (context.getBrowser() != null && !context.getBrowser().isEmpty()) {
            if (env.length() > 0) env.append(" | ");
            env.append("Browser: ").append(context.getBrowser());
        }
        return env.toString();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
