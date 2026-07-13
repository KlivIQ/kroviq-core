package kroviq.ai.defect;

import kroviq.ai.rca.RCAResult;

public class DefectTitleGenerator {

    public String generate(DefectContext context) {
        String module = resolveModule(context);
        String action = resolveAction(context);
        String rootCause = resolveRootCause(context.getRcaResult());

        return module + " - " + action + " failed due to " + rootCause;
    }

    private String resolveModule(DefectContext context) {
        String module = context.getModuleName();
        if (module == null || module.isEmpty()) {
            module = extractModuleFromTestCaseId(context.getTestCaseId());
        }
        return module.isEmpty() ? "Unknown Module" : module;
    }

    private String resolveAction(DefectContext context) {
        String step = context.getFailedStep();
        if (step == null || step.isEmpty()) return "Action";

        // Extract meaningful action from step description
        String action = step.replaceAll("^(Given|When|Then|And|But)\\s+", "");
        // Truncate long actions
        if (action.length() > 50) action = action.substring(0, 50).trim();
        // Capitalize first letter
        if (!action.isEmpty()) {
            action = Character.toUpperCase(action.charAt(0)) + action.substring(1);
        }
        return action;
    }

    private String resolveRootCause(RCAResult rcaResult) {
        if (rcaResult == null) return "unknown failure";
        String rootCause = rcaResult.getCategory().getDisplayName().toLowerCase();
        return rootCause;
    }

    private String extractModuleFromTestCaseId(String testCaseId) {
        if (testCaseId == null || testCaseId.isEmpty()) return "";
        // Pattern: TC_MODULE_001 → Module
        String id = testCaseId.replaceFirst("^@?TC_", "");
        int lastUnderscore = id.lastIndexOf('_');
        if (lastUnderscore > 0) {
            String prefix = id.substring(0, lastUnderscore);
            return toPascalCase(prefix);
        }
        return id;
    }

    private String toPascalCase(String input) {
        String[] parts = input.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
