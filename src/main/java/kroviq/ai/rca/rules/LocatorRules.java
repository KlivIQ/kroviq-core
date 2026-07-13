package kroviq.ai.rca.rules;

import kroviq.ai.rca.RCAContext;
import kroviq.ai.rca.RootCauseCategory;
import java.util.Optional;
import java.util.regex.Pattern;

public class LocatorRules implements RCARule {

    private static final Pattern POSITIONAL_XPATH = Pattern.compile("\\[\\d+\\]");
    private static final Pattern MULTI_CLASS_XPATH = Pattern.compile("contains\\(@class");
    private static final int BRITTLE_LENGTH_THRESHOLD = 100;

    @Override
    public Optional<RuleVerdict> evaluate(RCAContext context) {
        String exName = context.getExceptionName();
        String exMsg = context.getExceptionMessage().toLowerCase();
        String locator = context.getLocatorUsed();

        // InvalidSelectorException — always locator issue
        if (exName.contains("InvalidSelector") || exName.contains("InvalidSelectorException")) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.LOCATOR_ISSUE, 95,
                    "Invalid selector syntax: " + truncate(locator, 80)));
        }

        // NoSuchElementException
        if (exName.contains("NoSuchElement") || exName.contains("NoSuchElementException")) {
            int brittleScore = calculateBrittleness(locator);

            if (brittleScore >= 3) {
                return Optional.of(RuleVerdict.of(RootCauseCategory.LOCATOR_ISSUE, 88,
                        "Element not found with brittle locator (score=" + brittleScore + "): " + truncate(locator, 80)));
            }

            if (brittleScore >= 1) {
                return Optional.of(RuleVerdict.of(RootCauseCategory.LOCATOR_ISSUE, 75,
                        "Element not found — locator may be fragile: " + truncate(locator, 80)));
            }

            // Stable locator but element absent — could be app defect
            return Optional.of(RuleVerdict.of(RootCauseCategory.APPLICATION_DEFECT, 65,
                    "Element absent from loaded page (stable locator): " + truncate(locator, 80)));
        }

        // NoSuchFrameException
        if (exName.contains("NoSuchFrame")) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.LOCATOR_ISSUE, 80,
                    "Target frame/iframe not found — frame locator may be incorrect"));
        }

        // NoSuchWindowException
        if (exName.contains("NoSuchWindow")) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.LOCATOR_ISSUE, 70,
                    "Target window/tab not found — window handle may be stale"));
        }

        return Optional.empty();
    }

    private int calculateBrittleness(String locator) {
        if (locator == null || locator.isEmpty()) return 0;

        int score = 0;

        // Positional indices like [3], [2]
        if (POSITIONAL_XPATH.matcher(locator).find()) score += 2;

        // Over-specific (too long)
        if (locator.length() > BRITTLE_LENGTH_THRESHOLD) score += 1;

        // Multiple class contains
        if (MULTI_CLASS_XPATH.matcher(locator).find()) score += 1;

        // Deep nesting (many slashes)
        long slashCount = locator.chars().filter(c -> c == '/').count();
        if (slashCount > 8) score += 1;

        // Lacks semantic attributes
        if (!hasSemanticAttribute(locator)) score += 1;

        return score;
    }

    private boolean hasSemanticAttribute(String locator) {
        if (locator == null) return false;
        String lower = locator.toLowerCase();
        return lower.contains("@id") || lower.contains("@name") || lower.contains("@data-testid")
                || lower.contains("@aria-label") || lower.contains("@data-cy")
                || lower.contains("@data-test") || lower.contains("[id=")
                || lower.contains("[name=");
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
