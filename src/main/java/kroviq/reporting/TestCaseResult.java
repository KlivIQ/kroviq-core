package kroviq.reporting;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

public class TestCaseResult {
    private final String testCaseId;
    private final String scenarioName;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final String status;
    private final List<StepResult> steps;
    private final long executionTimeSeconds;
    
    public TestCaseResult(String testCaseId, String scenarioName, LocalDateTime startTime,
                         LocalDateTime endTime, String status, List<StepResult> steps) {
        this.testCaseId = testCaseId;
        this.scenarioName = scenarioName;
        this.startTime = startTime;
        this.endTime = endTime != null ? endTime : LocalDateTime.now();
        this.status = status;
        this.steps = steps != null ? steps : new ArrayList<>();
        this.executionTimeSeconds = Duration.between(startTime, this.endTime).getSeconds();
    }
    
    // Getters
    public String getTestCaseId() { return testCaseId; }
    public String getScenarioName() { return scenarioName; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getStatus() { return status; }
    public List<StepResult> getSteps() { return steps; }
    public long getExecutionTimeSeconds() { return executionTimeSeconds; }
    public int getTotalSteps() { return steps.size(); }
    public long getPassedSteps() { 
        return steps.stream().mapToLong(s -> "PASSED".equals(s.getStatus()) ? 1 : 0).sum(); 
    }
    public long getFailedSteps() { 
        return steps.stream().mapToLong(s -> "FAILED".equals(s.getStatus()) ? 1 : 0).sum(); 
    }
}