package kroviq.reporting;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

public class TestRunSummary {
    private final String runId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final int totalTests;
    private final int passedTests;
    private final int failedTests;
    private final List<TestCaseResult> testCases;
    private final long executionTimeSeconds;
    
    public TestRunSummary(String runId, LocalDateTime startTime, LocalDateTime endTime,
                         int totalTests, int passedTests, int failedTests,
                         List<TestCaseResult> testCases) {
        this.runId = runId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalTests = totalTests;
        this.passedTests = passedTests;
        this.failedTests = failedTests;
        this.testCases = testCases != null ? testCases : new ArrayList<>();
        this.executionTimeSeconds = Duration.between(startTime, endTime).getSeconds();
    }
    
    // Getters
    public String getRunId() { return runId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public int getTotalTests() { return totalTests; }
    public int getPassedTests() { return passedTests; }
    public int getFailedTests() { return failedTests; }
    public List<TestCaseResult> getTestCases() { return testCases; }
    public long getExecutionTimeSeconds() { return executionTimeSeconds; }
    public double getPassPercentage() { 
        return totalTests > 0 ? (double) passedTests / totalTests * 100 : 0; 
    }
}