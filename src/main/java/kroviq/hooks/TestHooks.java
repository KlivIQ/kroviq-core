package kroviq.hooks;

import org.openqa.selenium.WebDriver;

import kroviq.utils.RunManager;
import kroviq.utils.TestContext;
import kroviq.utils.TestDataManager;
import kroviq.utils.WaitHandler;
import kroviq.wrapper.GenericWrapper;
import kroviq.reporting.*;
import kroviq.reporting.managers.TestRunReportManager;
import kroviq.api.core.ApiContext;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.BeforeStep;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestHooks {

    private static final Logger logger = LogManager.getLogger(TestHooks.class);

    private static WebDriver driver;
    private static String currentTestCaseId;

    @BeforeAll
    public static void globalSetup() {
        logger.info("=== GLOBAL SETUP - EXECUTION MODE: {} ===", RunManager.getExecutionMode());
        
        kroviq.utils.StartupGuard.validateFramework();
        kroviq.utils.ExecutionContext.initialize();
        
        TestRunReportManager reportManager = TestRunReportManager.getInstance();
        logger.info("   Run ID: {}", reportManager.getRunId());
        
        // REST-only mode: no browser, no navigation
        if (RunManager.isRestMode()) {
            logger.info("[Setup] REST mode — browser not initialized");
            return;
        }
        
        // Desktop mode: existing logic unchanged
        if (RunManager.isDesktopMode()) {
            logger.info("[Setup] Desktop mode — initializing WinAppDriver session");
            return;
        }
        
        // Web or Hybrid mode: initialize browser
        logger.info("[Setup] Browser mode: {}", RunManager.getBrowser());
        GenericWrapper.initializeDriver(RunManager.getBrowser());
        GenericWrapper.maximizeWindow();
        GenericWrapper.setImplicitWait(0);
        
        String environmentURL = RunManager.getEnvironmentURL();
        logger.info("Opening URL: {}", environmentURL);
        GenericWrapper.openUrl(environmentURL);
        
        driver = GenericWrapper.getDriver();
        logger.info("Browser initialized and ready for all scenarios");
    }

    @Before
    public void setup(Scenario scenario) {
        if (RunManager.isFailFastEnabled() && WaitHandler.isFatalExecutionStopped()) {
            logger.error("[FATAL] Execution stopped due to previous session failure. Skipping: {}", scenario.getName());
            throw new RuntimeException("[FATAL] Execution stopped due to previous session failure");
        }

        logger.info("Starting scenario: {}", scenario.getName());
        
        currentTestCaseId = extractTestCaseId(scenario);
        
        // Propagate execution context to TestContext
        TestContext.setCurrentTestCaseId(currentTestCaseId);
        TestContext.setCurrentScenario(scenario);
        
        // Extract and set module context
        String moduleFromUri = extractModuleFromUri(scenario.getUri().toString());
        TestContext.setCurrentModule(moduleFromUri);
        logger.info("Module detected: {}", moduleFromUri);
        
        // Iteration support
        int iterationIndex = extractIterationIndex(scenario);
        if (iterationIndex > 0) {
            String moduleName = moduleFromUri;
            kroviq.utils.IterationExecutor.setCurrentIteration(moduleName, currentTestCaseId, iterationIndex - 1);
        }
        
        // Initialize test case in reporting system
        try {
            TestRunReportManager.getInstance().startTestCase(currentTestCaseId, scenario.getName());
            StepResultTracker.setCurrentTestCase(currentTestCaseId);
        } catch (Exception e) {
            logger.error("Error initializing test case reporting: {}", e.getMessage());
        }
        
        if (driver != null) {
            try {
                driver.manage().deleteCookieNamed("JSESSIONID");
                driver.manage().deleteCookieNamed("auth-token");
                if (needsPageRefresh(scenario)) {
                    driver.navigate().refresh();
                    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
                        .until(webDriver -> ((org.openqa.selenium.JavascriptExecutor) webDriver)
                            .executeScript("return document.readyState").equals("complete"));
                }
            } catch (Exception e) {
                logger.error("Error in setup: {}", e.getMessage());
                StepResultTracker.stepFailed("Test Setup", null);
            }
        }
    }

    @BeforeStep
    public void beforeStep(Scenario scenario) {
        if (StepResultTracker.getCurrentTestCase() == null) {
            String testCaseId = extractTestCaseId(scenario);
            StepResultTracker.setCurrentTestCase(testCaseId);
        }
    }
    
    @AfterStep
    public void afterStep(Scenario scenario) {
        // Screenshot capture handled by StepResultTracker/StepHookManager
    }
    
    @After
    public void tearDown(Scenario scenario) {
        logger.info("Finishing scenario: {}", scenario.getName());
        
        if (kroviq.utils.MissingDataTracker.hasMissingData()) {
            String summary = kroviq.utils.MissingDataTracker.getSummary();
            logger.info("\n{}", summary);
            try {
                StepReportingWrapper.recordManualStep(summary, "WARN");
            } catch (Exception e) {
                logger.error("Failed to record missing data summary: {}", e.getMessage());
            }
        }
        
        kroviq.utils.MissingDataTracker.clear();
        
        String status = scenario.isFailed() ? "FAILED" : "PASSED";
        String testCaseId = extractTestCaseId(scenario);
        
        try {
            TestRunReportManager.getInstance().endTestCase(testCaseId, status);
            
            if (driver != null && shouldTakeScreenshot(scenario)) {
                String screenshotPath = ScreenshotManager.captureScreenshot(testCaseId, "Final Screenshot", status);
                if (screenshotPath != null) {
                    ExtentScreenshotAttacher.attachScreenshot(screenshotPath);
                }
            }
        } catch (Exception e) {
            logger.error("Error in test case teardown: {}", e.getMessage());
        }
        
        // Clean up thread-local data
        ApiContext.clearAll();
        StepResultTracker.cleanup();
        TestDataManager.get().clearRuntimeData();
        TestContext.clearCurrentModule();
        TestContext.clearCurrentTestCaseId();
        TestContext.clearCurrentScenario();
        TestContext.clearCurrentTableRow();
        
        logger.info("Scenario completed, browser remains open for next scenario");
    }
    
    @AfterAll
    public static void globalTearDown() {
        logger.info("=== GLOBAL TEARDOWN - CLOSING BROWSER ===");
        
        try {
            TestRunReportManager.getInstance().generateFinalReport();
            logger.info("CONSOLIDATED REPORT GENERATED SUCCESSFULLY!");
        } catch (Exception e) {
            logger.error("Report generation failed: {}", e.getMessage());
        }
        
        // QMetry integration -- push results if enabled
        try {
            kroviq.qmetry.QMetryResultPusher.pushIfEnabled();
        } catch (Exception e) {
            logger.error("QMetry upload failed: {}", e.getMessage());
        }
        
        try {
            GenericWrapper.quitDriver();
            logger.info("Browser closed after all scenarios completed");
        } catch (Exception e) {
            logger.error("Error closing browser: {}", e.getMessage());
        }
    }
    
    private String extractTestCaseId(Scenario scenario) {
        for (String tag : scenario.getSourceTagNames()) {
            if (tag.matches("@TC_.*|@.*TC.*")) {
                return tag.replace("@", "");
            }
        }
        return "N/A";
    }
    
    public static String getCurrentTestCaseId() {
        return currentTestCaseId;
    }
    
    private boolean needsPageRefresh(Scenario scenario) {
        String scenarioName = scenario.getName().toLowerCase();
        return scenarioName.contains("login") || 
               scenarioName.contains("logout") || 
               scenarioName.contains("session");
    }
    
    private boolean shouldTakeScreenshot(Scenario scenario) {
        return scenario.isFailed() || 
               scenario.getName().toLowerCase().contains("critical") ||
               scenario.getName().toLowerCase().contains("login");
    }
    
    private String extractModuleFromUri(String uri) {
        String[] parts = uri.replace("\\", "/").split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("testscripts".equals(parts[i]) || "features".equals(parts[i])) {
                return parts[i + 1];
            }
            if ("runtime_features".equals(parts[i])) {
                // Flat structure: runtime_features/Login.feature -> module "Login"
                return parts[i + 1].replace(".feature", "");
            }
        }
        throw new RuntimeException("Cannot extract module from URI: " + uri);
    }
    
    private int extractIterationIndex(Scenario scenario) {
        for (String tag : scenario.getSourceTagNames()) {
            if (tag.startsWith("@ITERATION_")) {
                try {
                    return Integer.parseInt(tag.substring(11));
                } catch (NumberFormatException e) {
                    logger.error("Invalid iteration tag format: {}", tag);
                }
            }
        }
        return 0;
    }
}
