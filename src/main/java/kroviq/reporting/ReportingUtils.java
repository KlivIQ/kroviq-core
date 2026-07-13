package kroviq.reporting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReportingUtils {
    private static final Logger logger = LogManager.getLogger(ReportingUtils.class);
    
    public static void logTestResult(String testCaseId, String status, String message) {
        logger.info("Test: {} | Status: {} | Message: {}", testCaseId, status, message);
    }
    
    public static void logTestStep(String stepDescription) {
        logger.info("Step: {}", stepDescription);
    }
}