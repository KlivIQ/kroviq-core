package kroviq.reporting;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import kroviq.ai.rca.*;
import kroviq.ai.defect.*;
import kroviq.utils.RunManager;
import kroviq.utils.TestContext;
import kroviq.wrapper.GenericWrapper;

public class StepHookManager {
    private static final ThreadLocal<String> currentStepName = new ThreadLocal<>();
    private static final ThreadLocal<Long> stepStartTime = new ThreadLocal<>();
    private static final ThreadLocal<String> readableMessage = new ThreadLocal<>();
    private static final ThreadLocal<StepCategory> stepCategory = new ThreadLocal<>();
    
    /**
     * Step category set by the caller (StepReportingWrapper) to indicate
     * whether the step is an action or validation. Used to determine
     * success status: ACTION -> INFO, VALIDATION -> PASS.
     */
    public enum StepCategory {
        ACTION,
        VALIDATION
    }
    
    public static void setStepCategory(StepCategory category) {
        stepCategory.set(category);
    }
    
    public static void startStep(String stepName) {
        currentStepName.set(stepName);
        stepStartTime.set(System.currentTimeMillis());
    }
    
    public static void endStep(boolean passed, String readable) {
        readableMessage.set(readable);
        endStep(passed);
    }
    
    public static void endStep(boolean passed) {
        String stepName = currentStepName.get();
        Long startTime = stepStartTime.get();
        String readable = readableMessage.get();
        StepCategory category = stepCategory.get();
        
        if (stepName != null && startTime != null) {
            if (passed) {
                if (isValidationStep(category, stepName)) {
                    StepResultTracker.stepPassed(stepName, readable);
                } else {
                    StepResultTracker.stepActionPassed(stepName, readable);
                }
            } else {
                String screenshotPath = captureFailureScreenshot(stepName);
                StepResultTracker.stepFailed(stepName, readable, screenshotPath);
                performRCA(stepName, screenshotPath);
            }
        }
        
        // Cleanup
        currentStepName.remove();
        stepStartTime.remove();
        readableMessage.remove();
        stepCategory.remove();
    }
    
    /**
     * Determines if a step is a validation step.
     * Primary: keyword matching on step name (validation keywords override caller category).
     * Secondary: uses explicit StepCategory set by the caller when no keywords match.
     */
    private static boolean isValidationStep(StepCategory category, String stepName) {
        if (isValidationByKeyword(stepName)) {
            return true;
        }
        if (category != null) {
            return category == StepCategory.VALIDATION;
        }
        return false;
    }
    
    /**
     * Fallback keyword classifier -- used only when StepCategory is not explicitly set.
     * Isolated here for easy future refinement or replacement.
     */
    private static boolean isValidationByKeyword(String stepName) {
        if (stepName == null) return false;
        String lower = stepName.toLowerCase();
        return lower.startsWith("verify ") || lower.startsWith("validate ")
            || lower.startsWith("assert ") || lower.contains("should see");
    }
    
    private static String captureFailureScreenshot(String stepName) {
        try {
            WebDriver driver = GenericWrapper.getDriver();
            if (driver != null) {
                String testCaseId = StepResultTracker.getCurrentTestCase();
                if (testCaseId != null) {
                    return ScreenshotManager.captureScreenshot(testCaseId, stepName, "FAILED");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to capture failure screenshot: " + e.getMessage());
        }
        return null;
    }
    
    public static String getCurrentStepName() {
        return currentStepName.get();
    }

    private static final ThreadLocal<Throwable> lastFailureException = new ThreadLocal<>();

    public static void setLastFailureException(Throwable t) { lastFailureException.set(t); }

    private static void performRCA(String stepName, String screenshotPath) {
        try {
            if (!RunManager.isRcaEnabled()) return;

            Throwable exception = lastFailureException.get();
            lastFailureException.remove();

            RCAContext context = RCAEvidenceCollector.collect(stepName, exception, screenshotPath);
            RCAAssistant assistant = new RCAAssistant();
            RCAResult result = assistant.analyze(context);
            TestContext.setLastRCAResult(result);

            // Persist for HTML report rendering
            String tcId = TestContext.getCurrentTestCaseId();
            if (tcId != null) {
                kroviq.reporting.managers.TestRunReportManager.getInstance().storeRCAResult(tcId, result);
                RCARecurrenceTracker.getInstance().recordExecution(tcId);
            }

            // Generate defect draft
            generateDefectDraft(result, stepName, screenshotPath);
        } catch (Exception e) {
            System.err.println("[RCA] Analysis failed (non-fatal): " + e.getMessage());
        }
    }

    private static void generateDefectDraft(RCAResult rcaResult, String stepName, String screenshotPath) {
        try {
            if (!RunManager.isDefectWriterEnabled()) return;

            DefectContext defectContext = DefectEvidenceCollector.collect(rcaResult, stepName, screenshotPath);
            DefectWriterAssistant writer = new DefectWriterAssistant();
            DefectDraft draft = writer.generate(defectContext);
            TestContext.setLastDefectDraft(draft);

            String tcId = TestContext.getCurrentTestCaseId();
            if (tcId != null) {
                kroviq.reporting.managers.TestRunReportManager.getInstance().storeDefectDraft(tcId, draft);
            }
        } catch (Exception e) {
            System.err.println("[DefectWriter] Draft generation failed (non-fatal): " + e.getMessage());
        }
    }
}