package kroviq.ai.spark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SparkConfig {

    private String featuresPath = "src/test/resources/features/{module}/";
    private String testDataPath = "TestDatastore/json/";
    private String constantsPath = "src/main/java/kroviq/constants/";
    private String previewDir = "spark-output/";
    private int maxScenariosPerFlow = 6;
    private boolean includeEdgeCases = true;
    private boolean includeNegativeCases = true;
    private String tagPrefix = "@TC_{MODULE}_";

    private static final ObjectMapper mapper = new ObjectMapper();

    public static SparkConfig load(String projectRoot) {
        SparkConfig config = new SparkConfig();
        Path configPath = Path.of(projectRoot, "spark-config.json");

        if (Files.exists(configPath)) {
            try {
                JsonNode root = mapper.readTree(Files.readString(configPath));
                JsonNode paths = root.get("outputPaths");
                if (paths != null) {
                    if (paths.has("features")) config.featuresPath = paths.get("features").asText();
                    if (paths.has("testData")) config.testDataPath = paths.get("testData").asText();
                    if (paths.has("constants")) config.constantsPath = paths.get("constants").asText();
                }
                JsonNode gen = root.get("generation");
                if (gen != null) {
                    if (gen.has("maxScenariosPerFlow")) config.maxScenariosPerFlow = gen.get("maxScenariosPerFlow").asInt();
                    if (gen.has("includeEdgeCases")) config.includeEdgeCases = gen.get("includeEdgeCases").asBoolean();
                    if (gen.has("includeNegativeCases")) config.includeNegativeCases = gen.get("includeNegativeCases").asBoolean();
                    if (gen.has("tagPrefix")) config.tagPrefix = gen.get("tagPrefix").asText();
                }
                JsonNode preview = root.get("preview");
                if (preview != null && preview.has("outputDir")) {
                    config.previewDir = preview.get("outputDir").asText();
                }
                System.out.println("[Spark] Configuration loaded from " + configPath);
            } catch (IOException e) {
                System.out.println("[Spark] WARNING: Failed to parse spark-config.json, using defaults: " + e.getMessage());
            }
        } else {
            System.out.println("[Spark] No spark-config.json found, using defaults.");
        }

        return config;
    }

    public String getFeaturesPath(String module) {
        return featuresPath.replace("{module}", module);
    }

    public String getTestDataPath() { return testDataPath; }
    public String getConstantsPath() { return constantsPath; }
    public String getPreviewDir() { return previewDir; }
    public int getMaxScenariosPerFlow() { return maxScenariosPerFlow; }
    public boolean isIncludeEdgeCases() { return includeEdgeCases; }
    public boolean isIncludeNegativeCases() { return includeNegativeCases; }
    public String getTagPrefix() { return tagPrefix; }
}
