package kroviq.ai.spark.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kroviq.ai.spark.catalog.StepCatalog;
import kroviq.ai.spark.catalog.StepReusabilityAnalyzer;
import kroviq.ai.spark.model.GherkinResult;
import kroviq.ai.spark.model.ReusabilityStats;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GherkinResultParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static GherkinResult parse(String gherkinWriterOutput, String moduleName, StepCatalog catalog) {
        try {
            JsonNode root = mapper.readTree(gherkinWriterOutput);

            String featureContent = root.has("feature_file") ? root.get("feature_file").asText() : "";
            String constantsContent = root.has("constants_class") ? root.get("constants_class").asText() : "";

            // Parse test data
            Map<String, Map<String, String>> testData = new LinkedHashMap<>();
            if (root.has("test_data_json")) {
                JsonNode tdNode = root.get("test_data_json");
                Iterator<String> fieldNames = tdNode.fieldNames();
                while (fieldNames.hasNext()) {
                    String key = fieldNames.next();
                    if ("_module".equals(key)) continue;
                    Map<String, String> fields = new LinkedHashMap<>();
                    JsonNode tcNode = tdNode.get(key);
                    Iterator<String> tcFields = tcNode.fieldNames();
                    while (tcFields.hasNext()) {
                        String field = tcFields.next();
                        fields.put(field, tcNode.get(field).asText());
                    }
                    testData.put(key, fields);
                }
            }

            // Parse declared custom steps from Gherkin Writer output
            List<String> customSteps = new ArrayList<>();
            if (root.has("new_step_definitions_needed")) {
                for (JsonNode stepNode : root.get("new_step_definitions_needed")) {
                    String step = stepNode.has("step") ? stepNode.get("step").asText() : "";
                    String reason = stepNode.has("reason") ? stepNode.get("reason").asText() : "";
                    customSteps.add(step + (reason.isEmpty() ? "" : " — " + reason));
                }
            }

            // Perform accurate reusability analysis against step catalog
            StepReusabilityAnalyzer analyzer = new StepReusabilityAnalyzer(catalog);
            StepReusabilityAnalyzer.AnalysisResult analysis = analyzer.analyze(featureContent, customSteps);

            return new GherkinResult(featureContent, testData, constantsContent, customSteps, analysis.getStats(), moduleName);

        } catch (Exception e) {
            System.out.println("[Spark] ERROR parsing Gherkin Writer output: " + e.getMessage());
            ReusabilityStats emptyStats = new ReusabilityStats(0, 0, Map.of());
            return new GherkinResult("", Map.of(), "", List.of("Parse error: " + e.getMessage()), emptyStats, moduleName);
        }
    }
}
