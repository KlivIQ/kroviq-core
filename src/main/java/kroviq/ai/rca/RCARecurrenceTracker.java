package kroviq.ai.rca;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RCARecurrenceTracker {
    private static final RCARecurrenceTracker instance = new RCARecurrenceTracker();

    // Key: testCaseId + "|" + category, Value: occurrence count
    private final Map<String, AtomicInteger> failureHistory = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> executionCount = new ConcurrentHashMap<>();

    private RCARecurrenceTracker() {}

    public static RCARecurrenceTracker getInstance() { return instance; }

    public void recordExecution(String testCaseId) {
        if (testCaseId == null || testCaseId.isEmpty()) return;
        executionCount.computeIfAbsent(testCaseId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void recordFailure(String testCaseId, RootCauseCategory category) {
        if (testCaseId == null || testCaseId.isEmpty()) return;
        String key = testCaseId + "|" + category.name();
        failureHistory.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public boolean isSeenBefore(String testCaseId, RootCauseCategory category) {
        String key = testCaseId + "|" + category.name();
        AtomicInteger count = failureHistory.get(key);
        return count != null && count.get() > 1;
    }

    public String getFailureFrequency(String testCaseId, RootCauseCategory category) {
        String key = testCaseId + "|" + category.name();
        AtomicInteger failures = failureHistory.get(key);
        AtomicInteger runs = executionCount.get(testCaseId);

        if (failures == null || runs == null) return "N/A";
        return failures.get() + "/" + runs.get() + " runs";
    }

    public boolean isRecurringPattern(String testCaseId, RootCauseCategory category) {
        String key = testCaseId + "|" + category.name();
        AtomicInteger failures = failureHistory.get(key);
        AtomicInteger runs = executionCount.get(testCaseId);

        if (failures == null || runs == null || runs.get() < 3) return false;
        double failRate = (double) failures.get() / runs.get();
        return failRate >= 0.5;
    }

    public void reset() {
        failureHistory.clear();
        executionCount.clear();
    }
}
