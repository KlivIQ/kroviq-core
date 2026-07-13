package kroviq.ai.spark;

import kroviq.ai.spark.catalog.StepCatalog;
import kroviq.ai.spark.catalog.StepCatalogLoader;
import kroviq.ai.spark.model.GherkinResult;
import kroviq.ai.spark.model.ReusabilityStats;
import kroviq.ai.spark.model.SparkInput;
import kroviq.ai.spark.model.SparkOutput;
import kroviq.ai.spark.output.PreviewPlacer;
import kroviq.ai.spark.parser.GherkinResultParser;
import kroviq.ai.spark.stages.FlowAnalyzer;
import kroviq.ai.spark.stages.GherkinWriter;
import kroviq.ai.spark.stages.ScenarioGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Kroviq Spark — Pipeline Orchestrator
 *
 * Chains: FlowAnalyzer → ScenarioGenerator → GherkinWriter → PreviewPlacer
 *
 * Two execution modes:
 * 1. generatePrompts() — produces the 3 stage prompts for AI execution
 * 2. generateFromOutputs() — takes AI outputs and produces final assets
 */
public class SparkOrchestrator {

    private final SparkConfig config;
    private final String projectRoot;
    private final StepCatalog catalog;

    public SparkOrchestrator(String projectRoot) {
        this.projectRoot = projectRoot;
        this.config = SparkConfig.load(projectRoot);
        this.catalog = StepCatalogLoader.loadFromProjectRoot(projectRoot);
    }

    public SparkOrchestrator(String projectRoot, StepCatalog catalog) {
        this.projectRoot = projectRoot;
        this.config = SparkConfig.load(projectRoot);
        this.catalog = catalog;
    }

    /**
     * Phase 1A: Generate the Flow Analyzer prompt from BRD input.
     * User executes this prompt with AI and provides the output to Phase 1B.
     */
    public String generateFlowAnalyzerPrompt(SparkInput input) {
        System.out.println("[Spark] Stage 1: Generating Flow Analyzer prompt for module '" + input.getModuleName() + "'");
        return new FlowAnalyzer().analyze(input);
    }

    /**
     * Phase 1B: Generate the Scenario Generator prompt from Flow JSON.
     * User provides Flow Analyzer output, gets Scenario Generator prompt.
     */
    public String generateScenarioGeneratorPrompt(String flowJson, String moduleName) {
        System.out.println("[Spark] Stage 2: Generating Scenario Generator prompt for module '" + moduleName + "'");
        return new ScenarioGenerator().generate(flowJson, moduleName);
    }

    /**
     * Phase 1C: Generate the Gherkin Writer prompt from Scenarios JSON.
     * User provides Scenario Generator output, gets Gherkin Writer prompt.
     */
    public String generateGherkinWriterPrompt(String scenariosJson, String moduleName, String pageName) {
        System.out.println("[Spark] Stage 3: Generating Gherkin Writer prompt for module '" + moduleName + "'");
        System.out.println("[Spark] Step catalog: " + catalog.size() + " reusable steps available");
        return new GherkinWriter().write(scenariosJson, moduleName, pageName, catalog);
    }

    /**
     * Phase 2: Parse Gherkin Writer output and place preview assets.
     * Called after user has the final Gherkin Writer JSON output.
     */
    public SparkOutput placePreview(String gherkinWriterOutput, String moduleName) {
        System.out.println("[Spark] Parsing Gherkin Writer output and placing preview...");
        System.out.println("[Spark] Analyzing reusability against " + catalog.size() + " catalog steps...");

        GherkinResult result = GherkinResultParser.parse(gherkinWriterOutput, moduleName, catalog);

        PreviewPlacer placer = new PreviewPlacer(config, projectRoot);
        SparkOutput output = placer.place(result);

        // Print summary
        printSummary(output, result);
        return output;
    }

    /**
     * Full pipeline execution with all 3 stage outputs provided.
     * For automated/batch mode where all AI outputs are available.
     */
    public SparkOutput generateFromOutputs(String flowJson, String scenariosJson,
                                           String gherkinWriterOutput, String moduleName) {
        System.out.println("[Spark] Full pipeline — processing all stage outputs for '" + moduleName + "'");
        return placePreview(gherkinWriterOutput, moduleName);
    }

    /**
     * Saves intermediate stage output to spark-output/{module}/ for debugging.
     */
    public void saveIntermediateOutput(String moduleName, String stageName, String content) {
        try {
            Path dir = Path.of(projectRoot, config.getPreviewDir(), moduleName);
            Files.createDirectories(dir);
            Path file = dir.resolve(stageName + "-output.json");
            Files.writeString(file, content);
            System.out.println("[Spark] Saved " + stageName + " output → " + file);
        } catch (IOException e) {
            System.out.println("[Spark] WARNING: Could not save intermediate output: " + e.getMessage());
        }
    }


    private void printSummary(SparkOutput output, GherkinResult result) {
        ReusabilityStats stats = result.getReusability();
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║         Kroviq Spark — Generation Complete           ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║ Module: " + padRight(result.getModuleName(), 43) + "║");
        System.out.println("║ Files generated: " + padRight(String.valueOf(output.getFilesWritten().size()), 34) + "║");
        System.out.println("║ Test cases: " + padRight(String.valueOf(result.getTestData().size()), 39) + "║");
        System.out.println("║ Reusability: " + padRight(stats.reusabilityPercent() + "%", 38) + "║");
        System.out.println("║ Status: " + padRight(stats.getReadinessLevel().getDisplay(), 43) + "║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║ Preview: " + padRight(output.getPreviewDir(), 42) + "║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        if (!output.getWarnings().isEmpty()) {
            System.out.println("Warnings:");
            output.getWarnings().forEach(w -> System.out.println("  ⚠ " + w));
            System.out.println();
        }

        System.out.println("Next: Review REVIEW.md then run --apply " + result.getModuleName());
    }

    private String padRight(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }
}
