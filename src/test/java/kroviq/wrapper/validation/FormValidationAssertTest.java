package kroviq.wrapper.validation;

import kroviq.wrapper.core.FormValidationAssert;
import kroviq.wrapper.factory.GenericFormValidationAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FormValidationAssertTest {

    private WebDriver driver;
    @Mock private WebElement fieldElement;
    @Mock private WebElement wrapperElement;
    @Mock private WebElement errorElement;
    @Mock private WebElement formElement;

    private GenericFormValidationAssert validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        validator = new GenericFormValidationAssert();
        // Default: no errors, no wrappers
        when(fieldElement.findElement(any(By.class))).thenThrow(new org.openqa.selenium.NoSuchElementException(""));
        when(fieldElement.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(fieldElement.getAttribute("aria-describedby")).thenReturn(null);
        when(fieldElement.getAttribute("aria-invalid")).thenReturn(null);
        when(fieldElement.getAttribute("class")).thenReturn("");
    }

    @Test
    void implementsInterface() {
        assertInstanceOf(FormValidationAssert.class, validator);
    }

    // --- hasError ---

    @Test
    void hasError_antdError_returnsTrue() {
        when(fieldElement.findElement(any(By.class))).thenReturn(wrapperElement);
        when(wrapperElement.findElements(By.cssSelector(".ant-form-item-explain-error")))
                .thenReturn(List.of(errorElement));
        when(errorElement.isDisplayed()).thenReturn(true);
        when(errorElement.getText()).thenReturn("This field is required");
        when(wrapperElement.findElements(any(By.class))).thenReturn(Collections.emptyList());
        // Override for the specific selector
        when(wrapperElement.findElements(By.cssSelector(".ant-form-item-explain-error")))
                .thenReturn(List.of(errorElement));

        assertTrue(validator.hasError(driver, fieldElement));
    }

    @Test
    void hasError_noErrors_returnsFalse() {
        assertFalse(validator.hasError(driver, fieldElement));
    }

    @Test
    void hasError_muiError_returnsTrue() {
        when(fieldElement.findElement(any(By.class))).thenReturn(wrapperElement);
        when(wrapperElement.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(wrapperElement.findElements(By.cssSelector(".MuiFormHelperText-root.Mui-error")))
                .thenReturn(List.of(errorElement));
        when(errorElement.isDisplayed()).thenReturn(true);
        when(errorElement.getText()).thenReturn("Invalid email format");

        assertTrue(validator.hasError(driver, fieldElement));
    }

    // --- getErrorMessage ---

    @Test
    void getErrorMessage_antdRequired_returnsText() {
        when(fieldElement.findElement(any(By.class))).thenReturn(wrapperElement);
        when(wrapperElement.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(wrapperElement.findElements(By.cssSelector(".ant-form-item-explain-error")))
                .thenReturn(List.of(errorElement));
        when(errorElement.isDisplayed()).thenReturn(true);
        when(errorElement.getText()).thenReturn("Please enter username");

        assertEquals("Please enter username", validator.getErrorMessage(driver, fieldElement));
    }

    @Test
    void getErrorMessage_noError_returnsEmpty() {
        assertEquals("", validator.getErrorMessage(driver, fieldElement));
    }

    @Test
    void getErrorMessage_ariaDescribedBy_returnsLinkedText() {
        when(fieldElement.getAttribute("aria-describedby")).thenReturn("email-error");
        WebElement linkedError = mock(WebElement.class);
        when(driver.findElement(By.id("email-error"))).thenReturn(linkedError);
        when(linkedError.isDisplayed()).thenReturn(true);
        when(linkedError.getText()).thenReturn("Must be a valid email");
        when(linkedError.getAttribute("class")).thenReturn("error-text");

        assertEquals("Must be a valid email", validator.getErrorMessage(driver, fieldElement));
    }

    // --- isFieldInvalid ---

    @Test
    void isFieldInvalid_ariaInvalidTrue_returnsTrue() {
        when(fieldElement.getAttribute("aria-invalid")).thenReturn("true");
        assertTrue(validator.isFieldInvalid(driver, fieldElement));
    }

    @Test
    void isFieldInvalid_ariaInvalidFalse_returnsFalse() {
        when(fieldElement.getAttribute("aria-invalid")).thenReturn("false");
        when(((JavascriptExecutor) driver).executeScript(anyString(), any(WebElement.class))).thenReturn(false);
        assertFalse(validator.isFieldInvalid(driver, fieldElement));
    }

    @Test
    void isFieldInvalid_ngInvalidClass_returnsTrue() {
        when(fieldElement.getAttribute("class")).thenReturn("ng-dirty ng-invalid");
        assertTrue(validator.isFieldInvalid(driver, fieldElement));
    }

    @Test
    void isFieldInvalid_muiErrorClass_returnsTrue() {
        when(fieldElement.getAttribute("class")).thenReturn("MuiInput-root Mui-error");
        assertTrue(validator.isFieldInvalid(driver, fieldElement));
    }

    @Test
    void isFieldInvalid_antdStatusError_returnsTrue() {
        when(fieldElement.getAttribute("class")).thenReturn("ant-input ant-input-status-error");
        assertTrue(validator.isFieldInvalid(driver, fieldElement));
    }

    // --- getAllErrors ---

    @Test
    void getAllErrors_multipleErrors_returnsList() {
        WebElement err1 = mock(WebElement.class);
        WebElement err2 = mock(WebElement.class);
        when(err1.isDisplayed()).thenReturn(true);
        when(err1.getText()).thenReturn("Username is required");
        when(err2.isDisplayed()).thenReturn(true);
        when(err2.getText()).thenReturn("Password is required");

        when(formElement.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(formElement.findElements(By.cssSelector(".ant-form-item-explain-error")))
                .thenReturn(List.of(err1, err2));

        List<String> errors = validator.getAllErrors(driver, formElement);
        assertEquals(2, errors.size());
        assertTrue(errors.contains("Username is required"));
        assertTrue(errors.contains("Password is required"));
    }

    @Test
    void getAllErrors_noErrors_returnsEmptyList() {
        when(formElement.findElements(any(By.class))).thenReturn(Collections.emptyList());
        List<String> errors = validator.getAllErrors(driver, formElement);
        assertTrue(errors.isEmpty());
    }
}
