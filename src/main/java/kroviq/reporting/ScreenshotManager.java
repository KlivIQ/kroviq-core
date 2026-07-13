package kroviq.reporting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import kroviq.wrapper.GenericWrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScreenshotManager {
    private static final Logger logger = LogManager.getLogger(ScreenshotManager.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("HHmmss");
    
    public static String captureScreenshotOnFailure(String testCaseId, String stepDescription) {
        try {
            WebDriver driver = GenericWrapper.getDriver();
            if (driver == null) {
                logger.warn("WebDriver is null, cannot capture screenshot");
                return null;
            }
            
            String timestamp = LocalDateTime.now().format(TIMESTAMP);
            String sanitizedStep = sanitizeFileName(stepDescription);
            String fileName = String.format("%s_Step_%s_%s_FAIL.png", testCaseId, sanitizedStep, timestamp);
            
            String screenshotDir = kroviq.reporting.config.ReportPaths.getCucumberScreenshotsDir();
            File screenshotFile = new File(screenshotDir, fileName);
            
            // Ensure screenshots directory exists
            screenshotFile.getParentFile().mkdirs();
            
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            try (FileOutputStream fos = new FileOutputStream(screenshotFile)) {
                fos.write(screenshot);
            }
            
            // Return absolute path for Extent
            String absolutePath = screenshotFile.getAbsolutePath();
            logger.info("Screenshot captured: {}", absolutePath);
            return absolutePath;
            
        } catch (Exception e) {
            logger.error("Failed to capture screenshot: {}", e.getMessage());
            return null;
        }
    }
    
    public static String captureScreenshot(String testCaseId, String stepDescription, String status) {
        try {
            WebDriver driver = GenericWrapper.getDriver();
            if (driver == null) {
                return null;
            }
            
            String timestamp = LocalDateTime.now().format(TIMESTAMP);
            String sanitizedStep = sanitizeFileName(stepDescription);
            String fileName = String.format("%s_Step_%s_%s_%s.png", testCaseId, sanitizedStep, timestamp, status);
            
            String screenshotDir = kroviq.reporting.config.ReportPaths.getCucumberScreenshotsDir();
            File screenshotFile = new File(screenshotDir, fileName);
            
            // Ensure screenshots directory exists
            screenshotFile.getParentFile().mkdirs();
            
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            try (FileOutputStream fos = new FileOutputStream(screenshotFile)) {
                fos.write(screenshot);
            }
            
            // Return absolute path for Extent
            String absolutePath = screenshotFile.getAbsolutePath();
            logger.debug("Screenshot captured: {}", absolutePath);
            return absolutePath;
            
        } catch (Exception e) {
            logger.error("Failed to capture screenshot: {}", e.getMessage());
            return null;
        }
    }
    
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) return "Unknown";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_").substring(0, Math.min(fileName.length(), 50));
    }
    
    public static String captureStepScreenshot(String testCaseId, String stepName, String status) {
        try {
            WebDriver driver = GenericWrapper.getDriver();
            if (driver == null) {
                logger.warn("WebDriver is null, cannot capture step screenshot");
                return null;
            }
            
            String timestamp = LocalDateTime.now().format(TIMESTAMP);
            String sanitizedStep = sanitizeFileName(stepName);
            String fileName = String.format("%s_Step_%s_%s_%s.png", testCaseId, sanitizedStep, timestamp, status);
            
            String screenshotDir = kroviq.reporting.config.ReportPaths.getCucumberScreenshotsDir();
            File screenshotFile = new File(screenshotDir, fileName);
            
            // Ensure screenshots directory exists
            screenshotFile.getParentFile().mkdirs();
            
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            try (FileOutputStream fos = new FileOutputStream(screenshotFile)) {
                fos.write(screenshot);
            }
            
            // Return absolute path for Extent
            String absolutePath = screenshotFile.getAbsolutePath();
            logger.info("Step screenshot captured: {}", absolutePath);
            return absolutePath;
            
        } catch (Exception e) {
            logger.error("Failed to capture step screenshot: {}", e.getMessage());
            return null;
        }
    }
    
    public static void captureStepScreenshotIfEnabled(String testCaseId, String stepName, String status) {
        if (ReportConfig.getInstance().isCaptureScreenshotsOnFailure() && "FAILED".equals(status)) {
            captureStepScreenshot(testCaseId, stepName, status);
        }
    }
    
    public static void cleanupOldScreenshots(int daysToKeep) {
        try {
            File reportsDir = new File("Reports");
            if (!reportsDir.exists()) return;
            
            long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
            
            File[] runDirs = reportsDir.listFiles(File::isDirectory);
            if (runDirs != null) {
                for (File runDir : runDirs) {
                    if (runDir.lastModified() < cutoffTime) {
                        deleteDirectory(runDir);
                        logger.info("Cleaned up old run directory: {}", runDir.getName());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup old screenshots: {}", e.getMessage());
        }
    }
    
    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}