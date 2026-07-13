package kroviq.utils;

import io.cucumber.java.Scenario;
import java.lang.reflect.Method;

public class ScenarioRepeater {
    private static final ThreadLocal<Boolean> isRepeating = new ThreadLocal<>();
    private static final ThreadLocal<Integer> repeatCount = new ThreadLocal<>();
    
    public static void executeIterations(Scenario scenario, Runnable scenarioExecution) {
        String testCaseId = extractTestCaseId(scenario);
        String moduleName = getModuleNameFromScenario(scenario);
        
        // Check if this test case has multiple iterations
        if (IterationExecutor.hasIterations(moduleName, testCaseId)) {
            int totalIterations = IterationExecutor.getIterationCount(moduleName, testCaseId);
            
            System.out.println(String.format("[ITER] Test case %s has %d iterations - executing multiple times", 
                testCaseId, totalIterations));
            
            // Execute the scenario multiple times
            for (int i = 0; i < totalIterations; i++) {
                System.out.println(String.format("&#9654;  Executing iteration %d of %d for %s", 
                    i + 1, totalIterations, testCaseId));
                
                // Set current iteration
                IterationExecutor.setCurrentIteration(moduleName, testCaseId, i);
                
                // Mark as repeating to avoid infinite loops
                isRepeating.set(true);
                repeatCount.set(i);
                
                try {
                    // Execute the scenario
                    scenarioExecution.run();
                    System.out.println(String.format("[OK] Completed iteration %d of %d", i + 1, totalIterations));
                } catch (Exception e) {
                    System.err.println(String.format("[FAIL] Error in iteration %d: %s", i + 1, e.getMessage()));
                } finally {
                    isRepeating.remove();
                    repeatCount.remove();
                }
            }
        } else {
            // Single execution for non-iterative test cases
            scenarioExecution.run();
        }
    }
    
    public static boolean isCurrentlyRepeating() {
        return Boolean.TRUE.equals(isRepeating.get());
    }
    
    public static int getCurrentRepeatIndex() {
        return repeatCount.get() != null ? repeatCount.get() : 0;
    }
    
    private static String extractTestCaseId(Scenario scenario) {
        for (String tag : scenario.getSourceTagNames()) {
            if (tag.matches("@TC_.*|@.*TC.*")) {
                return tag.replace("@", "");
            }
        }
        return "N/A";
    }
    
    private static String getModuleNameFromScenario(Scenario scenario) {
        // Extract module name from scenario URI: features/{ModuleName}/{ModuleName}.feature
        String uri = scenario.getUri().toString();
        String[] parts = uri.replace("\\", "/").split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("testscripts".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return "ProductConfig"; // Fallback
    }
}
