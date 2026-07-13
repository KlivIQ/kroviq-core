package kroviq.ai.defect;

import kroviq.ai.rca.RCAResult;
import kroviq.reporting.TestCaseResult;
import kroviq.reporting.StepResult;
import kroviq.reporting.managers.TestRunReportManager;
import kroviq.utils.RunManager;
import kroviq.utils.TestContext;
import kroviq.wrapper.GenericWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;

public class DefectEvidenceCollector {
    private static final Logger logger = LogManager.getLogger(DefectEvidenceCollector.class);

    public static DefectContext collect(RCAResult rcaResult, String failedStep, String screenshotPath) {
        DefectContext.Builder builder = DefectContext.builder(rcaResult)
                .failedStep(failedStep);

        // Collect screenshot
        List<String> screenshots = new ArrayList<>();
        if (screenshotPath != null && !screenshotPath.isEmpty()) {
            screenshots.add(screenshotPath);
        }
        builder.screenshotPaths(screenshots);

        // Collect module and test case ID
        try {
            builder.moduleName(TestContext.getCurrentModule());
        } catch (Exception e) {
            logger.debug("Module context not available for defect writer");
        }

        try {
            String tcId = TestContext.getCurrentTestCaseId();
            if (tcId != null) builder.testCaseId(tcId);
        } catch (Exception e) {
            logger.debug("TestCaseId not available for defect writer");
        }

        // Collect scenario name
        try {
            if (TestContext.getCurrentScenario() != null) {
                builder.scenarioName(TestContext.getCurrentScenario().getName());
            }
        } catch (Exception e) {
            logger.debug("Scenario not available for defect writer");
        }

        // Collect current URL
        try {
            WebDriver driver = GenericWrapper.getDriver();
            if (driver != null) {
                builder.currentUrl(driver.getCurrentUrl());
            }
        } catch (Exception e) {
            logger.debug("Could not collect URL for defect writer");
        }

        // Collect environment info
        try {
            builder.environmentName(RunManager.getEnvironmentURL());
            builder.browser(RunManager.getBrowser());
        } catch (Exception e) {
            logger.debug("Could not collect environment info for defect writer");
        }

        // Collect previous steps
        builder.previousSteps(collectPreviousSteps());

        // Collect exception message from RCA
        if (rcaResult != null) {
            builder.exceptionMessage(rcaResult.getEvidenceObserved());
        }

        return builder.build();
    }

    private static List<String> collectPreviousSteps() {
        List<String> previous = new ArrayList<>();
        try {
            String tcId = TestContext.getCurrentTestCaseId();
            if (tcId != null) {
                TestCaseResult tcResult = TestRunReportManager.getInstance().getTestCaseResult(tcId);
                if (tcResult != null) {
                    List<StepResult> steps = tcResult.getSteps();
                    for (StepResult step : steps) {
                        previous.add(step.getDisplayMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not collect previous steps for defect writer");
        }
        return previous;
    }
}
