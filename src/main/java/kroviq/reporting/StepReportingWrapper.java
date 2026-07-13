package kroviq.reporting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import kroviq.reporting.StepHookManager.StepCategory;

public class StepReportingWrapper {
    private static final Logger logger = LogManager.getLogger(StepReportingWrapper.class);
    
    public static void executeStep(String stepDescription, Runnable stepAction) {
        StepHookManager.startStep(stepDescription);
        try {
            logger.info("Executing step: {}", stepDescription);
            stepAction.run();
            
            // Build readable message automatically
            String readableMessage = null;
            try {
                readableMessage = ReadableMessageBuilder.buildReadableMessage(
                    stepDescription, null, null, null
                );
            } catch (Exception e) {
                logger.debug("Message enrichment failed: {}", e.getMessage());
            }
            
            StepHookManager.setStepCategory(StepCategory.ACTION);
            StepHookManager.endStep(true, readableMessage);
            logger.info("Step passed: {}", stepDescription);
        } catch (Throwable e) {
            // Build readable message for failure
            String readableMessage = null;
            try {
                readableMessage = ReadableMessageBuilder.buildReadableMessage(
                    stepDescription, null, null, null
                );
            } catch (Exception ex) {
                logger.debug("Message enrichment failed: {}", ex.getMessage());
            }
            
            StepHookManager.setLastFailureException(e);
            StepHookManager.endStep(false, readableMessage);
            logger.error("Step failed: {} - {}", stepDescription, e.getMessage());
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            if (e instanceof Error) throw (Error) e;
            throw new RuntimeException(e);
        }
    }
    
    public static void executeStepWithContext(
        String stepDescription,
        String elementName,
        String pageName,
        String resolvedValue,
        Runnable stepAction
    ) {
        StepHookManager.startStep(stepDescription);
        try {
            logger.info("Executing step: {}", stepDescription);
            stepAction.run();
            
            // Build readable message with context
            String readableMessage = null;
            try {
                readableMessage = ReadableMessageBuilder.buildReadableMessage(
                    stepDescription, elementName, pageName, resolvedValue
                );
            } catch (Exception e) {
                logger.debug("Message enrichment failed: {}", e.getMessage());
            }
            
            StepHookManager.setStepCategory(StepCategory.ACTION);
            StepHookManager.endStep(true, readableMessage);
            logger.info("Step passed: {}", stepDescription);
        } catch (Throwable e) {
            // Build clean failure message (no technical details)
            String readableMessage = null;
            try {
                readableMessage = ReadableMessageBuilder.buildFailureMessage(
                    stepDescription, elementName, pageName, e instanceof Exception ? (Exception) e : new RuntimeException(e)
                );
            } catch (Exception ex) {
                logger.debug("Failure message building failed: {}", ex.getMessage());
                readableMessage = "Step execution failed";
            }
            
            StepHookManager.setLastFailureException(e);
            StepHookManager.setLastFailureException(e);
            StepHookManager.endStep(false, readableMessage);
            logger.error("Step failed: {} - {}", stepDescription, e.getMessage());
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            if (e instanceof Error) throw (Error) e;
            throw new RuntimeException(e);
        }
    }
    
    public static <T> T executeStepWithReturn(String stepDescription, java.util.function.Supplier<T> stepAction) {
        StepHookManager.startStep(stepDescription);
        try {
            logger.info("Executing step: {}", stepDescription);
            T result = stepAction.get();
            
            // Build readable message automatically
            String readableMessage = null;
            try {
                readableMessage = ReadableMessageBuilder.buildReadableMessage(
                    stepDescription, null, null, null
                );
            } catch (Exception e) {
                logger.debug("Message enrichment failed: {}", e.getMessage());
            }
            
            StepHookManager.setStepCategory(StepCategory.ACTION);
            StepHookManager.endStep(true, readableMessage);
            logger.info("Step passed: {}", stepDescription);
            return result;
        } catch (Throwable e) {
            // Build readable message for failure
            String readableMessage = null;
            try {
                readableMessage = ReadableMessageBuilder.buildReadableMessage(
                    stepDescription, null, null, null
                );
            } catch (Exception ex) {
                logger.debug("Message enrichment failed: {}", ex.getMessage());
            }
            
            StepHookManager.setLastFailureException(e);
            StepHookManager.endStep(false, readableMessage);
            logger.error("Step failed: {} - {}", stepDescription, e.getMessage());
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            if (e instanceof Error) throw (Error) e;
            throw new RuntimeException(e);
        }
    }
    
    public static void recordStepInfo(String stepDescription) {
        logger.info("Step info: {}", stepDescription);
    }
    
    public static void recordManualStep(String message, String status) {
        String normalizedStatus = status.toUpperCase();
        
        switch (normalizedStatus) {
            case "PASS":
            case "PASSED":
                StepResultTracker.stepPassed(message);
                logger.info("Manual step PASSED: {}", message);
                break;
            case "FAIL":
            case "FAILED":
                String screenshotPath = ScreenshotManager.captureScreenshot(
                    StepResultTracker.getCurrentTestCase(), message, "FAILED");
                StepResultTracker.stepFailed(message, screenshotPath);
                logger.error("Manual step FAILED: {}", message);
                break;
            case "INFO":
            default:
                StepResultTracker.stepActionPassed(message, null);
                logger.info("Manual step INFO: {}", message);
                break;
        }
    }
}