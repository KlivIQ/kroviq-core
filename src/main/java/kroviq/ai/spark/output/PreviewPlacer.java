package kroviq.ai.spark.output;

import kroviq.ai.spark.SparkConfig;
import kroviq.ai.spark.model.GherkinResult;
import kroviq.ai.spark.model.ReusabilityStats;
import kroviq.ai.spark.model.SparkOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PreviewPlacer {

    private final SparkConfig config;
    private final String projectRoot;

    public PreviewPlacer(SparkConfig config, String projectRoot) {
        this.config = config;
        this.projectRoot = projectRoot;
    }

    public SparkOutput place(GherkinResult result) {
        String module = result.getModuleName();
        Path previewDir = Path.of(projectRoot, config.getPreviewDir(), module);
        List<String> filesWritten = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            Files.createDirectories(previewDir);

            // Write feature file
            Path featurePath = previewDir.resolve(module + ".feature");
            Files.writeString(featurePath, result.getFeatureContent());
            filesWritten.add(featurePath.toString());

            // Write test data JSON
            Path testDataPath = previewDir.resolve(module + ".json");
            Files.writeString(testDataPath, renderTestDataJson(result.getTestData(), module));
            filesWritten.add(testDataPath.toString());

            // Write constants template
            Path constantsPath = previewDir.resolve(module + "Constants.java");
            Files.writeString(constantsPath, result.getConstantsContent());
            filesWritten.add(constantsPath.toString());

            // Write REVIEW.md
            Path reviewPath = previewDir.resolve("REVIEW.md");
            Files.writeString(reviewPath, renderReviewSummary(result));
            filesWritten.add(reviewPath.toString());

            // Write metadata
            Path metaPath = previewDir.resolve(".spark-meta.json");
            Files.writeString(metaPath, renderMeta(module, result));
            filesWritten.add(metaPath.toString());

        } catch (IOException e) {
            warnings.add("Failed to write preview files: " + e.getMessage());
        }

        // Add warnings for custom steps
        if (!result.getCustomStepsNeeded().isEmpty()) {
            warnings.add(result.getCustomStepsNeeded().size() + " custom step definitions needed before execution.");
        }

        return new SparkOutput(filesWritten, warnings, result.getReusability(), previewDir.toString());
    }

    private String renderTestDataJson(Map<String, Map<String, String>> testData, String module) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"_module\": \"").append(module).append("\"");

        for (Map.Entry<String, Map<String, String>> tc : testData.entrySet()) {
            sb.append(",\n  \"").append(tc.getKey()).append("\": {");
            List<Map.Entry<String, String>> fields = new ArrayList<>(tc.getValue().entrySet());
            for (int i = 0; i < fields.size(); i++) {
                Map.Entry<String, String> field = fields.get(i);
                sb.append("\n    \"").append(field.getKey()).append("\": \"").append(escapeJson(field.getValue())).append("\"");
                if (i < fields.size() - 1) sb.append(",");
            }
            sb.append("\n  }");
        }

        sb.append("\n}\n");
        return sb.toString();
    }

    private String renderReviewSummary(GherkinResult result) {
        ReusabilityStats stats = result.getReusability();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder sb = new StringBuilder();
        sb.append("# Spark Generation Summary — ").append(result.getModuleName()).append("\n\n");
        sb.append("**Generated:** ").append(timestamp).append("\n");
        sb.append("**Module:** ").append(result.getModuleName()).append("\n\n");
        sb.append("---\n\n");

        // Generated assets
        sb.append("## ✅ Generated Assets\n\n");
        sb.append("| Asset | File | Status |\n");
        sb.append("|-------|------|--------|\n");
        sb.append("| Feature File | ").append(result.getModuleName()).append(".feature | ✅ Ready for review |\n");
        sb.append("| Test Data | ").append(result.getModuleName()).append(".json | ✅ Ready for review |\n");
        sb.append("| Constants Template | ").append(result.getModuleName()).append("Constants.java | ⚠️ Locators required |\n\n");

        // Generation stats
        sb.append("## 📊 Generation Stats\n\n");
        sb.append("- Test cases generated: ").append(result.getTestData().size()).append("\n");
        sb.append("- Total steps: ").append(stats.totalSteps()).append("\n\n");

        // Automation readiness
        sb.append("## 🤖 Automation Readiness\n\n");
        sb.append("**Score: ").append(stats.reusabilityPercent()).append("%**\n\n");
        sb.append("**Status: ").append(stats.getReadinessLevel().getDisplay()).append("**\n\n");

        // Reusability analysis
        sb.append("## 🔄 Step Reusability Analysis\n\n");
        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        sb.append("| Existing reusable steps matched | ").append(stats.getMatchedSteps()).append(" |\n");
        sb.append("| Custom steps required | ").append(stats.getUnmatchedSteps()).append(" |\n");
        sb.append("| **Reusability Score** | **").append(stats.reusabilityPercent()).append("%** |\n\n");

        // Step usage breakdown
        if (!stats.getStepUsageCount().isEmpty()) {
            sb.append("### ✅ Reusable Steps Used\n\n");
            sb.append("| Step Pattern | Count |\n");
            sb.append("|---|---|\n");
            stats.getStepUsageCount().entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .limit(10)
                    .forEach(e -> sb.append("| `").append(e.getKey()).append("` | ").append(e.getValue()).append(" |\n"));
            sb.append("\n");
        }

        // Custom steps needed
        if (!result.getCustomStepsNeeded().isEmpty()) {
            sb.append("### ⚠️ Custom Steps Needed (").append(result.getCustomStepsNeeded().size()).append(")\n\n");
            for (String step : result.getCustomStepsNeeded()) {
                sb.append("- ").append(step).append("\n");
            }
            sb.append("\n");
        }

        // Manual work required
        sb.append("## ⚠️ Manual Work Required\n\n");
        sb.append("| Item | Priority | Notes |\n");
        sb.append("|------|----------|-------|\n");
        sb.append("| Locator mapping | HIGH | All constants need real XPath/CSS |\n");
        if (!result.getCustomStepsNeeded().isEmpty()) {
            sb.append("| Custom step definitions | MEDIUM | ").append(result.getCustomStepsNeeded().size()).append(" steps not covered by CommonSteps |\n");
        }
        sb.append("| Business rule validation | MEDIUM | Verify scenario coverage with domain expert |\n");
        sb.append("| Test data refinement | LOW | Sample values generated — verify with real data |\n\n");

        // Apply instructions
        sb.append("## 📋 Next Steps\n\n");
        sb.append("1. Review ").append(result.getModuleName()).append(".feature — verify scenario coverage\n");
        sb.append("2. Review ").append(result.getModuleName()).append(".json — verify test data accuracy\n");
        sb.append("3. Fill locators in ").append(result.getModuleName()).append("Constants.java\n");

        if (stats.getReadinessLevel() == ReusabilityStats.ReadinessLevel.LOW_READINESS) {
            sb.append("4. ❌ Apply blocked — reusability below 50%. Use `--force` to override.\n");
        } else {
            sb.append("4. Run: `mvn exec:java \"-Dexec.args=--apply ").append(result.getModuleName()).append("\"`\n");
        }

        return sb.toString();
    }

    private String renderMeta(String module, GherkinResult result) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return "{\n" +
                "  \"module\": \"" + module + "\",\n" +
                "  \"generatedAt\": \"" + timestamp + "\",\n" +
                "  \"reusabilityPercent\": " + result.getReusability().reusabilityPercent() + ",\n" +
                "  \"readinessLevel\": \"" + result.getReusability().getReadinessLevel().name() + "\",\n" +
                "  \"totalSteps\": " + result.getReusability().totalSteps() + ",\n" +
                "  \"matchedSteps\": " + result.getReusability().getMatchedSteps() + ",\n" +
                "  \"unmatchedSteps\": " + result.getReusability().getUnmatchedSteps() + ",\n" +
                "  \"customStepsNeeded\": " + result.getCustomStepsNeeded().size() + ",\n" +
                "  \"testCases\": " + result.getTestData().size() + "\n" +
                "}\n";
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
