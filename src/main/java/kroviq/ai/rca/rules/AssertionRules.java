package kroviq.ai.rca.rules;

import kroviq.ai.rca.RCAContext;
import kroviq.ai.rca.RootCauseCategory;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssertionRules implements RCARule {

    private static final Pattern EXPECTED_ACTUAL = Pattern.compile(
            "expected\\s*[:\\[<](.{1,80})[>\\]].*?(?:but was|actual|got)\\s*[:\\[<](.{1,80})[>\\]]",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override
    public Optional<RuleVerdict> evaluate(RCAContext context) {
        String exName = context.getExceptionName();
        String exMsg = context.getExceptionMessage();

        // AssertionError / AssertionFailedError
        if (exName.contains("Assertion") || exName.contains("ComparisonFailure")) {
            String evidence = extractExpectedActual(exMsg);

            if (evidence.isEmpty()) {
                // Fallback: use the message itself as evidence
                evidence = "Assertion failed: " + truncate(exMsg, 120);
            }

            // If expected/actual differ significantly, likely app defect
            if (looksLikeDataMismatch(exMsg)) {
                return Optional.of(RuleVerdict.of(RootCauseCategory.ASSERTION_FAILURE, 80,
                        evidence));
            }

            // Generic assertion failure
            return Optional.of(RuleVerdict.of(RootCauseCategory.ASSERTION_FAILURE, 75,
                    evidence));
        }

        // Custom framework assertion patterns
        if (exMsg != null && exMsg.toLowerCase().contains("expected") && exMsg.toLowerCase().contains("actual")) {
            String evidence = extractExpectedActual(exMsg);
            if (!evidence.isEmpty()) {
                return Optional.of(RuleVerdict.of(RootCauseCategory.ASSERTION_FAILURE, 75,
                        evidence));
            }
        }

        return Optional.empty();
    }

    private String extractExpectedActual(String message) {
        if (message == null || message.isEmpty()) return "";

        Matcher matcher = EXPECTED_ACTUAL.matcher(message);
        if (matcher.find()) {
            return "Expected: [" + matcher.group(1).trim() + "] but got: [" + matcher.group(2).trim() + "]";
        }

        // Fallback: look for simpler patterns
        if (message.contains("expected:") || message.contains("Expected:")) {
            return truncate(message, 150);
        }

        return "";
    }

    private boolean looksLikeDataMismatch(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        // Patterns suggesting the assertion itself is correct but data differs
        return lower.contains("expected:") && lower.contains("but was:")
                || lower.contains("expected [") && lower.contains("] but was [");
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
