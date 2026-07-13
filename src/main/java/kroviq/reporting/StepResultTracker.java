package kroviq.reporting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import kroviq.reporting.managers.TestRunReportManager;
import java.util.function.Supplier;

public class StepResultTracker {
    private static final Logger logger = LogManager.getLogger(StepResultTracker.class);
    private static final ThreadLocal<String> currentTestCaseId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentStepDescription = new ThreadLocal<>();
    
    public static void setCurrentTestCase(String testCaseId) {
        currentTestCaseId.set(testCaseId);
    }
    
    public static String getCurrentTestCase() {
        return currentTestCaseId.get();
    }
    
    public static void startStep(String stepDescription) {
        currentStepDescription.set(stepDescription);
        logger.debug("Starting step: {}", stepDescription);
    }
    
    public static void stepPassed(String stepDescription) {
        stepPassed(stepDescription, null);
    }
    
    public static void stepPassed(String stepDescription, String readableMessage) {
        String screenshotPath = null;
        if (ReportConfig.getInstance().isCaptureScreenshotsOnPass()) {
            screenshotPath = ScreenshotManager.captureScreenshot(getCurrentTestCase(), stepDescription, "PASSED");
        }
        recordStepResult(stepDescription, readableMessage, "PASSED", screenshotPath);
    }
    
    public static void stepFailed(String stepDescription, String screenshotPath) {
        stepFailed(stepDescription, null, screenshotPath);
    }
    
    public static void stepFailed(String stepDescription, String readableMessage, String screenshotPath) {
        recordStepResult(stepDescription, readableMessage, "FAILED", screenshotPath);
        
        // Attach screenshot to Extent report
        ExtentScreenshotAttacher.attachScreenshot(screenshotPath);
    }
    
    public static void stepInfo(String stepDescription) {
        recordStepResult(stepDescription, null, "INFO", null);
    }
    
    /**
     * Records a successful action step as INFO.
     * Bypasses the existing INFO filter in recordStepResult by using a dedicated code path.
     * Validation steps continue to use stepPassed() -> "PASSED".
     */
    public static void stepActionPassed(String stepDescription, String readableMessage) {
        String displayMessage = readableMessage != null ? readableMessage : stepDescription;
        ExtentStepLogger.logStep(displayMessage, "INFO");
        
        String testCaseId = getCurrentTestCase();
        if (testCaseId != null) {
            TestRunReportManager.getInstance().addStepResult(testCaseId, stepDescription, readableMessage, "INFO", null);
        }
        logger.debug("Action step recorded as INFO: {}", stepDescription);
    }
    
    private static void recordStepResult(String stepDescription, String readableMessage, String status, String screenshotPath) {
        // Defensive filter: INFO entries should not reach test run report
        // StepHookManager.endStep() is the single source of truth for PASSED/FAILED
        if ("INFO".equals(status)) {
            logger.debug("INFO entry filtered (not recorded to report): {}", stepDescription);
            return;
        }
        
        // Use readable message if available, otherwise use technical description
        String displayMessage = readableMessage != null ? readableMessage : stepDescription;
        
        // Log to Extent HTML report (fail-safe, won't break execution)
        if (screenshotPath != null && !screenshotPath.isEmpty()) {
            ExtentStepLogger.logStepWithScreenshot(displayMessage, status, screenshotPath);
        } else {
            ExtentStepLogger.logStep(displayMessage, status);
        }
        
        // Log to test run report (existing flow)
        String testCaseId = getCurrentTestCase();
        if (testCaseId != null) {
            TestRunReportManager.getInstance().addStepResult(testCaseId, stepDescription, readableMessage, status, screenshotPath);
        }
        logger.debug("Step result recorded: {} - {}", stepDescription, status);
    }
    
    public static void wrapStepExecution(String stepDescription, Runnable stepAction) {
        startStep(stepDescription);
        try {
            stepAction.run();
            stepPassed(stepDescription);
        } catch (Exception e) {
            String screenshotPath = null;
            if (ReportConfig.getInstance().isCaptureScreenshotsOnFailure()) {
                screenshotPath = ScreenshotManager.captureScreenshotOnFailure(getCurrentTestCase(), stepDescription);
            }
            stepFailed(stepDescription, screenshotPath);
            throw e;
        }
    }
    
    public static <T> T wrapStepExecutionWithReturn(String stepDescription, java.util.function.Supplier<T> stepAction) {
        startStep(stepDescription);
        try {
            T result = stepAction.get();
            stepPassed(stepDescription);
            return result;
        } catch (Exception e) {
            String screenshotPath = null;
            if (ReportConfig.getInstance().isCaptureScreenshotsOnFailure()) {
                screenshotPath = ScreenshotManager.captureScreenshotOnFailure(getCurrentTestCase(), stepDescription);
            }
            stepFailed(stepDescription, screenshotPath);
            throw e;
        }
    }
    
    public static void recordStepWithScreenshot(String stepDescription, String status) {
        String screenshotPath = null;
        if (("FAILED".equals(status) && ReportConfig.getInstance().isCaptureScreenshotsOnFailure()) ||
            ("PASSED".equals(status) && ReportConfig.getInstance().isCaptureScreenshotsOnPass())) {
            screenshotPath = ScreenshotManager.captureScreenshot(getCurrentTestCase(), stepDescription, status);
        }
        recordStepResult(stepDescription, null, status, screenshotPath);
    }
    
    public static void cleanup() {
        currentTestCaseId.remove();
        currentStepDescription.remove();
    }
}