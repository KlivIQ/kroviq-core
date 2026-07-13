package kroviq.reporting.config;

import kroviq.reporting.managers.TestRunReportManager;

public class ReportPaths {
    
    // Get dynamic run-specific base directory
    public static String getRunSpecificBaseDir() {
        try {
            return TestRunReportManager.getInstance().getRunDirectory();
        } catch (Exception e) {
            return "target"; // Fallback
        }
    }
    
    // ExtentReports HTML path
    public static String getExtentReportsPath() {
        try {
            String runId = TestRunReportManager.getInstance().getRunId();
            return getRunSpecificBaseDir() + "/" + runId + ".html";
        } catch (Exception e) {
            return getRunSpecificBaseDir() + "/extent-report.html";
        }
    }
    
    // Screenshot directory
    public static String getCucumberScreenshotsDir() {
        return getRunSpecificBaseDir() + "/screenshots";
    }
    
    public static String getExtentScreenshotsDir() {
        return getRunSpecificBaseDir() + "/screenshots";
    }
    
    // Excel report directory
    public static String getExcelReportsDir() {
        return getRunSpecificBaseDir();
    }
}