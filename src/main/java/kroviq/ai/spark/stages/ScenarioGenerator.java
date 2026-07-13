package kroviq.ai.spark.stages;

/**
 * Stage 2: Converts Flow JSON into structured test scenarios.
 * Input: Flow JSON string (from FlowAnalyzer)
 * Output: Scenarios JSON string (consumed by GherkinWriter)
 */
public class ScenarioGenerator {

    private static final String SKILL_VERSION = "2.2";

    public String generate(String flowJson, String moduleName) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("=== SCENARIO GENERATOR INPUT ===\n\n");
        prompt.append("Module: ").append(moduleName).append("\n\n");
        prompt.append("Flow JSON:\n").append(flowJson).append("\n\n");
        prompt.append("=== END INPUT ===\n\n");
        prompt.append("Process this using Scenario Generator Skill v").append(SKILL_VERSION).append(".\n");
        prompt.append("Output: Scenarios JSON only. No markdown fences. No explanation.\n");

        return prompt.toString();
    }

    public String getSkillVersion() { return SKILL_VERSION; }
}
