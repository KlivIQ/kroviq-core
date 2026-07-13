package kroviq.ai.spark.catalog;

import kroviq.ai.spark.model.ReusabilityStats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StepReusabilityAnalyzer {

    private static final Pattern STEP_LINE_PATTERN = Pattern.compile(
            "^\\s*(When|Then|And|But)\\s+(.+)$"
    );

    private static final Pattern PARAM_PATTERN = Pattern.compile("\"[^\"]*\"");

    private final StepCatalog catalog;

    public StepReusabilityAnalyzer(StepCatalog catalog) {
        this.catalog = catalog;
    }

    public AnalysisResult analyze(String featureContent, List<String> declaredCustomSteps) {
        List<String> stepLines = extractStepLines(featureContent);
        int matched = 0;
        int unmatched = 0;
        Map<String, Integer> usageCount = new LinkedHashMap<>();
        List<String> unmatchedSteps = new ArrayList<>();

        for (String stepLine : stepLines) {
            MatchResult result = matchStep(stepLine);
            if (result.matched) {
                matched++;
                usageCount.merge(result.matchedPattern, 1, Integer::sum);
            } else {
                unmatched++;
                unmatchedSteps.add(stepLine);
            }
        }

        // Also count declared custom steps from the Gherkin Writer output
        int declaredCustomCount = declaredCustomSteps != null ? declaredCustomSteps.size() : 0;
        if (declaredCustomCount > unmatched) {
            unmatched = declaredCustomCount;
        }

        ReusabilityStats stats = new ReusabilityStats(matched, unmatched, usageCount);
        return new AnalysisResult(stats, unmatchedSteps);
    }

    private List<String> extractStepLines(String featureContent) {
        List<String> steps = new ArrayList<>();
        if (featureContent == null || featureContent.isEmpty()) return steps;

        for (String line : featureContent.split("\n")) {
            String trimmed = line.trim();
            Matcher m = STEP_LINE_PATTERN.matcher(trimmed);
            if (m.matches()) {
                // Skip comment lines that happen to start with When/Then
                if (!trimmed.startsWith("#")) {
                    steps.add(trimmed);
                }
            }
        }
        return steps;
    }

    private MatchResult matchStep(String stepLine) {
        Matcher m = STEP_LINE_PATTERN.matcher(stepLine);
        if (!m.matches()) return new MatchResult(false, "", 0);

        String keyword = m.group(1);
        String stepBody = m.group(2);

        // Normalize "And"/"But" to check against both When and Then
        String effectiveKeyword = keyword.equals("And") || keyword.equals("But") ? null : keyword;

        // Check for NEW STEP NEEDED marker
        if (stepLine.contains("# NEW STEP NEEDED")) {
            return new MatchResult(false, stepBody, 0);
        }

        // Convert step body to a pattern by replacing quoted strings with {string}
        String normalized = PARAM_PATTERN.matcher(stepBody).replaceAll("{string}");
        // Also normalize numeric params
        normalized = normalized.replaceAll("\\b\\d+\\b", "{string}");

        // Try exact pattern match against catalog
        for (StepEntry entry : catalog.getAll()) {
            if (effectiveKeyword != null && !entry.getKeyword().equalsIgnoreCase(effectiveKeyword)) {
                // For And/But, skip keyword check
                if (!keyword.equals("And") && !keyword.equals("But")) continue;
            }

            if (patternsMatch(normalized, entry.getPattern())) {
                return new MatchResult(true, entry.getPattern(), 100);
            }
        }

        // Try relaxed matching: category-based
        StepCategory inferredCategory = inferCategory(stepBody);
        if (inferredCategory != StepCategory.UNKNOWN) {
            List<StepEntry> candidates = catalog.getByCategory(inferredCategory);
            if (!candidates.isEmpty()) {
                // Check param count similarity
                int paramCount = countParams(stepBody);
                for (StepEntry candidate : candidates) {
                    if (candidate.getParams().size() == paramCount) {
                        return new MatchResult(true, candidate.getPattern(), 85);
                    }
                }
                // Category match but param mismatch — still consider matched at lower confidence
                // Only if the step uses standard Kroviq patterns (has "on ... page", "in modal", etc.)
                if (stepBody.contains("on \"") && stepBody.contains("\" page")) {
                    return new MatchResult(true, candidates.get(0).getPattern(), 75);
                }
            }
        }

        return new MatchResult(false, stepBody, 0);
    }

    private boolean patternsMatch(String normalizedStep, String catalogPattern) {
        // Normalize catalog pattern: "I open {string} from main menu" → compare with step
        String stepLower = normalizedStep.toLowerCase().trim();
        String patternLower = catalogPattern.toLowerCase().trim();

        // Direct match
        if (stepLower.equals(patternLower)) return true;

        // Match with "I " prefix handling (step has "I ", pattern has "I ")
        if (stepLower.startsWith("i ") && patternLower.startsWith("i ")) {
            return stepLower.equals(patternLower);
        }

        // Flexible match: replace {string} with regex and test
        String regex = Pattern.quote(patternLower)
                .replace("\\{string\\}", "\\E.+\\Q")
                .replace("\\{int\\}", "\\E\\d+\\Q");
        // Clean up empty quote sections
        regex = regex.replace("\\Q\\E", "");
        try {
            return stepLower.matches(regex);
        } catch (Exception e) {
            return false;
        }
    }

    private StepCategory inferCategory(String stepBody) {
        String lower = stepBody.toLowerCase();

        if (lower.startsWith("i open ") && lower.contains("from main menu")) return StepCategory.NAVIGATE;
        if (lower.startsWith("i click on ")) return StepCategory.CLICK;
        if (lower.startsWith("i close modal")) return StepCategory.CLICK;
        if (lower.startsWith("i enter ")) return StepCategory.INPUT;
        if (lower.startsWith("i select ") && lower.contains(" from ")) return StepCategory.SELECT;
        if (lower.startsWith("i select radio")) return StepCategory.RADIO;
        if (lower.startsWith("i set ") && lower.contains("date to")) return StepCategory.DATE;
        if (lower.startsWith("i set ") && lower.contains("datetime to")) return StepCategory.DATE;
        if (lower.startsWith("i set ") && lower.contains("checkbox to")) return StepCategory.CHECKBOX;
        if (lower.startsWith("i set ") && lower.contains("to ")) return StepCategory.TOGGLE;
        if (lower.startsWith("i verify element ")) return StepCategory.VALIDATE_ELEMENT;
        if (lower.startsWith("i verify ") && lower.contains("has value")) return StepCategory.VALIDATE_ELEMENT;
        if (lower.startsWith("i should see the ")) return StepCategory.VALIDATE_ELEMENT;
        if (lower.startsWith("i validate message ")) return StepCategory.VALIDATE_MESSAGE;
        if (lower.startsWith("i validate ") && lower.contains("outcome")) return StepCategory.VALIDATE_MESSAGE;
        if (lower.startsWith("i verify toast")) return StepCategory.VALIDATE_MESSAGE;
        if (lower.startsWith("i verify record ")) return StepCategory.VALIDATE_TABLE;
        if (lower.startsWith("i verify first row")) return StepCategory.VALIDATE_TABLE;
        if (lower.startsWith("i perform ") && lower.contains("action on record")) return StepCategory.TABLE_ACTION;
        if (lower.startsWith("i remember value")) return StepCategory.VALUE_CAPTURE;
        if (lower.contains("remembered value")) return StepCategory.VALUE_CAPTURE;
        if (lower.startsWith("i wait for")) return StepCategory.WAIT;
        if (lower.startsWith("i log ")) return StepCategory.LOG;
        if (lower.startsWith("i report step")) return StepCategory.LOG;

        return StepCategory.UNKNOWN;
    }

    private int countParams(String stepBody) {
        Matcher m = PARAM_PATTERN.matcher(stepBody);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    public static class MatchResult {
        final boolean matched;
        final String matchedPattern;
        final int confidence;

        MatchResult(boolean matched, String matchedPattern, int confidence) {
            this.matched = matched;
            this.matchedPattern = matchedPattern;
            this.confidence = confidence;
        }
    }

    public static class AnalysisResult {
        private final ReusabilityStats stats;
        private final List<String> unmatchedSteps;

        public AnalysisResult(ReusabilityStats stats, List<String> unmatchedSteps) {
            this.stats = stats;
            this.unmatchedSteps = unmatchedSteps;
        }

        public ReusabilityStats getStats() { return stats; }
        public List<String> getUnmatchedSteps() { return unmatchedSteps; }
    }
}
