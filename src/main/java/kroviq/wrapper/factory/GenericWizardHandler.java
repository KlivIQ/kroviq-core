package kroviq.wrapper.factory;

import kroviq.wrapper.core.HandlerUtils;
import kroviq.wrapper.core.WizardHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class GenericWizardHandler implements WizardHandler {

    private static final Logger logger = LogManager.getLogger(GenericWizardHandler.class);
    private final WebDriver driver;

    private static final String[] STEP_ITEM_SELECTORS = {
            // AntD Steps
            ".ant-steps-item",
            // MUI Stepper
            ".MuiStep-root",
            // Angular Material Stepper
            "mat-step-header, .mat-step-header",
            // PrimeNG Steps
            ".p-steps-item",
            // Generic ARIA
            "[role='tab'], [role='step']",
            // Generic class-based
            "[class*='step-item'], [class*='wizard-step']"
    };

    private static final String[] ACTIVE_INDICATORS = {
            "ant-steps-item-active", "ant-steps-item-process",
            "Mui-active", "MuiStepLabel-active",
            "mat-step-header[aria-selected='true']",
            "p-highlight", "p-steps-current",
            "active", "current", "selected"
    };

    private static final String[] COMPLETED_INDICATORS = {
            "ant-steps-item-finish",
            "Mui-completed", "MuiStepLabel-completed",
            "mat-step-completed",
            "p-steps-complete",
            "completed", "done", "finished"
    };

    private static final String[] ERROR_INDICATORS = {
            "ant-steps-item-error",
            "Mui-error",
            "mat-step-error",
            "error", "invalid", "failed"
    };

    public GenericWizardHandler(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void nextStep(WebDriver driver, WebElement wizardRoot) {
        int currentIndex = getActiveStepIndex(driver, wizardRoot);
        int total = getTotalSteps(driver, wizardRoot);
        if (currentIndex < total - 1) {
            goToStep(driver, wizardRoot, currentIndex + 1);
            logger.info("[Wizard] Next step: {} -> {}", currentIndex, currentIndex + 1);
        }
    }

    @Override
    public void previousStep(WebDriver driver, WebElement wizardRoot) {
        int currentIndex = getActiveStepIndex(driver, wizardRoot);
        if (currentIndex > 0) {
            goToStep(driver, wizardRoot, currentIndex - 1);
            logger.info("[Wizard] Previous step: {} -> {}", currentIndex, currentIndex - 1);
        }
    }

    @Override
    public void goToStep(WebDriver driver, WebElement wizardRoot, String stepName) {
        List<WebElement> steps = findStepItems(wizardRoot);
        for (int i = 0; i < steps.size(); i++) {
            String text = getStepText(steps.get(i));
            if (text.equalsIgnoreCase(stepName) || text.contains(stepName)) {
                clickStep(driver, steps.get(i));
                waitForTransition(driver);
                logger.info("[Wizard] Navigated to step: '{}'", stepName);
                return;
            }
        }
        throw new RuntimeException("[Wizard] Step '" + stepName + "' not found");
    }

    @Override
    public void goToStep(WebDriver driver, WebElement wizardRoot, int stepIndex) {
        List<WebElement> steps = findStepItems(wizardRoot);
        if (stepIndex < 0 || stepIndex >= steps.size()) {
            throw new RuntimeException("[Wizard] Step index " + stepIndex + " out of range (0-" + (steps.size() - 1) + ")");
        }
        clickStep(driver, steps.get(stepIndex));
        waitForTransition(driver);
    }

    @Override
    public String getActiveStep(WebDriver driver, WebElement wizardRoot) {
        List<WebElement> steps = findStepItems(wizardRoot);
        for (WebElement step : steps) {
            if (isActive(step)) return getStepText(step);
        }
        return "";
    }

    @Override
    public int getActiveStepIndex(WebDriver driver, WebElement wizardRoot) {
        List<WebElement> steps = findStepItems(wizardRoot);
        for (int i = 0; i < steps.size(); i++) {
            if (isActive(steps.get(i))) return i;
        }
        return 0;
    }

    @Override
    public boolean isStepCompleted(WebDriver driver, WebElement wizardRoot, String stepName) {
        WebElement step = findStepByName(wizardRoot, stepName);
        return step != null && hasIndicator(step, COMPLETED_INDICATORS);
    }

    @Override
    public boolean isStepError(WebDriver driver, WebElement wizardRoot, String stepName) {
        WebElement step = findStepByName(wizardRoot, stepName);
        return step != null && hasIndicator(step, ERROR_INDICATORS);
    }

    @Override
    public int getTotalSteps(WebDriver driver, WebElement wizardRoot) {
        return findStepItems(wizardRoot).size();
    }

    // --- Private helpers ---

    private List<WebElement> findStepItems(WebElement wizardRoot) {
        for (String selector : STEP_ITEM_SELECTORS) {
            List<WebElement> items = wizardRoot.findElements(By.cssSelector(selector));
            if (!items.isEmpty()) return items;
        }
        return List.of();
    }

    private boolean isActive(WebElement step) {
        String className = step.getAttribute("class");
        if (className == null) className = "";
        String ariaSelected = step.getAttribute("aria-selected");
        String ariaCurrent = step.getAttribute("aria-current");

        if ("true".equals(ariaSelected) || "step".equals(ariaCurrent)) return true;

        for (String indicator : ACTIVE_INDICATORS) {
            if (className.contains(indicator)) return true;
        }
        return false;
    }

    private boolean hasIndicator(WebElement step, String[] indicators) {
        String className = step.getAttribute("class");
        if (className == null) className = "";
        for (String indicator : indicators) {
            if (className.contains(indicator)) return true;
        }
        // Check child elements for indicator classes
        for (String indicator : indicators) {
            List<WebElement> children = step.findElements(By.cssSelector("[class*='" + indicator + "']"));
            if (!children.isEmpty()) return true;
        }
        return false;
    }

    private String getStepText(WebElement step) {
        // Try specific title/label selectors first
        String[] titleSelectors = {
                ".ant-steps-item-title",
                ".MuiStepLabel-label",
                ".mat-step-text-label",
                ".p-steps-title, .p-menuitem-text",
                "[class*='step-title']",
                "[class*='step-label']"
        };
        for (String sel : titleSelectors) {
            List<WebElement> titles = step.findElements(By.cssSelector(sel));
            for (WebElement t : titles) {
                String text = t.getText().trim();
                if (!text.isEmpty()) return text;
            }
        }
        // Fallback: first line of step text
        String text = step.getText().trim();
        if (text.contains("\n")) text = text.split("\n")[0].trim();
        return text;
    }

    private WebElement findStepByName(WebElement wizardRoot, String stepName) {
        List<WebElement> steps = findStepItems(wizardRoot);
        for (WebElement step : steps) {
            String text = getStepText(step);
            if (text.equalsIgnoreCase(stepName) || text.contains(stepName)) return step;
        }
        return null;
    }

    private void clickStep(WebDriver driver, WebElement step) {
        // Try clicking title/header first (some steppers need this)
        List<WebElement> clickables = step.findElements(By.cssSelector(
                ".ant-steps-item-container, .MuiStepButton-root, mat-step-header, a, button, [role='tab']"));
        if (!clickables.isEmpty()) {
            HandlerUtils.scrollIntoViewAndClick(driver, clickables.get(0));
        } else {
            HandlerUtils.scrollIntoViewAndClick(driver, step);
        }
    }

    private void waitForTransition(WebDriver driver) {
        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
