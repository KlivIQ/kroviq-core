package kroviq.reporting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * DEPRECATED: ExtentCucumberAdapter dependency removed.
 * This class is now a no-op stub to maintain backward compatibility.
 * Will be removed after confirming framework stability with custom HTML reports.
 */
public class ExtentStepLogger {
    private static final Logger logger = LogManager.getLogger(ExtentStepLogger.class);
    
    /**
     * Logs a step to Extent HTML report.
     * @param message Readable step message
     * @param status PASSED, FAILED, or INFO
     */
    public static void logStep(String message, String status) {
        // NO-OP: ExtentCucumberAdapter dependency removed
        // Steps are now logged to custom HTML report via TestRunReportManager
        logger.debug("ExtentStepLogger is deprecated - step logged to custom report: {} - {}", status, message);
    }
    
    /**
     * Logs a step with screenshot attachment.
     * @param message Readable step message
     * @param status PASSED or FAILED
     * @param screenshotPath Absolute path to screenshot file
     */
    public static void logStepWithScreenshot(String message, String status, String screenshotPath) {
        // NO-OP: ExtentCucumberAdapter dependency removed
        // Steps with screenshots are now logged to custom HTML report via TestRunReportManager
        logger.debug("ExtentStepLogger is deprecated - step with screenshot logged to custom report: {} - {}", status, message);
    }
}
