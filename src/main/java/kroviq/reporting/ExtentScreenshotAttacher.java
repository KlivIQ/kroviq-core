package kroviq.reporting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * DEPRECATED: ExtentCucumberAdapter dependency removed.
 * This class is now a no-op stub to maintain backward compatibility.
 * Will be removed after confirming framework stability with custom HTML reports.
 */
public class ExtentScreenshotAttacher {
    private static final Logger logger = LogManager.getLogger(ExtentScreenshotAttacher.class);
    
    public static void attachScreenshot(String absoluteScreenshotPath) {
        // NO-OP: ExtentCucumberAdapter dependency removed
        // Screenshots are now handled by custom HTML report generator
        logger.debug("ExtentScreenshotAttacher is deprecated - screenshot handled by custom report: {}", absoluteScreenshotPath);
    }
}
