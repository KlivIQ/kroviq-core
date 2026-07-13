package kroviq.reporting.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ExtentConfig {
    private static final Logger logger = LogManager.getLogger(ExtentConfig.class);
    private static boolean initialized = false;
    
    public static void initialize() {
        if (initialized) {
            System.out.println("[DEBUG] [DEBUG] ExtentConfig already initialized - skipping");
            return;
        }
        
        System.out.println("[DEBUG] [DEBUG] ExtentConfig.initialize() called");
        System.out.println("[DEBUG] Initializing ExtentReports configuration...");
        
        try {
            // CRITICAL: Set system properties FIRST (before file creation)
            setSystemProperties();
            
            // Delete stale extent.properties files
            deleteStalePropertiesFiles();
            
            // Create new extent.properties file
            createExtentPropertiesFile();
            
            initialized = true;
            System.out.println("[OK] ExtentReports configuration initialized");
            System.out.println("[DEBUG] [DEBUG] ExtentConfig initialization complete\n");
            
        } catch (Exception e) {
            logger.error("Failed to initialize ExtentReports configuration: {}", e.getMessage(), e);
            System.err.println("[FAIL] Failed to initialize ExtentReports configuration: " + e.getMessage());
        }
    }
    
    private static void deleteStalePropertiesFiles() {
        System.out.println("[DEBUG] Deleting stale extent.properties files...");
        
        // Delete from src/main/resources
        File srcPropertiesFile = new File("src/main/resources/extent.properties");
        if (srcPropertiesFile.exists()) {
            boolean deleted = srcPropertiesFile.delete();
            if (deleted) {
                System.out.println("[DEBUG] Deleted: src/main/resources/extent.properties");
            } else {
                System.out.println("[WARN] [DEBUG] Failed to delete: src/main/resources/extent.properties");
            }
        }
        
        // Delete from target/classes
        File targetPropertiesFile = new File("target/classes/extent.properties");
        if (targetPropertiesFile.exists()) {
            boolean deleted = targetPropertiesFile.delete();
            if (deleted) {
                System.out.println("[DEBUG] Deleted: target/classes/extent.properties");
            } else {
                System.out.println("[WARN] [DEBUG] Failed to delete: target/classes/extent.properties");
            }
        }
    }
    
    private static void createExtentPropertiesFile() throws IOException {
        System.out.println("[DEBUG] [DEBUG] Creating NEW extent.properties file...");
        
        File resourcesDir = new File("src/main/resources");
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs();
        }
        
        File propertiesFile = new File(resourcesDir, "extent.properties");
        
        String extentReportPath = ReportPaths.getExtentReportsPath();
        String screenshotsDir = ReportPaths.getExtentScreenshotsDir();
        
        System.out.println("[DEBUG] [DEBUG] Writing extent.properties:");
        System.out.println("[DEBUG] [DEBUG]   Report Path: " + new java.io.File(extentReportPath).getAbsolutePath());
        System.out.println("[DEBUG] [DEBUG]   Screenshots: " + new java.io.File(screenshotsDir).getAbsolutePath());
        System.out.println("[DEBUG] [DEBUG]   Timestamp: " + java.time.LocalDateTime.now());
        
        Properties props = new Properties();
        props.setProperty("extent.reporter.spark.start", "true");
        props.setProperty("extent.reporter.spark.out", extentReportPath);
        props.setProperty("extent.reporter.spark.screenshots.dir", screenshotsDir);
        props.setProperty("screenshot.dir", screenshotsDir);
        props.setProperty("screenshot.rel.path", "screenshots/");
        props.setProperty("systeminfo.os", System.getProperty("os.name", "Windows 11"));
        props.setProperty("systeminfo.user", System.getProperty("user.name", "Test User"));
        props.setProperty("systeminfo.build", "1.0");
        props.setProperty("systeminfo.AppName", kroviq.utils.RunManager.getProjectName());
        
        try (FileOutputStream fos = new FileOutputStream(propertiesFile)) {
            props.store(fos, "ExtentReports Configuration");
        }
        
        System.out.println("[OK] Created: " + propertiesFile.getAbsolutePath());
    }
    
    private static void setSystemProperties() {
        String extentReportPath = ReportPaths.getExtentReportsPath();
        String screenshotsDir = ReportPaths.getExtentScreenshotsDir();
        
        System.out.println("[DEBUG] [DEBUG] Setting Extent system properties (PRIMARY)...");
        
        // Set system properties that ExtentReports adapter might use
        System.setProperty("extent.reporter.spark.start", "true");
        System.setProperty("extent.reporter.spark.out", extentReportPath);
        System.setProperty("extent.reporter.spark.screenshots.dir", screenshotsDir);
        
        System.out.println("[DEBUG] [DEBUG] System properties set:");
        System.out.println("[DEBUG] [DEBUG]   extent.reporter.spark.out = " + System.getProperty("extent.reporter.spark.out"));
        System.out.println("[DEBUG] [DEBUG]   extent.reporter.spark.screenshots.dir = " + System.getProperty("extent.reporter.spark.screenshots.dir"));
    }
    
    public static String getExtentReportsPlugin() {
        return "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:";
    }
}