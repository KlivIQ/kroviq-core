package kroviq.ai.rca.rules;

import kroviq.ai.rca.RCAContext;
import kroviq.ai.rca.RootCauseCategory;
import java.util.Optional;

public class EnvironmentRules implements RCARule {

    @Override
    public Optional<RuleVerdict> evaluate(RCAContext context) {
        String exName = context.getExceptionName();
        String exMsg = context.getExceptionMessage().toLowerCase();
        String url = context.getCurrentUrl().toLowerCase();

        // Connection refused / unreachable
        if (exName.contains("ConnectException") || exMsg.contains("connection refused")
                || exMsg.contains("err_connection_refused")) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.ENVIRONMENT_ISSUE, 92,
                    "Target server unreachable — connection refused"));
        }

        // SSL / certificate errors
        if (exMsg.contains("ssl") || exMsg.contains("certificate") || exMsg.contains("err_cert")) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.ENVIRONMENT_ISSUE, 85,
                    "SSL/Certificate error — environment trust issue"));
        }

        // DNS resolution failure
        if (exMsg.contains("err_name_not_resolved") || exMsg.contains("unknownhost")
                || exName.contains("UnknownHost")) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.ENVIRONMENT_ISSUE, 90,
                    "DNS resolution failed — host not found"));
        }

        // Network timeout (distinct from element timeout)
        if (exMsg.contains("err_timed_out") || exMsg.contains("net::err_")
                || (exMsg.contains("network timeout") || exMsg.contains("network_timeout"))) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.NETWORK_TIMEOUT, 85,
                    "Network-level timeout — server did not respond"));
        }

        // HTTP error codes in message
        if (exMsg.contains("502") || exMsg.contains("503") || exMsg.contains("504")
                || exMsg.contains("bad gateway") || exMsg.contains("service unavailable")) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.ENVIRONMENT_ISSUE, 80,
                    "Server returned error status — environment may be down"));
        }

        // Timeout on login/auth pages
        if (exName.contains("TimeoutException") && (url.contains("login") || url.contains("auth")
                || url.contains("sso") || url.contains("oauth"))) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.ENVIRONMENT_ISSUE, 75,
                    "Timeout on authentication page — SSO/auth service may be unresponsive"));
        }

        // Page load timeout (renderer timeout) — likely network/server issue
        if (exName.contains("TimeoutException") && exMsg.contains("timed out receiving message from renderer")) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.NETWORK_TIMEOUT, 85,
                    "Page load timeout — server did not respond within page load timeout"));
        }

        // API dependency failure patterns
        if (exMsg.contains("api") && (exMsg.contains("timeout") || exMsg.contains("unavailable")
                || exMsg.contains("500"))) {
            return Optional.of(RuleVerdict.of(RootCauseCategory.API_DEPENDENCY_FAILURE, 78,
                    "API dependency failure detected in error message"));
        }

        return Optional.empty();
    }
}
