package kroviq.ai.rca;

import kroviq.reporting.StepResult;
import kroviq.reporting.managers.TestRunReportManager;
import kroviq.reporting.TestCaseResult;
import kroviq.utils.TestContext;
import kroviq.wrapper.GenericWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;

public class RCAEvidenceCollector {
    private static final Logger logger = LogManager.getLogger(RCAEvidenceCollector.class);
    private static final int MAX_PREVIOUS_STEPS = 5;

    public static RCAContext collect(String failedStepDescription, Throwable exception, String screenshotPath) {
        RCAContext.Builder builder = RCAContext.builder(failedStepDescription, exception)
                .screenshotPath(screenshotPath != null ? screenshotPath : "");

        // Collect current URL
        try {
            WebDriver driver = GenericWrapper.getDriver();
            if (driver != null) {
                builder.currentUrl(driver.getCurrentUrl());
            }
        } catch (Exception e) {
            logger.debug("Could not collect current URL: {}", e.getMessage());
        }

        // Collect page source (optional — for loading indicator detection)
        try {
            WebDriver driver = GenericWrapper.getDriver();
            if (driver != null) {
                String source = driver.getPageSource();
                // Only store first 5000 chars to keep memory bounded
                if (source != null && source.length() > 5000) {
                    source = source.substring(0, 5000);
                }
                builder.pageSource(source != null ? source : "");
            }
        } catch (Exception e) {
            logger.debug("Could not collect page source: {}", e.getMessage());
        }

        // Collect module and test case ID
        try {
            builder.moduleName(TestContext.getCurrentModule());
        } catch (Exception e) {
            logger.debug("Module context not available");
        }

        try {
            String tcId = TestContext.getCurrentTestCaseId();
            if (tcId != null) builder.testCaseId(tcId);
        } catch (Exception e) {
            logger.debug("TestCaseId not available");
        }

        // Collect previous steps
        builder.previousSteps(collectPreviousSteps());

        // Extract locator from exception message if available
        builder.locatorUsed(extractLocatorFromException(exception));

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
                    int start = Math.max(0, steps.size() - MAX_PREVIOUS_STEPS);
                    for (int i = start; i < steps.size(); i++) {
                        previous.add(steps.get(i).getDisplayMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not collect previous steps: {}", e.getMessage());
        }
        return previous;
    }

    private static String extractLocatorFromException(Throwable exception) {
        if (exception == null || exception.getMessage() == null) return "";
        String msg = exception.getMessage();

        // Common Selenium patterns: "Unable to locate element: {method: xpath, selector: ...}"
        int selectorIdx = msg.indexOf("selector:");
        if (selectorIdx < 0) selectorIdx = msg.indexOf("\"selector\":");
        if (selectorIdx >= 0) {
            String sub = msg.substring(selectorIdx).replaceFirst("^[^:]+:\\s*", "");
            // Strip leading quotes/escapes
            sub = sub.replaceAll("^\\\\\"", "").replaceAll("^\"" , "");
            int end = sub.indexOf('}');
            if (end < 0) end = sub.indexOf('\n');
            if (end < 0) end = Math.min(sub.length(), 120);
            String locator = sub.substring(0, end).trim();
            // Strip trailing quotes/escapes
            locator = locator.replaceAll("\\\\\"$", "").replaceAll("\"$", "");
            return locator;
        }

        // Pattern: "By.xpath: //..."
        int byIdx = msg.indexOf("By.");
        if (byIdx >= 0) {
            String sub = msg.substring(byIdx);
            int end = sub.indexOf('\n');
            if (end > 0) return sub.substring(0, end).trim();
            return sub.substring(0, Math.min(sub.length(), 120)).trim();
        }

        return "";
    }
}
