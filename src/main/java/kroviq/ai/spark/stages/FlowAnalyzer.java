package kroviq.ai.spark.stages;

import kroviq.ai.spark.model.SparkInput;

/**
 * Stage 1: Analyzes BRD content and extracts structured flow information.
 * Input: SparkInput (BRD content + module name + context)
 * Output: Structured flow JSON string (consumed by ScenarioGenerator)
 */
public class FlowAnalyzer {

    private static final String SKILL_VERSION = "3.1";

    public String analyze(SparkInput input) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("=== FLOW ANALYZER INPUT ===\n\n");
        prompt.append("Module: ").append(input.getModuleName()).append("\n");
        prompt.append("Analysis Mode: brd_only\n\n");

        if (input.getBusinessContext() != null && !input.getBusinessContext().isEmpty()) {
            prompt.append("Business Context:\n").append(input.getBusinessContext()).append("\n\n");
        }

        prompt.append("BRD Content:\n").append(input.getBrdContent()).append("\n\n");
        prompt.append("=== END INPUT ===\n\n");
        prompt.append("Process this using Flow Analyser Skill v").append(SKILL_VERSION).append(".\n");
        prompt.append("Output: Flow JSON only. No markdown fences. No explanation.\n");

        return prompt.toString();
    }

    public String getSkillVersion() { return SKILL_VERSION; }
}
