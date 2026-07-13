package kroviq.reporting.managers;

import kroviq.reporting.config.ReportPaths;
import kroviq.reporting.config.ExtentConfig;
import kroviq.reporting.generators.ExcelReportGenerator;
import kroviq.reporting.managers.TestRunReportManager;
import kroviq.reporting.TestRunSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReportManager {
    private static final Logger logger = LogManager.getLogger(ReportManager.class);
    private static ReportManager instance;
    
    private ReportManager() {}
    
    public static synchronized ReportManager getInstance() {
        if (instance == null) {
            instance = new ReportManager();
        }
        return instance;
    }
    
    public static synchronized void reset() {
        instance = null;
    }
    
    public void initializeReports() {
        System.out.println("\n[DEBUG] INITIALIZING REPORTING SYSTEM...");
        System.out.println("[DEBUG] [DEBUG] ReportManager.initializeReports() called");
        
        try {
            // Get run directory from TestRunReportManager (should already exist)
            String runDir = ReportPaths.getRunSpecificBaseDir();
            System.out.println("[DEBUG] [DEBUG] Using Run Directory: " + new java.io.File(runDir).getAbsolutePath());
            
            // Create all report directories
            createReportDirectories();
            
            // ExtentReports configuration disabled - using custom HTML report instead
            // System.out.println("[DEBUG] [DEBUG] Initializing ExtentConfig...");
            // ExtentConfig.initialize();
            // System.out.println("[DEBUG] [DEBUG] ExtentConfig initialized");
            System.out.println("[DEBUG] [DEBUG] ExtentConfig initialization skipped (using custom HTML report)");
            
            System.out.println("[OK] Reporting system initialized successfully\n");
            
        } catch (Exception e) {
            logger.error("Failed to initialize reporting system: {}", e.getMessage(), e);
            System.err.println("[FAIL] Failed to initialize reporting system: " + e.getMessage());
        }
    }
    
    public List<String> getCucumberPluginConfiguration() {
        // ExtentReports Cucumber Adapter disabled - using custom HTML report instead
        List<String> plugins = new ArrayList<>();
        
        System.out.println("[INFO] Cucumber plugins configured: " + plugins.size() + " plugins (ExtentReports disabled)");
        return plugins;
    }
    
    public void generateFinalReports() {
        System.out.println("\n[STATS] GENERATING FINAL REPORTS...");
        
        try {
            // Wait for reports to be generated
            waitForReportsGeneration();
            
            // Generate Excel report
            generateExcelReport();
            
            // Display report links
            displayReportLinks();
            
            System.out.println("[OK] All reports generated successfully");
            
        } catch (Exception e) {
            logger.error("Error generating final reports: {}", e.getMessage(), e);
            System.err.println("[FAIL] Error generating final reports: " + e.getMessage());
        }
    }
    
    private void createReportDirectories() {
        System.out.println("[DIR] Creating report directories...");
        
        String baseDir = ReportPaths.getRunSpecificBaseDir();
        System.out.println("[DIR] Base directory: " + baseDir);
        
        String[] directories = {
            ReportPaths.getCucumberScreenshotsDir()
        };
        
        for (String dir : directories) {
            File directory = new File(dir);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (created) {
                    System.out.println("[OK] Created: " + dir);
                } else {
                    System.err.println("[FAIL] Failed to create: " + dir);
                }
            }
        }
    }
    
    private void waitForReportsGeneration() {
        System.out.println("[INFO] Waiting for report generation...");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("[OK] Report generation wait completed");
    }
    
    private void generateExcelReport() {
        try {
            System.out.println("[STATS] Generating Excel report...");
            
            TestRunSummary summary = TestRunReportManager.getInstance().getRunSummary();
            if (summary != null) {
                ExcelReportGenerator.generateConsolidatedReport(summary);
            } else {
                System.err.println("[FAIL] Cannot generate Excel report: TestRunSummary is null");
            }
            
        } catch (Exception e) {
            logger.error("Error generating Excel report: {}", e.getMessage(), e);
            System.err.println("[FAIL] Error generating Excel report: " + e.getMessage());
        }
    }
    
    private void displayReportLinks() {
        System.out.println("\n=== REPORTS DIRECTORY ===");
        System.out.println("[DIR] All reports location: " + ReportPaths.getRunSpecificBaseDir());
        System.out.println("\n=== GENERATED REPORTS ===");
        
        File extentReport = new File(ReportPaths.getExtentReportsPath());
        if (extentReport.exists()) {
            System.out.println("[WEB] HTML Report: file:///" + extentReport.getAbsolutePath());
        }
        
        try {
            String excelDir = ReportPaths.getExcelReportsDir();
            String runId = TestRunReportManager.getInstance().getRunId();
            File excelReport = new File(excelDir, runId + ".xlsx");
            if (excelReport.exists()) {
                System.out.println("[STATS] Excel Report: file:///" + excelReport.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("[WARN] Excel report location unavailable");
        }
        
        System.out.println("==========================\n");
    }
}