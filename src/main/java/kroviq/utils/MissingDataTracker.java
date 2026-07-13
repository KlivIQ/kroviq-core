package kroviq.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MissingDataTracker {
    
    private static final ThreadLocal<List<MissingDataEntry>> missingData = 
        ThreadLocal.withInitial(ArrayList::new);
    
    public static class MissingDataEntry {
        public final String module;
        public final String testCaseId;
        public final String fieldName;
        
        public MissingDataEntry(String module, String testCaseId, String fieldName) {
            this.module = module;
            this.testCaseId = testCaseId;
            this.fieldName = fieldName;
        }
    }
    
    public static void record(String module, String testCaseId, String fieldName) {
        missingData.get().add(new MissingDataEntry(module, testCaseId, fieldName));
    }
    
    public static boolean hasMissingData() {
        return !missingData.get().isEmpty();
    }
    
    public static String getSummary() {
        List<MissingDataEntry> entries = missingData.get();
        if (entries.isEmpty()) {
            return "";
        }
        
        // Group by test case
        var grouped = entries.stream()
            .collect(Collectors.groupingBy(
                e -> e.testCaseId,
                Collectors.mapping(e -> e.fieldName, Collectors.toList())
            ));
        
        StringBuilder summary = new StringBuilder("[WARN] Missing test data detected:\n");
        grouped.forEach((testCase, fields) -> {
            summary.append("  ").append(testCase).append(" -> ")
                   .append(String.join(", ", fields)).append("\n");
        });
        
        return summary.toString();
    }
    
    public static void clear() {
        missingData.get().clear();
    }
}
