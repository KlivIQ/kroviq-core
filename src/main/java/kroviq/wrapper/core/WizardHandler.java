package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public interface WizardHandler {

    void nextStep(WebDriver driver, WebElement wizardRoot);

    void previousStep(WebDriver driver, WebElement wizardRoot);

    void goToStep(WebDriver driver, WebElement wizardRoot, String stepName);

    void goToStep(WebDriver driver, WebElement wizardRoot, int stepIndex);

    String getActiveStep(WebDriver driver, WebElement wizardRoot);

    int getActiveStepIndex(WebDriver driver, WebElement wizardRoot);

    boolean isStepCompleted(WebDriver driver, WebElement wizardRoot, String stepName);

    boolean isStepError(WebDriver driver, WebElement wizardRoot, String stepName);

    int getTotalSteps(WebDriver driver, WebElement wizardRoot);
}
