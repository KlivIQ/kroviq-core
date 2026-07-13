package kroviq.ai.rca.rules;

import kroviq.ai.rca.RCAContext;
import java.util.Optional;

@FunctionalInterface
public interface RCARule {
    Optional<RuleVerdict> evaluate(RCAContext context);
}
