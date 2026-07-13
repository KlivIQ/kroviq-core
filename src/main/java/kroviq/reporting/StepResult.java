package kroviq.reporting;

import java.time.LocalDateTime;

public class StepResult {
    private final String stepDescription;
    private final String readableMessage;
    private final String status;
    private final LocalDateTime timestamp;
    private final String screenshotPath;
    private final String errorMessage;
    
    public StepResult(String stepDescription, String status, LocalDateTime timestamp, 
                     String screenshotPath, String errorMessage) {
        this(stepDescription, null, status, timestamp, screenshotPath, errorMessage);
    }
    
    public StepResult(String stepDescription, String readableMessage, String status, 
                     LocalDateTime timestamp, String screenshotPath, String errorMessage) {
        this.stepDescription = stepDescription;
        this.readableMessage = readableMessage;
        this.status = status;
        this.timestamp = timestamp;
        this.screenshotPath = screenshotPath;
        this.errorMessage = errorMessage;
    }
    
    public StepResult(String stepDescription, String status, String screenshotPath) {
        this(stepDescription, null, status, LocalDateTime.now(), screenshotPath, null);
    }
    
    public StepResult(String stepDescription, String status) {
        this(stepDescription, null, status, LocalDateTime.now(), null, null);
    }
    
    // Getters
    public String getStepDescription() { return stepDescription; }
    public String getReadableMessage() { return readableMessage; }
    public String getDisplayMessage() { 
        return readableMessage != null ? readableMessage : stepDescription; 
    }
    public String getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getScreenshotPath() { return screenshotPath; }
    public String getErrorMessage() { return errorMessage; }
    public boolean hasScreenshot() { return screenshotPath != null && !screenshotPath.isEmpty(); }
}