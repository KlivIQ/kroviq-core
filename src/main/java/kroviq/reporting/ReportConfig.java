package kroviq.reporting;

import kroviq.utils.RunManager;

public class ReportConfig {
    private static final ReportConfig instance = new ReportConfig();
    
    private boolean captureScreenshotsOnFailure = true;
    private boolean captureScreenshotsOnPass = false;
    private boolean includeStepDetails = true;
    private int screenshotCleanupDays = 30;
    private String reportTitle;
    private boolean generateConsolidatedReport = true;
    private boolean fallbackMode = false;
    private boolean rcaEnabled = true;
    
    private ReportConfig() {
        this.reportTitle = "NAF Pro Test Execution - " + RunManager.getProjectName();
        this.captureScreenshotsOnPass = RunManager.isCaptureScreenshotsOnPass();
        this.captureScreenshotsOnFailure = RunManager.isCaptureScreenshotsOnFailure();
        this.rcaEnabled = RunManager.isRcaEnabled();
    }
    
    public static ReportConfig getInstance() {
        return instance;
    }
    
    // Getters and Setters
    public boolean isCaptureScreenshotsOnFailure() { return captureScreenshotsOnFailure; }
    public void setCaptureScreenshotsOnFailure(boolean captureScreenshotsOnFailure) { 
        this.captureScreenshotsOnFailure = captureScreenshotsOnFailure; 
    }
    
    public boolean isCaptureScreenshotsOnPass() { return captureScreenshotsOnPass; }
    public void setCaptureScreenshotsOnPass(boolean captureScreenshotsOnPass) { 
        this.captureScreenshotsOnPass = captureScreenshotsOnPass; 
    }
    
    public boolean isIncludeStepDetails() { return includeStepDetails; }
    public void setIncludeStepDetails(boolean includeStepDetails) { 
        this.includeStepDetails = includeStepDetails; 
    }
    
    public int getScreenshotCleanupDays() { return screenshotCleanupDays; }
    public void setScreenshotCleanupDays(int screenshotCleanupDays) { 
        this.screenshotCleanupDays = screenshotCleanupDays; 
    }
    
    public String getReportTitle() { return reportTitle; }
    public void setReportTitle(String reportTitle) { 
        this.reportTitle = reportTitle; 
    }
    
    public boolean isGenerateConsolidatedReport() { return generateConsolidatedReport; }
    public void setGenerateConsolidatedReport(boolean generateConsolidatedReport) { 
        this.generateConsolidatedReport = generateConsolidatedReport; 
    }
    
    public boolean isFallbackMode() { return fallbackMode; }
    public void setFallbackMode(boolean fallbackMode) { 
        this.fallbackMode = fallbackMode;
    }
    
    public boolean isRcaEnabled() { return rcaEnabled; }
    public void setRcaEnabled(boolean rcaEnabled) { this.rcaEnabled = rcaEnabled; }
}