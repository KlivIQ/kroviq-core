package kroviq.ai.spark.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kroviq.ai.spark.SparkConfig;
import kroviq.ai.spark.model.ReusabilityStats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class AssetApplier {

    private final SparkConfig config;
    private final String projectRoot;
    private static final ObjectMapper mapper = new ObjectMapper();

    public AssetApplier(SparkConfig config, String projectRoot) {
        this.config = config;
        this.projectRoot = projectRoot;
    }

    public void apply(String module, boolean force) {
        Path previewDir = Path.of(projectRoot, config.getPreviewDir(), module);

        if (!Files.exists(previewDir)) {
            System.out.println("[Spark] ERROR: No preview found for module '" + module + "'");
            System.out.println("[Spark] Run --brd first to generate preview assets.");
            return;
        }

        // Check readiness level
        if (!force && isBlockedByReadiness(previewDir)) {
            System.out.println("[Spark] ❌ Apply BLOCKED — reusability below 50%.");
            System.out.println("[Spark] Use --force to override, or improve BRD quality.");
            return;
        }

        List<String> applied = new ArrayList<>();

        try {
            // Apply feature file
            Path featureSource = previewDir.resolve(module + ".feature");
            if (Files.exists(featureSource)) {
                Path featureTarget = Path.of(projectRoot, config.getFeaturesPath(module), module + ".feature");
                applyFile(featureSource, featureTarget, force);
                applied.add(featureTarget.toString());
            }

            // Apply test data
            Path testDataSource = previewDir.resolve(module + ".json");
            if (Files.exists(testDataSource)) {
                Path testDataTarget = Path.of(projectRoot, config.getTestDataPath(), module + ".json");
                applyFile(testDataSource, testDataTarget, force);
                applied.add(testDataTarget.toString());
            }

            // Apply constants
            Path constantsSource = previewDir.resolve(module + "Constants.java");
            if (Files.exists(constantsSource)) {
                Path constantsTarget = Path.of(projectRoot, config.getConstantsPath(), module + "Constants.java");
                applyFile(constantsSource, constantsTarget, force);
                applied.add(constantsTarget.toString());
            }

            System.out.println("[Spark] ✅ Applied " + applied.size() + " files for module '" + module + "':");
            applied.forEach(f -> System.out.println("  → " + f));

        } catch (IOException e) {
            System.out.println("[Spark] ERROR during apply: " + e.getMessage());
        }
    }

    private void applyFile(Path source, Path target, boolean force) throws IOException {
        if (Files.exists(target) && !force) {
            System.out.println("[Spark] SKIPPED (exists): " + target);
            System.out.println("[Spark]   Use --force to overwrite, or delete the existing file.");
            return;
        }
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private boolean isBlockedByReadiness(Path previewDir) {
        Path metaPath = previewDir.resolve(".spark-meta.json");
        if (!Files.exists(metaPath)) return false;

        try {
            JsonNode meta = mapper.readTree(Files.readString(metaPath));
            String level = meta.has("readinessLevel") ? meta.get("readinessLevel").asText() : "";
            return "LOW_READINESS".equals(level);
        } catch (IOException e) {
            return false;
        }
    }
}
