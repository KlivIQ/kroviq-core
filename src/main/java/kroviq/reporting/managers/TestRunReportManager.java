package kroviq.reporting.managers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import kroviq.ai.rca.RCAResult;
import kroviq.ai.defect.DefectDraft;
import kroviq.reporting.*;
import kroviq.reporting.generators.ExcelReportGenerator;
import kroviq.reporting.generators.HtmlReportGenerator;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

public class TestRunReportManager {
    private static final Logger logger = LogManager.getLogger(TestRunReportManager.class);
    private static TestRunReportManager instance;
    private static final DateTimeFormatter RUN_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final String runId;
    private final String runDirectory;
    private final LocalDateTime runStartTime;
    private final ConcurrentHashMap<String, TestCaseData> testCases;
    private final ConcurrentLinkedQueue<StepData> allSteps;
    private final ConcurrentHashMap<String, RCAResult> rcaResults;
    private final ConcurrentHashMap<String, DefectDraft> defectDrafts;
    private final AtomicInteger totalTests;
    private final AtomicInteger passedTests;
    private final AtomicInteger failedTests;
    
    private TestRunReportManager() {
        this.runStartTime = LocalDateTime.now();
        this.runId = "TestRun_" + runStartTime.format(RUN_TIMESTAMP);
        this.runDirectory = "Reports/Run_" + runStartTime.format(RUN_TIMESTAMP);
        this.testCases = new ConcurrentHashMap<>();
        this.allSteps = new ConcurrentLinkedQueue<>();
        this.rcaResults = new ConcurrentHashMap<>();
        this.defectDrafts = new ConcurrentHashMap<>();
        this.totalTests = new AtomicInteger(0);
        this.passedTests = new AtomicInteger(0);
        this.failedTests = new AtomicInteger(0);
        
        System.out.println("\n[DEBUG] [DEBUG] TestRunReportManager constructor called");
        System.out.println("[DEBUG] [DEBUG] Creating Run folder: " + runDirectory);
        
        // Create run directory and subdirectories
        new File(runDirectory).mkdirs();
        new File(runDirectory + "/screenshots").mkdirs();
        
        // Ensure shared assets folder exists and copy logo
        ensureSharedAssets();
        
        System.out.println("[DEBUG] [DEBUG] Run folder created successfully");
        System.out.println("[DEBUG] [DEBUG] Absolute path: " + new File(runDirectory).getAbsolutePath());
        
        logger.info("Test run started: {}", runId);
        System.out.println("\n[START] TEST RUN STARTED");
        System.out.println("[INFO] Run ID: " + runId);
        System.out.println("[DIR] Report Directory: " + new File(runDirectory).getAbsolutePath());
        System.out.println("===================\n");
    }
    
    public static synchronized TestRunReportManager getInstance() {
        if (instance == null) {
            instance = new TestRunReportManager();
        }
        return instance;
    }
    
    public static synchronized void reset() {
        instance = null;
    }
    
    public void startTestCase(String testCaseId, String scenarioName) {
        try {
            TestCaseData testCase = new TestCaseData(testCaseId, scenarioName);
            testCases.put(testCaseId, testCase);
            totalTests.incrementAndGet();
            logger.debug("Started test case: {}", testCaseId);
            System.out.println("[DONE] Started test case: " + testCaseId);
        } catch (Exception e) {
            logger.error("Error starting test case {}: {}", testCaseId, e.getMessage());
            System.err.println("[WARN]  Failed to start test case " + testCaseId + ": " + e.getMessage());
        }
    }
    
    public void endTestCase(String testCaseId, String status) {
        try {
            TestCaseData testCase = testCases.get(testCaseId);
            if (testCase != null) {
                testCase.endTime = LocalDateTime.now();
                testCase.status = status;
                
                if ("PASSED".equals(status)) {
                    passedTests.incrementAndGet();
                } else {
                    failedTests.incrementAndGet();
                }
                logger.debug("Ended test case: {} - {}", testCaseId, status);
                System.out.println("[DONE] Ended test case: " + testCaseId + " - " + status);
            } else {
                logger.warn("Test case {} not found when ending", testCaseId);
                System.err.println("[WARN]  Test case not found when ending: " + testCaseId);
            }
        } catch (Exception e) {
            logger.error("Error ending test case {}: {}", testCaseId, e.getMessage());
            System.err.println("[WARN]  Failed to end test case " + testCaseId + ": " + e.getMessage());
        }
    }
    
    public void addStepResult(String testCaseId, String stepDescription, String status, String screenshotPath) {
        addStepResult(testCaseId, stepDescription, null, status, screenshotPath);
    }
    
    public void addStepResult(String testCaseId, String stepDescription, String readableMessage, String status, String screenshotPath) {
        try {
            StepData step = new StepData(testCaseId, stepDescription, readableMessage, status, screenshotPath);
            allSteps.add(step);
            
            TestCaseData testCase = testCases.get(testCaseId);
            if (testCase != null) {
                testCase.steps.add(step);
            } else {
                logger.warn("Test case {} not found when adding step result", testCaseId);
            }
            
            logger.debug("Added step result: {} - {}", stepDescription, status);
        } catch (Exception e) {
            logger.error("Error adding step result for {}: {}", testCaseId, e.getMessage());
            System.err.println("[WARN]  Failed to add step result for " + testCaseId + ": " + e.getMessage());
        }
    }
    
    public String getRunDirectory() {
        return runDirectory;
    }
    
    public String getRunId() {
        return runId;
    }
    
    /**
     * Ensure shared assets folder exists in Reports directory and copy logo if needed
     */
    private void ensureSharedAssets() {
        try {
            File assetsDir = new File("Reports/assets");
            File logoFile = new File(assetsDir, "kroviq_logo.png");
            
            // Create assets directory if it doesn't exist
            if (!assetsDir.exists()) {
                assetsDir.mkdirs();
                logger.info("Created shared assets directory: {}", assetsDir.getAbsolutePath());
            }
            
            // Copy logo from resources if it doesn't exist in Reports/assets
            if (!logoFile.exists()) {
                InputStream logoStream = getClass().getResourceAsStream("/assets/kroviq_logo.png");
                if (logoStream != null) {
                    Files.copy(logoStream, logoFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logoStream.close();
                    logger.info("Copied logo to shared assets: {}", logoFile.getAbsolutePath());
                    System.out.println("[OK] Logo copied to Reports/assets/");
                } else {
                    logger.warn("Logo resource not found in classpath: /assets/kroviq_logo.png");
                }
            }
        } catch (Exception e) {
            logger.warn("Could not setup shared assets: {}", e.getMessage());
            System.out.println("[WARN]  Could not copy logo to Reports/assets/: " + e.getMessage());
        }
    }
    
    public TestRunSummary getRunSummary() {
        try {
            List<TestCaseResult> testCaseResults = new ArrayList<>();
            
            System.out.println("[FILE] Building test run summary...");
            System.out.println("   [STATS] Collected test cases: " + testCases.size());
            System.out.println("   [STATS] Total steps recorded: " + allSteps.size());
            
            // Sort test cases by start time to maintain chronological order
            testCases.values().stream()
                .sorted((tc1, tc2) -> tc1.startTime.compareTo(tc2.startTime))
                .forEach(testCaseData -> {
                    List<StepResult> stepResults = new ArrayList<>();
                    
                    // Sort steps by timestamp to maintain chronological order
                    testCaseData.steps.stream()
                        .sorted((s1, s2) -> s1.timestamp.compareTo(s2.timestamp))
                        .forEach(stepData -> {
                            stepResults.add(new StepResult(
                                stepData.stepDescription,
                                stepData.readableMessage,
                                stepData.status,
                                stepData.timestamp,
                                stepData.screenshotPath,
                                null
                            ));
                        });
                    
                    testCaseResults.add(new TestCaseResult(
                        testCaseData.testCaseId,
                        testCaseData.scenarioName,
                        testCaseData.startTime,
                        testCaseData.endTime != null ? testCaseData.endTime : LocalDateTime.now(),
                        testCaseData.status != null ? testCaseData.status : "UNKNOWN",
                        stepResults
                    ));
                });
            
            TestRunSummary summary = new TestRunSummary(
                runId,
                runStartTime,
                LocalDateTime.now(),
                totalTests.get(),
                passedTests.get(),
                failedTests.get(),
                testCaseResults
            );
            
            System.out.println("[OK] Test run summary built successfully");
            return summary;
            
        } catch (Exception e) {
            logger.error("Error building test run summary: {}", e.getMessage(), e);
            System.err.println("[FAIL] Error building test run summary: " + e.getMessage());
            
            // Return minimal summary to prevent complete failure
            return new TestRunSummary(
                runId != null ? runId : "UNKNOWN_RUN",
                runStartTime != null ? runStartTime : LocalDateTime.now(),
                LocalDateTime.now(),
                totalTests.get(),
                passedTests.get(),
                failedTests.get(),
                new ArrayList<>()
            );
        }
    }
    
    public void generateFinalReport() {
        System.out.println("\n[UP] GENERATING FINAL CONSOLIDATED REPORT...");
        
        try {
            // Check if report generation is enabled
            if (!ReportConfig.getInstance().isGenerateConsolidatedReport()) {
                System.out.println("[WARN]  Consolidated report generation is disabled in config");
                return;
            }
            
            // Get and validate summary data
            TestRunSummary summary = getRunSummary();
            
            if (summary == null) {
                System.err.println("[FAIL] Cannot generate report: TestRunSummary is null");
                return;
            }
            
            // Log summary statistics
            System.out.println("[STATS] Test Run Statistics:");
            System.out.println("   [INFO] Run ID: " + summary.getRunId());
            System.out.println("   [INFO] Start: " + summary.getStartTime());
            System.out.println("   [INFO] End: " + summary.getEndTime());
            System.out.println("   [STATS] Total Tests: " + summary.getTotalTests());
            System.out.println("   [OK] Passed: " + summary.getPassedTests());
            System.out.println("   [FAIL] Failed: " + summary.getFailedTests());
            System.out.println("   [FILE] Test Cases: " + summary.getTestCases().size());
            
            // Validate we have test data
            if (summary.getTotalTests() == 0) {
                System.out.println("[WARN]  No test data found - generating empty report");
            }
            
            // Generate the Excel report in unified directory
            ExcelReportGenerator.generateConsolidatedReport(summary);
            
            // Generate custom HTML report
            String customHtmlPath = runDirectory + "/" + runId + "_Custom.html";
            HtmlReportGenerator.generateHtmlReport(summary, customHtmlPath);
            
            logger.info("Final consolidated report generated successfully");
            System.out.println("[OK] FINAL REPORT GENERATION COMPLETED");
            
        } catch (Exception e) {
            logger.error("Error generating final report: {}", e.getMessage(), e);
            System.err.println("[FAIL] FINAL REPORT GENERATION FAILED!");
            System.err.println("[CHECK] Error: " + e.getMessage());
            System.err.println("[FAIL] Error Type: " + e.getClass().getSimpleName());
            e.printStackTrace();
        }
        
        System.out.println("[DONE] Final report process completed.\n");
    }
    
    public TestCaseResult getTestCaseResult(String testCaseId) {
        TestCaseData data = testCases.get(testCaseId);
        if (data == null) return null;

        List<StepResult> stepResults = new ArrayList<>();
        data.steps.forEach(stepData -> stepResults.add(new StepResult(
                stepData.stepDescription, stepData.readableMessage, stepData.status,
                stepData.timestamp, stepData.screenshotPath, null)));

        return new TestCaseResult(data.testCaseId, data.scenarioName, data.startTime,
                data.endTime != null ? data.endTime : LocalDateTime.now(),
                data.status != null ? data.status : "RUNNING", stepResults);
    }

    public void storeRCAResult(String testCaseId, RCAResult result) {
        if (testCaseId != null && result != null) {
            rcaResults.put(testCaseId, result);
        }
    }

    public RCAResult getRCAResult(String testCaseId) {
        return testCaseId != null ? rcaResults.get(testCaseId) : null;
    }

    public void storeDefectDraft(String testCaseId, DefectDraft draft) {
        if (testCaseId != null && draft != null) {
            defectDrafts.put(testCaseId, draft);
        }
    }

    public DefectDraft getDefectDraft(String testCaseId) {
        return testCaseId != null ? defectDrafts.get(testCaseId) : null;
    }

    // Data classes
    public static class TestCaseData {
        public final String testCaseId;
        public final String scenarioName;
        public final LocalDateTime startTime;
        public LocalDateTime endTime;
        public String status;
        public final ConcurrentLinkedQueue<StepData> steps;
        
        public TestCaseData(String testCaseId, String scenarioName) {
            this.testCaseId = testCaseId;
            this.scenarioName = scenarioName;
            this.startTime = LocalDateTime.now();
            this.steps = new ConcurrentLinkedQueue<>();
            this.status = "RUNNING";
        }
    }
    
    public static class StepData {
        public final String testCaseId;
        public final String stepDescription;
        public final String readableMessage;
        public final String status;
        public final String screenshotPath;
        public final LocalDateTime timestamp;
        
        public StepData(String testCaseId, String stepDescription, String readableMessage, String status, String screenshotPath) {
            this.testCaseId = testCaseId;
            this.stepDescription = stepDescription;
            this.readableMessage = readableMessage;
            this.status = status;
            this.screenshotPath = screenshotPath;
            this.timestamp = LocalDateTime.now();
        }
    }
}