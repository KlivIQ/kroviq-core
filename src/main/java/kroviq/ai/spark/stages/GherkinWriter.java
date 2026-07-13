package kroviq.ai.spark.stages;

import kroviq.ai.spark.catalog.StepCatalog;
import kroviq.ai.spark.catalog.StepEntry;

import java.util.stream.Collectors;

/**
 * Stage 3: Converts Scenarios JSON into Kroviq-compliant feature file, test data, and constants.
 * Input: Scenarios JSON string (from ScenarioGenerator) + StepCatalog
 * Output: Gherkin Writer prompt with embedded step vocabulary
 */
public class GherkinWriter {

    private static final String SKILL_VERSION = "2.2";

    public String write(String scenariosJson, String moduleName, String pageName, StepCatalog catalog) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("=== GHERKIN WRITER INPUT ===\n\n");
        prompt.append("Module: ").append(moduleName).append("\n");
        prompt.append("Page Name: ").append(pageName).append("\n\n");

        // Embed step catalog as permitted vocabulary
        if (catalog.size() > 0) {
            prompt.append("=== PERMITTED STEP VOCABULARY (").append(catalog.size()).append(" steps) ===\n");
            prompt.append("CRITICAL: Only use steps from this list. Exact syntax must be followed.\n");
            prompt.append("Any step not covered: write closest available, add # NEW STEP NEEDED comment.\n\n");

            for (StepEntry entry : catalog.getAll()) {
                prompt.append(entry.getKeyword()).append(" ").append(entry.getPattern()).append("\n");
                if (entry.getExample() != null && !entry.getExample().isEmpty()) {
                    prompt.append("  Example: ").append(entry.getExample()).append("\n");
                }
            }
            prompt.append("\n=== END VOCABULARY ===\n\n");
        }

        prompt.append("Scenarios JSON:\n").append(scenariosJson).append("\n\n");
        prompt.append("=== END INPUT ===\n\n");
        prompt.append("Process this using Gherkin Writer Skill v").append(SKILL_VERSION).append(".\n");
        prompt.append("Output: JSON with keys: feature_file, test_data_json, constants_class, new_step_definitions_needed.\n");
        prompt.append("No markdown fences. No explanation.\n");

        return prompt.toString();
    }

    public String getSkillVersion() { return SKILL_VERSION; }
}
