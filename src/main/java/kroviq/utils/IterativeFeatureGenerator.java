package kroviq.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class IterativeFeatureGenerator {
    private static final String RUNTIME_FEATURES_DIR = "target/runtime_features";
    
    public static List<String> generateIterativeFeatures(List<String> originalFeaturePaths) {
        List<String> generatedPaths = new ArrayList<>();
        
        try {
            cleanRuntimeDirectory();
            TestDataManager.get().reloadTestData();
            IterationExecutor.initializeIterations();
            
            // Discover actual .feature files from provided paths (directories or files)
            List<String> featureFiles = discoverFeatureFiles(originalFeaturePaths);
            
            if (featureFiles.isEmpty()) {
                System.out.println("[WARN]  No feature files found in provided paths");
                return originalFeaturePaths;
            }
            
            for (String featureFile : featureFiles) {
                String generated = processFeatureFile(featureFile);
                if (generated != null) {
                    generatedPaths.add(generated);
                }
            }
            
            System.out.println("[OK] Iterative feature generation completed: " + generatedPaths.size() + " files");
            
        } catch (Exception e) {
            System.err.println("[FAIL] Error generating iterative features: " + e.getMessage());
            e.printStackTrace();
            return originalFeaturePaths; // Fallback to original
        }
        
        return generatedPaths.isEmpty() ? originalFeaturePaths : generatedPaths;
    }
    
    private static List<String> discoverFeatureFiles(List<String> paths) throws IOException {
        List<String> featureFiles = new ArrayList<>();
        
        for (String path : paths) {
            Path p = Paths.get(path);
            
            if (Files.isDirectory(p)) {
                // Recursively scan directory for .feature files
                Files.walk(p)
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".feature"))
                    .forEach(file -> featureFiles.add(file.toString()));
            } else if (Files.isRegularFile(p) && path.endsWith(".feature")) {
                // Already a feature file
                featureFiles.add(path);
            }
        }
        
        return featureFiles;
    }
    
    private static void cleanRuntimeDirectory() throws IOException {
        Path runtimeDir = Paths.get(RUNTIME_FEATURES_DIR);
        if (Files.exists(runtimeDir)) {
            Files.walk(runtimeDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException e) {}
                });
        }
        Files.createDirectories(runtimeDir);
        System.out.println("[CLEAN] Cleaned runtime features directory");
    }
    
    private static String processFeatureFile(String featureFilePath) throws IOException {
        Path originalPath = Paths.get(featureFilePath);
        if (!Files.exists(originalPath)) {
            System.err.println("[WARN]  Feature file not found: " + featureFilePath);
            return null;
        }
        
        List<String> originalLines = Files.readAllLines(originalPath);
        List<String> outputLines = new ArrayList<>();
        
        String moduleName = extractModuleNameFromPath(featureFilePath);
        boolean hasIterations = false;
        
        int i = 0;
        while (i < originalLines.size()) {
            String line = originalLines.get(i);
            
            // Check if this is a test case tag line
            if (line.trim().startsWith("@") && line.contains("TC_")) {
                String testCaseId = extractTestCaseId(line);
                
                if (testCaseId != null && IterationExecutor.hasIterations(moduleName, testCaseId)) {
                    hasIterations = true;
                    int iterationCount = IterationExecutor.getIterationCount(moduleName, testCaseId);
                    
                    // Collect scenario block (tags + scenario line + steps)
                    List<String> scenarioBlock = collectScenarioBlock(originalLines, i);
                    
                    // Generate iterations
                    for (int iter = 1; iter <= iterationCount; iter++) {
                        outputLines.addAll(generateIteratedScenario(scenarioBlock, testCaseId, iter, iterationCount));
                        outputLines.add(""); // Blank line between scenarios
                    }
                    
                    System.out.println(String.format("[ITER] %s: Generated %d iterations", testCaseId, iterationCount));
                    
                    // Skip the original scenario block
                    i += scenarioBlock.size();
                    continue;
                }
            }
            
            outputLines.add(line);
            i++;
        }
        
        // Preserve directory structure from testscripts onward
        String relativePath = extractRelativePath(featureFilePath);
        Path outputPath = Paths.get(RUNTIME_FEATURES_DIR, relativePath);
        
        // Create parent directories
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, outputLines);
        
        if (hasIterations) {
            System.out.println("[FILE] Generated: " + outputPath);
        }
        
        return outputPath.toString();
    }
    
    private static String extractRelativePath(String featureFilePath) {
        String normalized = featureFilePath.replace("\\", "/");
        int index = normalized.indexOf("testscripts/");
        if (index >= 0) {
            return normalized.substring(index);
        }
        // Fallback: just filename
        return Paths.get(featureFilePath).getFileName().toString();
    }
    
    private static List<String> collectScenarioBlock(List<String> lines, int startIndex) {
        List<String> block = new ArrayList<>();
        int i = startIndex;
        
        // Collect all tags
        while (i < lines.size() && lines.get(i).trim().startsWith("@")) {
            block.add(lines.get(i));
            i++;
        }
        
        // Collect scenario line
        if (i < lines.size() && lines.get(i).trim().startsWith("Scenario")) {
            block.add(lines.get(i));
            i++;
        }
        
        // Collect steps until blank line or next scenario
        while (i < lines.size()) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("@") || line.startsWith("Scenario")) {
                break;
            }
            block.add(lines.get(i));
            i++;
        }
        
        return block;
    }
    
    private static List<String> generateIteratedScenario(List<String> scenarioBlock, String testCaseId, int iteration, int total) {
        List<String> output = new ArrayList<>();
        
        for (String line : scenarioBlock) {
            if (line.trim().startsWith("Scenario:")) {
                // Add iteration tag before scenario
                output.add(String.format("@ITERATION_%d", iteration));
                // Modify scenario name
                String scenarioName = line.substring(line.indexOf("Scenario:") + 9).trim();
                output.add(String.format("Scenario: %s [Iteration %d]", scenarioName, iteration));
            } else {
                output.add(line);
            }
        }
        
        return output;
    }
    
    private static String extractTestCaseId(String tagLine) {
        String[] tags = tagLine.trim().split("\\s+");
        for (String tag : tags) {
            if (tag.startsWith("@TC_")) {
                return tag.substring(1); // Remove @
            }
        }
        return null;
    }
    
    private static String extractModuleNameFromPath(String featurePath) {
        String[] parts = featurePath.replace("\\", "/").split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("testscripts".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return "ProductConfig";
    }
}