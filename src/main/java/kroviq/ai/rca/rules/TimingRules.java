package kroviq.ai.rca.rules;

import kroviq.ai.rca.RCAContext;
import kroviq.ai.rca.RootCauseCategory;
import java.util.Optional;

public class TimingRules implements RCARule {

    @Override
    public Optional<RuleVerdict> evaluate(RCAContext context) {
        String exName = context.getExceptionName();
        String exMsg = context.getExceptionMessage().toLowerCase();
        String pageSource = context.getPageSource().toLowerCase();

        // StaleElementReferenceException — always timing
        if (exName.contains("StaleElement") || exName.contains("StaleElementReferenceException")) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.TIMING_SYNC_ISSUE, 90,
                    "DOM refreshed during interaction — element reference became stale"));
        }

        // ElementClickInterceptedException — overlay/modal blocking
        if (exName.contains("ElementClickIntercepted") || exName.contains("ElementClickInterceptedException")) {
            String blocker = extractBlockingElement(context.getExceptionMessage());
            return Optional.of(RuleVerdict.of(RootCauseCategory.TIMING_SYNC_ISSUE, 82,
                    "Click intercepted by overlay/modal" + (blocker.isEmpty() ? "" : ": " + blocker)));
        }

        // ElementNotInteractableException — element present but not ready
        if (exName.contains("ElementNotInteractable") || exName.contains("ElementNotInteractableException")) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.TIMING_SYNC_ISSUE, 78,
                    "Element found but not interactable — may still be loading or hidden"));
        }

        // TimeoutException — check for loading indicators
        if (exName.contains("TimeoutException") || exName.contains("Timeout")) {
            if (hasLoadingIndicator(pageSource)) {
                return Optional.of(RuleVerdict.of(RootCauseCategory.TIMING_SYNC_ISSUE, 85,
                        "Timeout with loading indicator detected — page still loading"));
            }

            if (exMsg.contains("waiting for") || exMsg.contains("expected condition")) {
                return Optional.of(RuleVerdict.of(RootCauseCategory.TIMING_SYNC_ISSUE, 75,
                        "Explicit wait timed out — element did not meet expected condition"));
            }

            // Generic timeout without loading evidence
            return Optional.of(RuleVerdict.of(RootCauseCategory.TIMING_SYNC_ISSUE, 70,
                    "Timeout waiting for element — possible sync issue"));
        }

        // MoveTargetOutOfBoundsException
        if (exName.contains("MoveTargetOutOfBounds")) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.TIMING_SYNC_ISSUE, 65,
                    "Element position not stable — may be animating or not fully rendered"));
        }

        return Optional.empty();
    }

    private boolean hasLoadingIndicator(String pageSource) {
        if (pageSource.isEmpty()) return false;
        return pageSource.contains("ant-spin") || pageSource.contains("loading")
                || pageSource.contains("spinner") || pageSource.contains("skeleton")
                || pageSource.contains("mat-progress") || pageSource.contains("p-progressbar")
                || pageSource.contains("mui-circularProgress");
    }

    private String extractBlockingElement(String message) {
        if (message == null) return "";
        // Pattern: "Other element would receive the click: <div class="...">"
        int idx = message.indexOf("Other element");
        if (idx >= 0) {
            String sub = message.substring(idx);
            int end = Math.min(sub.length(), 100);
            return sub.substring(0, end).trim();
        }
        return "";
    }
}
