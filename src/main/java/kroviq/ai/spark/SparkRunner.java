package kroviq.ai.spark;

import kroviq.ai.spark.model.SparkInput;
import kroviq.ai.spark.model.SparkOutput;
import kroviq.ai.spark.output.AssetApplier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Kroviq Spark — CLI Entry Point
 *
 * Usage:
 *   --brd <file> --module <name>         Generate prompts from BRD (Phase 1)
 *   --flow <file> --module <name>        Generate Scenario Generator prompt from Flow JSON
 *   --scenarios <file> --module <name>   Generate Gherkin Writer prompt from Scenarios JSON
 *   --finalize <file> --module <name>    Parse Gherkin Writer output and place preview
 *   --apply <module>                     Apply reviewed assets to framework (Phase 2)
 *   --apply <module> --force             Apply even if readiness is low
 *   --clean <module>                     Remove spark-output/{module}/ preview
 *   --status                             Show pending reviews in spark-output/
 *
 * Options:
 *   --context <text>                     Optional business context
 *   --page <name>                        Custom page name (default: {Module}Page)
 */
public class SparkRunner {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String projectRoot = System.getProperty("user.dir");
        String command = args[0];

        try {
            switch (command) {
                case "--brd" -> handleBrd(args, projectRoot);
                case "--flow" -> handleFlow(args, projectRoot);
                case "--scenarios" -> handleScenarios(args, projectRoot);
                case "--finalize" -> handleFinalize(args, projectRoot);
                case "--apply" -> handleApply(args, projectRoot);
                case "--clean" -> handleClean(args, projectRoot);
                case "--status" -> handleStatus(projectRoot);
                default -> {
                    System.out.println("[Spark] Unknown command: " + command);
                    printUsage();
                }
            }
        } catch (Exception e) {
            System.out.println("[Spark] ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleBrd(String[] args, String projectRoot) throws IOException {
        String brdFile = getArg(args, "--brd");
        String module = getArg(args, "--module");
        String context = getArgOptional(args, "--context", "");
        String page = getArgOptional(args, "--page", null);

        if (brdFile == null || module == null) {
            System.out.println("[Spark] ERROR: --brd and --module are required.");
            return;
        }

        String brdContent = Files.readString(Path.of(brdFile));
        SparkInput input = SparkInput.builder(brdContent, module)
                .businessContext(context)
                .pageName(page)
                .build();

        SparkOrchestrator orchestrator = new SparkOrchestrator(projectRoot);
        String prompt = orchestrator.generateFlowAnalyzerPrompt(input);

        // Save prompt for user to execute
        Path outputDir = Path.of(projectRoot, "spark-output", module);
        Files.createDirectories(outputDir);
        Path promptFile = outputDir.resolve("stage1-flow-analyzer-prompt.txt");
        Files.writeString(promptFile, prompt);

        System.out.println("[Spark] ✅ Flow Analyzer prompt generated.");
        System.out.println("[Spark] File: " + promptFile);
        System.out.println();
        System.out.println("Next steps:");
        System.out.println("  1. Execute this prompt with AI (paste into chat or API)");
        System.out.println("  2. Save the Flow JSON output to a file");
        System.out.println("  3. Run: --flow <flow-output.json> --module " + module);
    }

    private static void handleFlow(String[] args, String projectRoot) throws IOException {
        String flowFile = getArg(args, "--flow");
        String module = getArg(args, "--module");
        String page = getArgOptional(args, "--page", null);

        if (flowFile == null || module == null) {
            System.out.println("[Spark] ERROR: --flow and --module are required.");
            return;
        }

        String flowJson = Files.readString(Path.of(flowFile));
        SparkOrchestrator orchestrator = new SparkOrchestrator(projectRoot);

        // Save intermediate
        orchestrator.saveIntermediateOutput(module, "flow-analysis", flowJson);

        String prompt = orchestrator.generateScenarioGeneratorPrompt(flowJson, module);

        Path outputDir = Path.of(projectRoot, "spark-output", module);
        Files.createDirectories(outputDir);
        Path promptFile = outputDir.resolve("stage2-scenario-generator-prompt.txt");
        Files.writeString(promptFile, prompt);

        System.out.println("[Spark] ✅ Scenario Generator prompt generated.");
        System.out.println("[Spark] File: " + promptFile);
        System.out.println();
        System.out.println("Next steps:");
        System.out.println("  1. Execute this prompt with AI");
        System.out.println("  2. Save the Scenarios JSON output to a file");
        System.out.println("  3. Run: --scenarios <scenarios-output.json> --module " + module);
    }

    private static void handleScenarios(String[] args, String projectRoot) throws IOException {
        String scenariosFile = getArg(args, "--scenarios");
        String module = getArg(args, "--module");
        String page = getArgOptional(args, "--page", module + "Page");

        if (scenariosFile == null || module == null) {
            System.out.println("[Spark] ERROR: --scenarios and --module are required.");
            return;
        }

        String scenariosJson = Files.readString(Path.of(scenariosFile));
        SparkOrchestrator orchestrator = new SparkOrchestrator(projectRoot);

        // Save intermediate
        orchestrator.saveIntermediateOutput(module, "scenarios", scenariosJson);

        String prompt = orchestrator.generateGherkinWriterPrompt(scenariosJson, module, page);

        Path outputDir = Path.of(projectRoot, "spark-output", module);
        Files.createDirectories(outputDir);
        Path promptFile = outputDir.resolve("stage3-gherkin-writer-prompt.txt");
        Files.writeString(promptFile, prompt);

        System.out.println("[Spark] ✅ Gherkin Writer prompt generated.");
        System.out.println("[Spark] File: " + promptFile);
        System.out.println();
        System.out.println("Next steps:");
        System.out.println("  1. Execute this prompt with AI");
        System.out.println("  2. Save the Gherkin Writer JSON output to a file");
        System.out.println("  3. Run: --finalize <gherkin-output.json> --module " + module);
    }

    private static void handleFinalize(String[] args, String projectRoot) throws IOException {
        String gherkinFile = getArg(args, "--finalize");
        String module = getArg(args, "--module");

        if (gherkinFile == null || module == null) {
            System.out.println("[Spark] ERROR: --finalize and --module are required.");
            return;
        }

        String gherkinOutput = Files.readString(Path.of(gherkinFile));
        SparkOrchestrator orchestrator = new SparkOrchestrator(projectRoot);
        orchestrator.placePreview(gherkinOutput, module);
    }

    private static void handleApply(String[] args, String projectRoot) {
        String module = args.length > 1 ? args[1] : null;
        boolean force = hasFlag(args, "--force");

        if (module == null || module.startsWith("--")) {
            System.out.println("[Spark] ERROR: Module name required. Usage: --apply <module>");
            return;
        }

        SparkConfig config = SparkConfig.load(projectRoot);
        AssetApplier applier = new AssetApplier(config, projectRoot);
        applier.apply(module, force);
    }

    private static void handleClean(String[] args, String projectRoot) throws IOException {
        String module = args.length > 1 ? args[1] : null;
        if (module == null || module.startsWith("--")) {
            System.out.println("[Spark] ERROR: Module name required. Usage: --clean <module>");
            return;
        }

        Path previewDir = Path.of(projectRoot, "spark-output", module);
        if (!Files.exists(previewDir)) {
            System.out.println("[Spark] No preview found for module '" + module + "'");
            return;
        }

        Files.walk(previewDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });

        System.out.println("[Spark] ✅ Cleaned spark-output/" + module + "/");
    }

    private static void handleStatus(String projectRoot) throws IOException {
        Path sparkOutput = Path.of(projectRoot, "spark-output");
        if (!Files.exists(sparkOutput)) {
            System.out.println("[Spark] No pending reviews. spark-output/ does not exist.");
            return;
        }

        System.out.println("[Spark] Pending reviews in spark-output/:");
        System.out.println();

        Files.list(sparkOutput)
                .filter(Files::isDirectory)
                .forEach(dir -> {
                    String module = dir.getFileName().toString();
                    boolean hasReview = Files.exists(dir.resolve("REVIEW.md"));
                    boolean hasFeature = Files.exists(dir.resolve(module + ".feature"));
                    System.out.println("  " + module + (hasReview ? " [REVIEW.md ✓]" : "") + (hasFeature ? " [.feature ✓]" : " [incomplete]"));
                });
    }

    private static String getArg(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(flag)) return args[i + 1];
        }
        return null;
    }

    private static String getArgOptional(String[] args, String flag, String defaultValue) {
        String value = getArg(args, flag);
        return value != null ? value : defaultValue;
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equals(flag)) return true;
        }
        return false;
    }

    private static void printUsage() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║           Kroviq Spark — BRD to Gherkin              ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  --brd <file> --module <name>         Stage 1: BRD → Flow Analyzer prompt");
        System.out.println("  --flow <file> --module <name>        Stage 2: Flow JSON → Scenario Generator prompt");
        System.out.println("  --scenarios <file> --module <name>   Stage 3: Scenarios → Gherkin Writer prompt");
        System.out.println("  --finalize <file> --module <name>    Finalize: Parse output → preview assets");
        System.out.println("  --apply <module>                     Apply reviewed assets to framework");
        System.out.println("  --apply <module> --force             Apply even if readiness is low");
        System.out.println("  --clean <module>                     Remove preview for module");
        System.out.println("  --status                             Show pending reviews");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --context <text>                     Business context for Flow Analyzer");
        System.out.println("  --page <name>                        Custom page name (default: {Module}Page)");
        System.out.println();
        System.out.println("Example workflow:");
        System.out.println("  1. mvn exec:java \"-Dexec.args=--brd docs/Login.md --module Login\"");
        System.out.println("  2. Execute generated prompt with AI → save output");
        System.out.println("  3. mvn exec:java \"-Dexec.args=--flow flow-output.json --module Login\"");
        System.out.println("  4. Execute generated prompt with AI → save output");
        System.out.println("  5. mvn exec:java \"-Dexec.args=--scenarios scenarios-output.json --module Login\"");
        System.out.println("  6. Execute generated prompt with AI → save output");
        System.out.println("  7. mvn exec:java \"-Dexec.args=--finalize gherkin-output.json --module Login\"");
        System.out.println("  8. Review spark-output/Login/REVIEW.md");
        System.out.println("  9. mvn exec:java \"-Dexec.args=--apply Login\"");
        System.out.println();
    }
}
