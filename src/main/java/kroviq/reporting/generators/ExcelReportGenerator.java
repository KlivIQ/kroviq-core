package kroviq.reporting.generators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.util.IOUtils;
import org.apache.poi.ss.util.CellRangeAddress;
import kroviq.reporting.*;
import kroviq.reporting.managers.TestRunReportManager;
import kroviq.utils.ExecutionContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExcelReportGenerator {
    private static final Logger logger = LogManager.getLogger(ExcelReportGenerator.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Legacy method for backward compatibility
    public static void generateTestReport(String testCaseId, String scenarioName, String startTime, String endTime, String status) {
        // This method is maintained for backward compatibility but actual reporting is handled by TestRunReportManager
        logger.debug("Legacy report method called for: {} - {}", testCaseId, status);
    }
    
    public static void generateConsolidatedReport(TestRunSummary summary) {
        System.out.println("\n[ITER] STARTING EXCEL REPORT GENERATION...");
        
        // Validate input data
        if (summary == null) {
            System.err.println("[FAIL] CRITICAL: TestRunSummary is null - cannot generate report");
            logger.error("TestRunSummary is null");
            return;
        }
        
        System.out.println("[STATS] Summary Data: " + summary.getTotalTests() + " tests, " + 
                          summary.getTestCases().size() + " test cases");
        
        try {
            String runDirectory;
            String runId;
            
            // Get directory and run ID with fallback
            try {
                runDirectory = kroviq.reporting.config.ReportPaths.getExcelReportsDir();
                runId = TestRunReportManager.getInstance().getRunId();
            } catch (Exception e) {
                // Fallback for test scenarios or when TestRunReportManager is not initialized
                runDirectory = System.getProperty("test.run.directory", "Reports/TestRun_" + System.currentTimeMillis() + "/excel");
                runId = System.getProperty("test.run.id", summary.getRunId());
                System.out.println("[WARN]  Using fallback directory and run ID");
            }
            
            System.out.println("[DIR] Run Directory: " + runDirectory);
            System.out.println("[INFO] Run ID: " + runId);
            
            // Validate directory exists and create if needed
            File dirFile = new File(runDirectory);
            if (!dirFile.exists()) {
                System.out.println("[DIR] Creating directory: " + runDirectory);
                boolean created = dirFile.mkdirs();
                if (!created && !dirFile.exists()) {
                    throw new RuntimeException("Failed to create directory: " + runDirectory);
                }
            }
            
            // Verify directory is writable
            if (!dirFile.canWrite()) {
                throw new RuntimeException("Directory is not writable: " + runDirectory);
            }
            
            // Use new filename format: TestRun_YYYY-MM-DD_HHMM.xlsx
            String reportFileName;
            try {
                reportFileName = runId.replace("_", "_").replace("TestRun_", "TestRun_") + ".xlsx";
                if (reportFileName.contains("_")) {
                    String[] parts = reportFileName.split("_");
                    if (parts.length >= 3) {
                        String datePart = parts[1];
                        String timePart = parts[2].replace(".xlsx", "");
                        if (datePart.length() >= 8 && timePart.length() >= 4) {
                            reportFileName = String.format("TestRun_%s-%s-%s_%s.xlsx", 
                                datePart.substring(0, 4), 
                                datePart.substring(4, 6), 
                                datePart.substring(6, 8),
                                timePart.substring(0, 4));
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback to simple filename
                reportFileName = runId + ".xlsx";
                System.out.println("[WARN]  Using simple filename format: " + reportFileName);
            }
            
            File reportFile = new File(runDirectory, reportFileName);
            System.out.println("[FILE] Report File: " + reportFile.getAbsolutePath());
            
            // Check if file already exists and handle conflicts
            if (reportFile.exists()) {
                System.out.println("[WARN]  File exists, will overwrite: " + reportFile.getName());
                // Try to delete existing file to ensure we can write
                try {
                    boolean deleted = reportFile.delete();
                    if (!deleted) {
                        System.out.println("[WARN]  Could not delete existing file, will attempt overwrite");
                    }
                } catch (Exception e) {
                    System.out.println("[WARN]  Error deleting existing file: " + e.getMessage());
                }
            }
            
            System.out.println("[DEBUG] Creating Excel workbook...");
            Workbook workbook = new XSSFWorkbook();
            
            // Create sheets with progress tracking
            System.out.println("[INFO] Creating Summary sheet...");
            createSummarySheet(workbook, summary);
            
            System.out.println("[INFO] Creating Test Cases sheet...");
            createTestCasesSheet(workbook, summary);
            
            System.out.println("[INFO] Creating Step Details sheet...");
            createStepDetailsSheet(workbook, summary);
            
            System.out.println("[INFO] Creating Screenshots sheet...");
            createScreenshotsSheet(workbook, summary);
            
            // Save the workbook with retry mechanism
            System.out.println("[INFO] Saving Excel file...");
            boolean saved = false;
            int attempts = 0;
            Exception lastException = null;
            
            while (!saved && attempts < 3) {
                attempts++;
                try {
                    System.out.println("[INFO] Save attempt " + attempts + "/3");
                    try (FileOutputStream fos = new FileOutputStream(reportFile)) {
                        workbook.write(fos);
                        fos.flush();
                    }
                    saved = true;
                } catch (Exception e) {
                    lastException = e;
                    System.err.println("[WARN]  Save attempt " + attempts + " failed: " + e.getMessage());
                    if (attempts < 3) {
                        Thread.sleep(1000); // Wait 1 second before retry
                    }
                }
            }
            
            workbook.close();
            
            if (!saved) {
                System.err.println("[FAIL] All save attempts failed. Last error: " + 
                    (lastException != null ? lastException.getMessage() : "Unknown"));
                throw new RuntimeException("Failed to save Excel file after 3 attempts", lastException);
            }
            
            // Verify file was created successfully
            if (reportFile.exists() && reportFile.length() > 0) {
                logger.info("Consolidated report generated: {}", reportFile.getAbsolutePath());
                System.out.println("[OK] EXCEL REPORT SUCCESSFULLY CREATED!");
                System.out.println("[FILE] File: " + reportFile.getAbsolutePath());
                System.out.println("[UP] Size: " + reportFile.length() + " bytes");
                System.out.println("[INFO] Generated at: " + java.time.LocalDateTime.now());
                
                // Additional validation
                if (reportFile.canRead()) {
                    System.out.println("[OK] File is readable");
                } else {
                    System.out.println("[WARN]  File exists but may not be readable");
                }
            } else {
                String errorMsg = "Excel file validation failed: ";
                if (!reportFile.exists()) {
                    errorMsg += "File does not exist";
                } else if (reportFile.length() == 0) {
                    errorMsg += "File is empty (0 bytes)";
                }
                throw new RuntimeException(errorMsg);
            }
            
        } catch (Exception e) {
            logger.error("Failed to generate consolidated report: {}", e.getMessage(), e);
            System.err.println("\n[FAIL] EXCEL REPORT GENERATION FAILED!");
            System.err.println("[CHECK] Error: " + e.getMessage());
            System.err.println("[FAIL] Error Type: " + e.getClass().getSimpleName());
            
            // Print more detailed error information
            if (e.getCause() != null) {
                System.err.println("[CHECK] Root Cause: " + e.getCause().getMessage());
            }
            
            // Print stack trace for debugging
            System.err.println("\n[NOTE] Stack Trace:");
            e.printStackTrace();
            
            // Try to generate a simple backup report
            generateBackupReport(summary);
        }
        
        System.out.println("[DONE] Excel report generation process completed.\n");
    }
    
    private static void generateBackupReport(TestRunSummary summary) {
        try {
            System.out.println("[ITER] Attempting to generate backup CSV report...");
            
            String runDirectory;
            String runId;
            
            try {
                runDirectory = kroviq.reporting.config.ReportPaths.getExcelReportsDir();
                runId = TestRunReportManager.getInstance().getRunId();
            } catch (Exception e) {
                runDirectory = "Reports/excel";
                runId = summary.getRunId();
                System.out.println("[WARN]  Using fallback for backup report");
            }
            
            // Ensure directory exists
            File dir = new File(runDirectory);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            File backupFile = new File(runDirectory, runId + "_backup.csv");
            
            try (java.io.PrintWriter writer = new java.io.PrintWriter(backupFile)) {
                writer.println("Test Case ID,Scenario Name,Status,Start Time,End Time,Steps Count");
                for (TestCaseResult testCase : summary.getTestCases()) {
                    writer.printf("%s,\"%s\",%s,%s,%s,%d%n",
                        testCase.getTestCaseId(),
                        testCase.getScenarioName().replace("\"", "\\\""),
                        testCase.getStatus(),
                        testCase.getStartTime(),
                        testCase.getEndTime(),
                        testCase.getTotalSteps());
                }
            }
            
            if (backupFile.exists() && backupFile.length() > 0) {
                System.out.println("[OK] Backup CSV report created: " + backupFile.getAbsolutePath());
                System.out.println("[UP] Backup file size: " + backupFile.length() + " bytes");
            } else {
                System.err.println("[FAIL] Backup CSV file was not created properly");
            }
            
        } catch (Exception e) {
            System.err.println("[FAIL] Backup report generation also failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createSummarySheet(Workbook workbook, TestRunSummary summary) {
        Sheet sheet = workbook.createSheet("Summary");
        int rowNum = 0;
        
        // Add logo at the top
        try {
            InputStream logoStream = ExcelReportGenerator.class.getResourceAsStream("/assets/kroviq_logo.png");
            if (logoStream != null) {
                byte[] logoBytes = IOUtils.toByteArray(logoStream);
                int pictureIdx = workbook.addPicture(logoBytes, Workbook.PICTURE_TYPE_PNG);
                logoStream.close();
                
                CreationHelper helper = workbook.getCreationHelper();
                Drawing<?> drawing = sheet.createDrawingPatriarch();
                ClientAnchor anchor = helper.createClientAnchor();
                
                // Position logo in cells A1:B3
                anchor.setCol1(0);
                anchor.setRow1(0);
                anchor.setCol2(2);
                anchor.setRow2(3);
                
                Picture picture = drawing.createPicture(anchor, pictureIdx);
                
                // Adjust row heights for logo visibility
                for (int i = 0; i < 3; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) row = sheet.createRow(i);
                    row.setHeightInPoints(25);
                }
                
                rowNum = 4; // Start content after logo
            }
        } catch (Exception e) {
            logger.warn("Could not add logo to Excel report: {}", e.getMessage());
            System.out.println("[WARN]  Logo not added to Excel (file may be missing): " + e.getMessage());
        }
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue(ReportConfig.getInstance().getReportTitle());
        
        rowNum++; // Empty row
        
        // Summary data
        createSummaryRow(sheet, rowNum++, "Run ID", summary.getRunId());
        createSummaryRow(sheet, rowNum++, "Start Time", summary.getStartTime().format(DATE_FORMAT));
        createSummaryRow(sheet, rowNum++, "End Time", summary.getEndTime().format(DATE_FORMAT));
        createSummaryRow(sheet, rowNum++, "Execution Time (seconds)", String.valueOf(summary.getExecutionTimeSeconds()));
        createSummaryRow(sheet, rowNum++, "Total Tests", String.valueOf(summary.getTotalTests()));
        createSummaryRow(sheet, rowNum++, "Passed Tests", String.valueOf(summary.getPassedTests()));
        createSummaryRow(sheet, rowNum++, "Failed Tests", String.valueOf(summary.getFailedTests()));
        createSummaryRow(sheet, rowNum++, "Pass Percentage", String.format("%.2f%%", summary.getPassPercentage()));
        createSummaryRow(sheet, rowNum++, "Executed By", ExecutionContext.getExecutionUser());
        createSummaryRow(sheet, rowNum++, "Machine", ExecutionContext.getMachineName());
        createSummaryRow(sheet, rowNum++, "Execution ID", ExecutionContext.getExecutionId());
        
        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }
    
    private static void createTestCasesSheet(Workbook workbook, TestRunSummary summary) {
        Sheet sheet = workbook.createSheet("Test Cases");
        
        // Header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Test Case ID");
        headerRow.createCell(1).setCellValue("Scenario Name");
        headerRow.createCell(2).setCellValue("Start Time");
        headerRow.createCell(3).setCellValue("End Time");
        headerRow.createCell(4).setCellValue("Status");
        headerRow.createCell(5).setCellValue("Duration (sec)");
        headerRow.createCell(6).setCellValue("Total Steps");
        headerRow.createCell(7).setCellValue("Passed Steps");
        headerRow.createCell(8).setCellValue("Failed Steps");
        
        int rowNum = 1;
        // Test cases are already sorted chronologically in summary
        for (TestCaseResult testCase : summary.getTestCases()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(testCase.getTestCaseId());
            row.createCell(1).setCellValue(testCase.getScenarioName());
            row.createCell(2).setCellValue(testCase.getStartTime().format(TIME_FORMAT));
            row.createCell(3).setCellValue(testCase.getEndTime().format(TIME_FORMAT));
            row.createCell(4).setCellValue(testCase.getStatus());
            row.createCell(5).setCellValue(testCase.getExecutionTimeSeconds());
            row.createCell(6).setCellValue(testCase.getTotalSteps());
            row.createCell(7).setCellValue(testCase.getPassedSteps());
            row.createCell(8).setCellValue(testCase.getFailedSteps());
        }
        
        // Auto-size columns
        for (int i = 0; i < 9; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private static void createStepDetailsSheet(Workbook workbook, TestRunSummary summary) {
        Sheet sheet = workbook.createSheet("Step Details");
        
        // Header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Test Case ID");
        headerRow.createCell(1).setCellValue("Step Description");
        headerRow.createCell(2).setCellValue("Status");
        headerRow.createCell(3).setCellValue("Timestamp");
        headerRow.createCell(4).setCellValue("Screenshot");
        headerRow.createCell(5).setCellValue("Error Message");
        
        int rowNum = 1;
        // Process test cases in chronological order (already sorted in summary)
        for (TestCaseResult testCase : summary.getTestCases()) {
            // Process steps in chronological order (already sorted in summary)
            for (StepResult step : testCase.getSteps()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(testCase.getTestCaseId());
                
                // Use readable message if available, otherwise use technical description
                String displayMessage = step.getDisplayMessage();
                row.createCell(1).setCellValue(displayMessage);
                
                row.createCell(2).setCellValue(step.getStatus());
                row.createCell(3).setCellValue(step.getTimestamp().format(TIME_FORMAT));
                
                // Create clickable hyperlink for screenshot
                Cell screenshotCell = row.createCell(4);
                if (step.hasScreenshot()) {
                    String screenshotPath = step.getScreenshotPath();
                    if (screenshotPath != null && !screenshotPath.isEmpty()) {
                        // Create relative path for portability
                        String relativePath = "screenshots/" + new File(screenshotPath).getName();
                        
                        CreationHelper createHelper = workbook.getCreationHelper();
                        Hyperlink hyperlink = createHelper.createHyperlink(HyperlinkType.FILE);
                        hyperlink.setAddress(relativePath);
                        
                        screenshotCell.setCellValue("View Screenshot");
                        screenshotCell.setHyperlink(hyperlink);
                        
                        // Style the hyperlink
                        CellStyle hyperlinkStyle = workbook.createCellStyle();
                        Font hyperlinkFont = workbook.createFont();
                        hyperlinkFont.setUnderline(Font.U_SINGLE);
                        hyperlinkFont.setColor(IndexedColors.BLUE.getIndex());
                        hyperlinkStyle.setFont(hyperlinkFont);
                        screenshotCell.setCellStyle(hyperlinkStyle);
                    } else {
                        screenshotCell.setCellValue("N/A");
                    }
                } else {
                    screenshotCell.setCellValue("");
                }
                
                row.createCell(5).setCellValue(step.getErrorMessage() != null ? step.getErrorMessage() : "");
            }
        }
        
        // Auto-size columns
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private static void createScreenshotsSheet(Workbook workbook, TestRunSummary summary) {
        Sheet sheet = workbook.createSheet("Screenshots");
        
        // Header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Test Case ID");
        headerRow.createCell(1).setCellValue("Step Description");
        headerRow.createCell(2).setCellValue("Screenshot Path");
        headerRow.createCell(3).setCellValue("Timestamp");
        
        int rowNum = 1;
        for (TestCaseResult testCase : summary.getTestCases()) {
            for (StepResult step : testCase.getSteps()) {
                if (step.hasScreenshot()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(testCase.getTestCaseId());
                    row.createCell(1).setCellValue(step.getStepDescription());
                    row.createCell(2).setCellValue(step.getScreenshotPath());
                    row.createCell(3).setCellValue(step.getTimestamp().format(TIME_FORMAT));
                }
            }
        }
        
        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private static void createSummaryRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }
}