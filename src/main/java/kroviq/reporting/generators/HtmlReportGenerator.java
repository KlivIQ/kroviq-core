package kroviq.reporting.generators;

import kroviq.reporting.TestRunSummary;
import kroviq.reporting.TestCaseResult;
import kroviq.reporting.StepResult;
import kroviq.ai.rca.RCAReportRenderer;
import kroviq.ai.rca.RCAResult;
import kroviq.ai.defect.DefectDraft;
import kroviq.ai.defect.DefectTemplateRenderer;
import kroviq.reporting.managers.TestRunReportManager;
import kroviq.utils.ExecutionContext;
import kroviq.utils.RunManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.List;

/**
 * Custom HTML Report Generator
 * Generates a lightweight, table-based HTML report from TestRunSummary data.
 * This report coexists with ExtentReports HTML and Excel reports.
 */
public class HtmlReportGenerator {
    private static final Logger logger = LogManager.getLogger(HtmlReportGenerator.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm:ss a");
    
    /**
     * Main entry point - generates complete HTML report from TestRunSummary
     * 
     * @param summary TestRunSummary containing all test execution data
     * @param outputPath Full path where HTML file should be saved
     */
    public static void generateHtmlReport(TestRunSummary summary, String outputPath) {
        try {
            logger.info("Generating custom HTML report: {}", outputPath);
            System.out.println("[FILE] Generating custom HTML report...");
            
            if (summary == null) {
                logger.error("Cannot generate HTML report: TestRunSummary is null");
                System.err.println("[FAIL] Cannot generate HTML report: TestRunSummary is null");
                return;
            }
            
            String htmlContent = generateCompleteHtml(summary);
            
            try (FileWriter writer = new FileWriter(outputPath)) {
                writer.write(htmlContent);
            }
            
            logger.info("Custom HTML report generated successfully: {}", outputPath);
            System.out.println("[OK] Custom HTML report generated: " + outputPath);
            
        } catch (IOException e) {
            logger.error("Error writing HTML report to file: {}", e.getMessage(), e);
            System.err.println("[FAIL] Error writing HTML report: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error generating HTML report: {}", e.getMessage(), e);
            System.err.println("[FAIL] Error generating HTML report: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Generate complete HTML document
     */
    private static String generateCompleteHtml(TestRunSummary summary) {
        StringBuilder html = new StringBuilder();
        
        html.append(generateHtmlHeader(summary));
        html.append("<body>\n");
        html.append(generateHeaderSection(summary));
        html.append(generateSummarySection(summary));
        html.append(generateTestCasesSection(summary));
        html.append(generateHtmlFooter());
        html.append(generateJavaScript());
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }
    
    /**
     * Generate HTML header with metadata and inline CSS
     */
    private static String generateHtmlHeader(TestRunSummary summary) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>Test Report - ").append(escapeHtml(summary.getRunId())).append("</title>\n");
        html.append(generateInlineCss());
        html.append("</head>\n");
        
        return html.toString();
    }
    
    /**
     * Generate inline CSS styles
     */
    private static String generateInlineCss() {
        return "<style>\n" +
                "* { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f5f5; color: #333; }\n" +
                ".container { max-width: 1400px; margin: 0 auto; padding: 20px; }\n" +
                ".page-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 8px; margin-bottom: 30px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }\n" +
                ".header-logo { max-height: 90px; margin-bottom: 15px; display: block; image-rendering: -webkit-optimize-contrast; image-rendering: crisp-edges; }\n" +
                ".page-header h1 { font-size: 32px; margin-bottom: 10px; }\n" +
                ".page-header p { font-size: 14px; opacity: 0.9; margin: 5px 0; }\n" +
                ".summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px; }\n" +
                ".stat-card { background: white; padding: 25px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); text-align: center; transition: transform 0.2s; }\n" +
                ".stat-card:hover { transform: translateY(-5px); box-shadow: 0 4px 8px rgba(0,0,0,0.15); }\n" +
                ".stat-card h3 { font-size: 14px; color: #666; margin-bottom: 10px; text-transform: uppercase; letter-spacing: 1px; }\n" +
                ".stat-card .stat-value { font-size: 36px; font-weight: bold; margin: 10px 0; }\n" +
                ".stat-card.total .stat-value { color: #667eea; }\n" +
                ".stat-card.passed .stat-value { color: #28a745; }\n" +
                ".stat-card.failed .stat-value { color: #dc3545; }\n" +
                ".stat-card.rate .stat-value { color: #17a2b8; }\n" +
                ".test-cases { background: white; border-radius: 8px; padding: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
                ".test-cases h2 { font-size: 24px; margin-bottom: 20px; color: #333; border-bottom: 2px solid #667eea; padding-bottom: 10px; }\n" +
                ".test-case-card { border: 1px solid #e0e0e0; border-radius: 6px; margin-bottom: 15px; overflow: hidden; transition: box-shadow 0.2s; }\n" +
                ".test-case-card:hover { box-shadow: 0 4px 8px rgba(0,0,0,0.1); }\n" +
                ".test-case-header { padding: 15px 20px; background: #f8f9fa; cursor: pointer; display: flex; align-items: center; justify-content: space-between; transition: background 0.2s; }\n" +
                ".test-case-header:hover { background: #e9ecef; }\n" +
                ".test-case-header .left { display: flex; align-items: center; gap: 15px; flex: 1; }\n" +
                ".test-case-header .right { display: flex; align-items: center; gap: 15px; }\n" +
                ".test-case-title { font-size: 16px; font-weight: 600; color: #333; }\n" +
                ".test-case-body { padding: 20px; background: white; display: none; }\n" +
                ".test-case-body.expanded { display: block; }\n" +
                ".step-table { width: 100%; border-collapse: collapse; margin-top: 10px; }\n" +
                ".step-table thead { background: #34495e; color: white; }\n" +
                ".step-table th { padding: 12px; text-align: left; font-weight: 600; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px; }\n" +
                ".step-table td { padding: 12px; border-bottom: 1px solid #e0e0e0; vertical-align: top; }\n" +
                ".step-table tbody tr:hover { background: #f8f9fa; }\n" +
                ".step-table tbody tr:last-child td { border-bottom: none; }\n" +
                ".status-col { width: 100px; }\n" +
                ".timestamp-col { width: 120px; color: #666; font-size: 13px; }\n" +
                ".details-col { }\n" +
                ".badge { display: inline-block; padding: 6px 12px; border-radius: 4px; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }\n" +
                ".badge-pass { background: #28a745; color: white; }\n" +
                ".badge-passed { background: #28a745; color: white; }\n" +
                ".badge-fail { background: #dc3545; color: white; }\n" +
                ".badge-failed { background: #dc3545; color: white; }\n" +
                ".badge-info { background: #17a2b8; color: white; }\n" +
                ".badge-skip { background: #ffc107; color: #333; }\n" +
                ".badge-skipped { background: #ffc107; color: #333; }\n" +
                ".screenshot-thumb { max-width: 200px; margin-top: 10px; border: 2px solid #e0e0e0; border-radius: 4px; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }\n" +
                ".screenshot-thumb:hover { transform: scale(1.05); box-shadow: 0 4px 8px rgba(0,0,0,0.2); }\n" +
                ".duration { color: #666; font-size: 13px; }\n" +
                ".footer { text-align: center; padding: 30px 20px; margin-top: 40px; background: white; border-radius: 8px; color: #666; font-size: 13px; }\n" +
                ".toggle-icon { font-size: 18px; transition: transform 0.3s; }\n" +
                ".toggle-icon.expanded { transform: rotate(90deg); }\n" +
                ".step-description { line-height: 1.6; }\n" +
                RCAReportRenderer.getRcaCssStyles() +
                DefectTemplateRenderer.getDefectCssStyles() +
                "@media print { .test-case-body { display: block !important; } }\n" +
                "</style>\n";
    }
    
    /**
     * Generate page header section
     */
    private static String generateHeaderSection(TestRunSummary summary) {
        StringBuilder html = new StringBuilder();
        
        String projectName = RunManager.getProjectName();
        String reportTitle = "Test Execution Report - " + projectName;
        
        html.append("<div class=\"container\">\n");
        html.append("<div class=\"page-header\">\n");
        html.append("<img src=\"../assets/kroviq_logo.png\" alt=\"Kroviq\" class=\"header-logo\">\n");
        html.append("<h1>").append(escapeHtml(reportTitle)).append("</h1>\n");
        html.append("<p><strong>Run ID:</strong> ").append(escapeHtml(summary.getRunId())).append("</p>\n");
        html.append("<p><strong>Start Time:</strong> ").append(formatDateTime(summary.getStartTime())).append("</p>\n");
        html.append("<p><strong>End Time:</strong> ").append(formatDateTime(summary.getEndTime())).append("</p>\n");
        html.append("<p><strong>Duration:</strong> ").append(formatDuration(summary.getStartTime(), summary.getEndTime())).append("</p>\n");
        html.append("<p><strong>Executed By:</strong> ").append(escapeHtml(ExecutionContext.getExecutionUser())).append("</p>\n");
        html.append("<p><strong>Machine:</strong> ").append(escapeHtml(ExecutionContext.getMachineName())).append("</p>\n");
        html.append("<p><strong>Execution ID:</strong> ").append(escapeHtml(ExecutionContext.getExecutionId())).append("</p>\n");
        html.append("</div>\n");
        
        return html.toString();
    }
    
    /**
     * Generate summary dashboard section
     */
    private static String generateSummarySection(TestRunSummary summary) {
        StringBuilder html = new StringBuilder();
        
        int total = summary.getTotalTests();
        int passed = summary.getPassedTests();
        int failed = summary.getFailedTests();
        double passRate = total > 0 ? (passed * 100.0 / total) : 0.0;
        
        html.append("<div class=\"summary\">\n");
        
        // Total Tests Card
        html.append("<div class=\"stat-card total\">\n");
        html.append("<h3>Total Tests</h3>\n");
        html.append("<div class=\"stat-value\">").append(total).append("</div>\n");
        html.append("</div>\n");
        
        // Passed Tests Card
        html.append("<div class=\"stat-card passed\">\n");
        html.append("<h3>Passed</h3>\n");
        html.append("<div class=\"stat-value\">").append(passed).append("</div>\n");
        html.append("</div>\n");
        
        // Failed Tests Card
        html.append("<div class=\"stat-card failed\">\n");
        html.append("<h3>Failed</h3>\n");
        html.append("<div class=\"stat-value\">").append(failed).append("</div>\n");
        html.append("</div>\n");
        
        // Pass Rate Card
        html.append("<div class=\"stat-card rate\">\n");
        html.append("<h3>Pass Rate</h3>\n");
        html.append("<div class=\"stat-value\">").append(String.format("%.1f%%", passRate)).append("</div>\n");
        html.append("</div>\n");
        
        html.append("</div>\n");
        
        return html.toString();
    }
    
    /**
     * Generate test cases section with step tables
     */
    private static String generateTestCasesSection(TestRunSummary summary) {
        StringBuilder html = new StringBuilder();
        
        html.append("<div class=\"test-cases\">\n");
        html.append("<h2>Test Cases (").append(summary.getTestCases().size()).append(")</h2>\n");
        
        int index = 1;
        for (TestCaseResult testCase : summary.getTestCases()) {
            html.append(generateTestCaseCard(testCase, index));
            index++;
        }
        
        html.append("</div>\n");
        
        return html.toString();
    }
    
    /**
     * Generate individual test case card with step table
     */
    private static String generateTestCaseCard(TestCaseResult testCase, int index) {
        StringBuilder html = new StringBuilder();
        
        String testId = "test-" + index;
        String status = testCase.getStatus();
        String badgeClass = getStatusBadgeClass(status);
        
        html.append("<div class=\"test-case-card\">\n");
        
        // Test Case Header
        html.append("<div class=\"test-case-header\" onclick=\"toggleTestCase('").append(testId).append("')\">\n");
        html.append("<div class=\"left\">\n");
        html.append("<span class=\"toggle-icon\" id=\"icon-").append(testId).append("\">&#9654;</span>\n");
        html.append("<span class=\"badge ").append(badgeClass).append("\">").append(escapeHtml(status)).append("</span>\n");
        html.append("<span class=\"test-case-title\">").append(escapeHtml(testCase.getTestCaseId()));
        if (testCase.getScenarioName() != null && !testCase.getScenarioName().isEmpty()) {
            html.append(" - ").append(escapeHtml(testCase.getScenarioName()));
        }
        html.append("</span>\n");
        html.append("</div>\n");
        html.append("<div class=\"right\">\n");
        html.append("<span class=\"duration\">Duration: ").append(formatDuration(testCase.getStartTime(), testCase.getEndTime())).append("</span>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        
        // Test Case Body (Step Table + RCA Card for failures)
        html.append("<div class=\"test-case-body\" id=\"").append(testId).append("\">\n");
        
        // Render RCA card for failed test cases
        if ("FAILED".equalsIgnoreCase(status)) {
            html.append(getRcaCardForTestCase(testCase));
            html.append(getDefectCardForTestCase(testCase));
        }
        
        html.append(generateStepTable(testCase.getSteps()));
        html.append("</div>\n");
        
        html.append("</div>\n");
        
        return html.toString();
    }
    
    private static String getRcaCardForTestCase(TestCaseResult testCase) {
        try {
            // Use stored runtime RCA result — no re-analysis
            RCAResult storedResult = TestRunReportManager.getInstance().getRCAResult(testCase.getTestCaseId());
            if (storedResult != null) {
                return RCAReportRenderer.renderHtmlCard(storedResult);
            }
        } catch (Exception e) {
            // Silently fall through
        }
        return "";
    }

    private static String getDefectCardForTestCase(TestCaseResult testCase) {
        try {
            DefectDraft storedDraft = TestRunReportManager.getInstance().getDefectDraft(testCase.getTestCaseId());
            if (storedDraft != null) {
                return DefectTemplateRenderer.renderHtmlCard(storedDraft);
            }
        } catch (Exception e) {
            // Silently fall through
        }
        return "";
    }
    
    /**
     * Generate step table for a test case
     */
    private static String generateStepTable(List<StepResult> steps) {
        StringBuilder html = new StringBuilder();
        
        if (steps == null || steps.isEmpty()) {
            html.append("<p style=\"padding: 20px; color: #666;\">No steps recorded for this test case.</p>\n");
            return html.toString();
        }
        
        html.append("<table class=\"step-table\">\n");
        html.append("<thead>\n");
        html.append("<tr>\n");
        html.append("<th class=\"status-col\">Status</th>\n");
        html.append("<th class=\"timestamp-col\">Timestamp</th>\n");
        html.append("<th class=\"details-col\">Step Details</th>\n");
        html.append("</tr>\n");
        html.append("</thead>\n");
        html.append("<tbody>\n");
        
        int stepNumber = 1;
        for (StepResult step : steps) {
            html.append(generateStepRow(step, stepNumber));
            stepNumber++;
        }
        
        html.append("</tbody>\n");
        html.append("</table>\n");
        
        return html.toString();
    }
    
    /**
     * Generate individual step row
     */
    private static String generateStepRow(StepResult step, int stepNumber) {
        StringBuilder html = new StringBuilder();
        
        String status = step.getStatus() != null ? step.getStatus() : "UNKNOWN";
        String badgeClass = getStatusBadgeClass(status);
        
        html.append("<tr>\n");
        
        // Status column
        html.append("<td class=\"status-col\">");
        html.append("<span class=\"badge ").append(badgeClass).append("\">").append(escapeHtml(status)).append("</span>");
        html.append("</td>\n");
        
        // Timestamp column
        html.append("<td class=\"timestamp-col\">");
        if (step.getTimestamp() != null) {
            html.append(formatTime(step.getTimestamp()));
        } else {
            html.append("-");
        }
        html.append("</td>\n");
        
        // Details column
        html.append("<td class=\"details-col\">");
        html.append("<div class=\"step-description\">");
        html.append("<strong>Step ").append(stepNumber).append(":</strong> ");
        
        // Use readable message if available, otherwise use step description
        String description = step.getReadableMessage() != null && !step.getReadableMessage().isEmpty() 
            ? step.getReadableMessage() 
            : step.getStepDescription();
        
        html.append(escapeHtml(description));
        html.append("</div>");
        
        // Add screenshot if available
        if (step.getScreenshotPath() != null && !step.getScreenshotPath().isEmpty()) {
            html.append(generateScreenshotHtml(step.getScreenshotPath()));
        }
        
        html.append("</td>\n");
        
        html.append("</tr>\n");
        
        return html.toString();
    }
    
    /**
     * Generate screenshot thumbnail HTML
     */
    private static String generateScreenshotHtml(String screenshotPath) {
        if (screenshotPath == null || screenshotPath.isEmpty()) {
            return "";
        }
        
        String relativePath = getRelativeScreenshotPath(screenshotPath);
        if (relativePath == null || relativePath.isEmpty()) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<br>\n");
        html.append("<img class=\"screenshot-thumb\" ");
        html.append("src=\"").append(relativePath).append("\" ");
        html.append("onclick=\"openScreenshot('").append(relativePath).append("')\" ");
        html.append("alt=\"Screenshot\" ");
        html.append("title=\"Click to view full size\">\n");
        
        return html.toString();
    }
    
    /**
     * Generate JavaScript for interactivity
     */
    private static String generateJavaScript() {
        return "<script>\n" +
                "function toggleTestCase(testId) {\n" +
                "    var body = document.getElementById(testId);\n" +
                "    var icon = document.getElementById('icon-' + testId);\n" +
                "    if (body.classList.contains('expanded')) {\n" +
                "        body.classList.remove('expanded');\n" +
                "        icon.classList.remove('expanded');\n" +
                "    } else {\n" +
                "        body.classList.add('expanded');\n" +
                "        icon.classList.add('expanded');\n" +
                "    }\n" +
                "}\n" +
                "function openScreenshot(src) {\n" +
                "    window.open(src, '_blank');\n" +
                "}\n" +
                "</script>\n";
    }
    
    /**
     * Generate HTML footer
     */
    private static String generateHtmlFooter() {
        StringBuilder html = new StringBuilder();
        
        html.append("<div class=\"footer\">\n");
        html.append("<p>Generated by Kroviq | ");
        html.append(formatDateTime(LocalDateTime.now()));
        html.append("</p>\n");
        html.append("</div>\n");
        html.append("</div>\n"); // Close container
        
        return html.toString();
    }
    
    /**
     * Get CSS badge class for status
     */
    private static String getStatusBadgeClass(String status) {
        if (status == null) {
            return "badge-info";
        }
        
        String statusLower = status.toLowerCase();
        if (statusLower.contains("pass")) {
            return "badge-pass";
        } else if (statusLower.contains("fail")) {
            return "badge-fail";
        } else if (statusLower.contains("skip")) {
            return "badge-skip";
        } else {
            return "badge-info";
        }
    }
    
    /**
     * Format timestamp for display (HH:mm:ss)
     */
    private static String formatTime(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return timestamp.format(TIME_FORMATTER);
    }
    
    /**
     * Format datetime for display (MMM dd, yyyy hh:mm:ss a)
     */
    private static String formatDateTime(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return timestamp.format(DATETIME_FORMATTER);
    }
    
    /**
     * Calculate and format duration between two timestamps
     */
    private static String formatDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return "-";
        }
        
        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        long millis = duration.toMillisPart();
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%02d:%02d", minutes, seconds);
        } else {
            return String.format("%02d.%03ds", seconds, millis);
        }
    }
    
    /**
     * Escape HTML special characters
     */
    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    /**
     * Convert screenshot path to relative path for HTML
     */
    private static String getRelativeScreenshotPath(String screenshotPath) {
        if (screenshotPath == null || screenshotPath.isEmpty()) {
            return null;
        }
        
        // Handle both absolute and relative paths
        if (screenshotPath.contains("screenshots")) {
            int index = screenshotPath.indexOf("screenshots");
            String relativePath = screenshotPath.substring(index);
            // Normalize path separators for web
            return relativePath.replace("\\", "/");
        }
        
        // If already relative, just normalize slashes
        return screenshotPath.replace("\\", "/");
    }
}
