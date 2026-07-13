package kroviq.utils;

import io.cucumber.core.cli.Main;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IterationRunner {
    
    public static void executeWithIterations() {
        try {
            IterationExecutor.initializeIterations();
            
            TestDataManager tdm = TestDataManager.get();
            boolean hasMultipleIterations = false;
            
            try {
                java.lang.reflect.Field iterationCountsField = IterationExecutor.class.getDeclaredField("iterationCounts");
                iterationCountsField.setAccessible(true);
                ThreadLocal<Map<String, Integer>> iterationCounts = 
                    (ThreadLocal<Map<String, Integer>>) iterationCountsField.get(null);
                
                if (iterationCounts.get() != null) {
                    for (Map.Entry<String, Integer> entry : iterationCounts.get().entrySet()) {
                        String key = entry.getKey();
                        int iterations = entry.getValue();
                        
                        if (iterations > 1) {
                            hasMultipleIterations = true;
                            String[] parts = key.split("\\.");
                            String moduleName = parts[0];
                            String testCaseId = parts[1];
                            
                            System.out.println(String.format("Iteration: %s has %d iterations in module %s", 
                                testCaseId, iterations, moduleName));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error checking iteration counts: " + e.getMessage());
            }
            
            if (!hasMultipleIterations) {
                System.out.println("No multiple iterations detected - executing normally");
            }
            
        } catch (Exception e) {
            System.err.println("Error in iteration execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void executeSingleIteration(int current, int total) {
        try {
            System.out.println(String.format("Executing iteration %d/%d", current, total));
            
            List<String> args = new ArrayList<>();
            args.add("--glue");
            args.add("kroviq.hooks");
            args.add("--glue");
            args.add("stepdefinitions");
            args.add("--tags");
            args.add("@TC_LOGIN_001");
            args.add("--plugin");
            args.add("pretty");
            args.add("src/test/resources/features/Login/Login.feature");
            
            Main.run(args.toArray(new String[0]), Thread.currentThread().getContextClassLoader());
            
            System.out.println(String.format("Completed iteration %d/%d", current, total));
            
        } catch (Exception e) {
            System.err.println(String.format("Error in iteration %d: %s", current, e.getMessage()));
        }
    }
}
