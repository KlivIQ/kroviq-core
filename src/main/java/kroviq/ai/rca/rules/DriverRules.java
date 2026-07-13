package kroviq.ai.rca.rules;

import kroviq.ai.rca.RCAContext;
import kroviq.ai.rca.RootCauseCategory;
import java.util.Optional;

public class DriverRules implements RCARule {

    @Override
    public Optional<RuleVerdict> evaluate(RCAContext context) {
        String exName = context.getExceptionName();
        String exMsg = context.getExceptionMessage().toLowerCase();

        // Session not created / session deleted
        if (exName.contains("SessionNotCreated") || exName.contains("NoSuchSession")) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.DRIVER_BROWSER_ISSUE, 95,
                    "Browser session could not be created or was terminated: " + context.getExceptionName()));
        }

        // WebDriverException with session indicators
        if (exName.equals("WebDriverException")) {
            if (exMsg.contains("session") || exMsg.contains("disconnected") || exMsg.contains("not reachable")) {
                return Optional.of(RuleVerdict.of(RootCauseCategory.DRIVER_BROWSER_ISSUE, 90,
                        "WebDriver session lost: " + truncate(context.getExceptionMessage(), 120)));
            }
            if (exMsg.contains("chrome not reachable") || exMsg.contains("unable to connect")) {
                return Optional.of(RuleVerdict.of(RootCauseCategory.DRIVER_BROWSER_ISSUE, 92,
                        "Browser process unreachable — possible crash"));
            }
        }

        // Browser crash patterns in message
        if (exMsg.contains("crashed") || exMsg.contains("target frame detached") || exMsg.contains("page crash")) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.DRIVER_BROWSER_ISSUE, 88,
                    "Browser crash detected: " + truncate(context.getExceptionMessage(), 100)));
        }

        // UnreachableBrowserException
        if (exName.contains("UnreachableBrowser")) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.DRIVER_BROWSER_ISSUE, 93,
                    "Browser process is unreachable — likely terminated"));
        }

        return Optional.empty();
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
