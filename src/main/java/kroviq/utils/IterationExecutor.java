package kroviq.utils;

import java.util.HashMap;
import java.util.Map;

public class IterationExecutor {
    private static final ThreadLocal<Map<String, Integer>> currentIterations = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, Integer>> iterationCounts = new ThreadLocal<>();
    
    public static void initializeIterations() {
        currentIterations.set(new HashMap<>());
        iterationCounts.set(new HashMap<>());
        
        // Scan test data for iteration counts from TestDataManager
        TestDataManager tdm = TestDataManager.get();
        Map<String, Integer> counts = new HashMap<>();
        
        // Get all iteration data from TestDataManager
        try {
            java.lang.reflect.Field iterationDataField = TestDataManager.class.getDeclaredField("iterationData");
            iterationDataField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, java.util.List<Map<String, String>>> iterationData = 
                (Map<String, java.util.List<Map<String, String>>>) iterationDataField.get(tdm);
            
            for (Map.Entry<String, java.util.List<Map<String, String>>> entry : iterationData.entrySet()) {
                int count = entry.getValue().size();
                if (count > 1) {
                    counts.put(entry.getKey(), count);
                }
            }
        } catch (Exception e) {
            System.err.println("Error accessing iteration data: " + e.getMessage());
        }
        
        iterationCounts.get().putAll(counts);
    }
    
    public static boolean hasIterations(String page, String testCaseId) {
        String key = page + "." + testCaseId;
        return iterationCounts.get() != null && iterationCounts.get().getOrDefault(key, 0) > 1;
    }
    
    public static int getIterationCount(String page, String testCaseId) {
        String key = page + "." + testCaseId;
        return iterationCounts.get() != null ? iterationCounts.get().getOrDefault(key, 1) : 1;
    }
    
    public static int getCurrentIteration(String page, String testCaseId) {
        String key = page + "." + testCaseId;
        return currentIterations.get() != null ? currentIterations.get().getOrDefault(key, -1) : -1;
    }
    
    public static void setCurrentIteration(String page, String testCaseId, int iteration) {
        if (currentIterations.get() == null) {
            currentIterations.set(new HashMap<>());
        }
        String key = page + "." + testCaseId;
        currentIterations.get().put(key, iteration);
    }
    
    public static String getModuleNameForTestCase(String testCaseId) {
        if (iterationCounts.get() == null) return null;
        
        for (String key : iterationCounts.get().keySet()) {
            if (key.endsWith("." + testCaseId)) {
                return key.substring(0, key.lastIndexOf("."));
            }
        }
        return null;
    }
    
    public static void clearIterations() {
        if (currentIterations.get() != null) {
            currentIterations.get().clear();
        }
        if (iterationCounts.get() != null) {
            iterationCounts.get().clear();
        }
        currentIterations.remove();
        iterationCounts.remove();
    }
}