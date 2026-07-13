package kroviq.wrapper.wizard;

import kroviq.wrapper.core.WizardHandler;
import kroviq.wrapper.factory.ComponentHandlerFactory;
import kroviq.wrapper.factory.GenericWizardHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WizardHandlerTest {

    private WebDriver driver;
    private WebElement wizardRoot;
    private GenericWizardHandler handler;

    @BeforeEach
    void setUp() {
        driver = mock(WebDriver.class);
        wizardRoot = mock(WebElement.class);
        handler = new GenericWizardHandler(driver);
    }

    @Test
    void implementsInterface() {
        assertInstanceOf(WizardHandler.class, handler);
    }

    @Test
    void factory_returnsGenericHandler() {
        when(wizardRoot.getAttribute("class")).thenReturn("wizard-container");
        when(wizardRoot.getTagName()).thenReturn("div");
        when(wizardRoot.getAttribute("data-testid")).thenReturn(null);
        WizardHandler h = ComponentHandlerFactory.getWizardHandler(wizardRoot, driver);
        assertInstanceOf(GenericWizardHandler.class, h);
    }

    @Test
    void getTotalSteps_antdSteps_returnsCount() {
        WebElement step1 = mock(WebElement.class);
        WebElement step2 = mock(WebElement.class);
        WebElement step3 = mock(WebElement.class);
        when(wizardRoot.findElements(By.cssSelector(".ant-steps-item")))
                .thenReturn(List.of(step1, step2, step3));

        assertEquals(3, handler.getTotalSteps(driver, wizardRoot));
    }

    @Test
    void getActiveStepIndex_antdActive_returnsIndex() {
        WebElement step1 = mock(WebElement.class);
        WebElement step2 = mock(WebElement.class);
        WebElement step3 = mock(WebElement.class);
        when(step1.getAttribute("class")).thenReturn("ant-steps-item ant-steps-item-finish");
        when(step2.getAttribute("class")).thenReturn("ant-steps-item ant-steps-item-active");
        when(step3.getAttribute("class")).thenReturn("ant-steps-item ant-steps-item-wait");
        when(step1.getAttribute("aria-selected")).thenReturn(null);
        when(step1.getAttribute("aria-current")).thenReturn(null);
        when(step2.getAttribute("aria-selected")).thenReturn(null);
        when(step2.getAttribute("aria-current")).thenReturn(null);
        when(step3.getAttribute("aria-selected")).thenReturn(null);
        when(step3.getAttribute("aria-current")).thenReturn(null);
        when(wizardRoot.findElements(By.cssSelector(".ant-steps-item")))
                .thenReturn(List.of(step1, step2, step3));

        assertEquals(1, handler.getActiveStepIndex(driver, wizardRoot));
    }

    @Test
    void getActiveStep_muiStepper_returnsLabel() {
        WebElement step1 = mock(WebElement.class);
        WebElement step2 = mock(WebElement.class);
        WebElement label2 = mock(WebElement.class);
        when(step1.getAttribute("class")).thenReturn("MuiStep-root");
        when(step1.getAttribute("aria-selected")).thenReturn(null);
        when(step1.getAttribute("aria-current")).thenReturn(null);
        when(step2.getAttribute("class")).thenReturn("MuiStep-root Mui-active");
        when(step2.getAttribute("aria-selected")).thenReturn(null);
        when(step2.getAttribute("aria-current")).thenReturn(null);
        when(step2.findElements(By.cssSelector(".MuiStepLabel-label"))).thenReturn(List.of(label2));
        when(step2.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(step2.findElements(By.cssSelector(".MuiStepLabel-label"))).thenReturn(List.of(label2));
        when(label2.getText()).thenReturn("Payment");
        when(wizardRoot.findElements(By.cssSelector(".ant-steps-item"))).thenReturn(Collections.emptyList());
        when(wizardRoot.findElements(By.cssSelector(".MuiStep-root"))).thenReturn(List.of(step1, step2));

        assertEquals("Payment", handler.getActiveStep(driver, wizardRoot));
    }

    @Test
    void isStepCompleted_finishedStep_returnsTrue() {
        WebElement step1 = mock(WebElement.class);
        WebElement titleEl = mock(WebElement.class);
        when(step1.getAttribute("class")).thenReturn("ant-steps-item ant-steps-item-finish");
        when(step1.findElements(By.cssSelector(".ant-steps-item-title"))).thenReturn(List.of(titleEl));
        when(step1.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(step1.findElements(By.cssSelector(".ant-steps-item-title"))).thenReturn(List.of(titleEl));
        when(titleEl.getText()).thenReturn("Personal Info");
        when(step1.getText()).thenReturn("Personal Info");
        when(wizardRoot.findElements(By.cssSelector(".ant-steps-item"))).thenReturn(List.of(step1));

        assertTrue(handler.isStepCompleted(driver, wizardRoot, "Personal Info"));
    }

    @Test
    void isStepError_errorStep_returnsTrue() {
        WebElement step1 = mock(WebElement.class);
        WebElement titleEl = mock(WebElement.class);
        when(step1.getAttribute("class")).thenReturn("ant-steps-item ant-steps-item-error");
        when(step1.findElements(By.cssSelector(".ant-steps-item-title"))).thenReturn(List.of(titleEl));
        when(step1.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(step1.findElements(By.cssSelector(".ant-steps-item-title"))).thenReturn(List.of(titleEl));
        when(titleEl.getText()).thenReturn("Verification");
        when(step1.getText()).thenReturn("Verification");
        when(wizardRoot.findElements(By.cssSelector(".ant-steps-item"))).thenReturn(List.of(step1));

        assertTrue(handler.isStepError(driver, wizardRoot, "Verification"));
    }

    @Test
    void getTotalSteps_emptyWizard_returnsZero() {
        when(wizardRoot.findElements(any(By.class))).thenReturn(Collections.emptyList());
        assertEquals(0, handler.getTotalSteps(driver, wizardRoot));
    }
}
