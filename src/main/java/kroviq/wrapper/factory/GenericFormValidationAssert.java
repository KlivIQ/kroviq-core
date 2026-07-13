package kroviq.wrapper.factory;

import kroviq.wrapper.core.FormValidationAssert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class GenericFormValidationAssert implements FormValidationAssert {

    private static final Logger logger = LogManager.getLogger(GenericFormValidationAssert.class);

    // Field-level error message selectors (scoped to field wrapper)
    private static final String[] FIELD_ERROR_SELECTORS = {
            // AntD
            ".ant-form-item-explain-error",
            ".ant-form-item-explain .ant-form-item-explain-error",

            // MUI
            ".MuiFormHelperText-root.Mui-error",
            "p.MuiFormHelperText-root.Mui-error",

            // Angular Material
            "mat-error",
            ".mat-mdc-form-field-error",
            ".mat-error",

            // PrimeNG
            "small.p-error",
            ".p-error",
            "p-message[severity='error']",

            // ARIA standard (aria-describedby linked error)
            "[role='alert']",

            // Generic patterns
            "[class*='error-message']",
            "[class*='field-error']",
            "[class*='validation-error']",
            "[class*='invalid-feedback']",
            ".error-text",
            ".help-block.text-danger"
    };

    // Form-level error selectors (page-scoped)
    private static final String[] FORM_ERROR_SELECTORS = {
            // AntD
            ".ant-form-item-explain-error",
            ".ant-alert-error .ant-alert-message",

            // MUI
            ".MuiAlert-standardError .MuiAlert-message",
            ".MuiFormHelperText-root.Mui-error",

            // Angular Material
            "mat-error",
            ".mat-mdc-form-field-error",

            // PrimeNG
            ".p-error",
            ".p-message-error .p-message-text",
            "p-messages[severity='error'] .p-message-text",

            // Generic
            "[class*='error-message']",
            "[class*='validation-error']",
            "[class*='form-error']",
            "[role='alert']",
            ".alert-danger",
            ".error-summary"
    };

    // Field wrapper selectors (to scope error lookup to the correct field)
    private static final String[] WRAPPER_SELECTORS = {
            // AntD
            "ancestor::div[contains(@class,'ant-form-item')][1]",
            // MUI
            "ancestor::div[contains(@class,'MuiFormControl')][1]",
            // Angular Material
            "ancestor::mat-form-field[1]",
            // PrimeNG
            "ancestor::*[contains(@class,'p-field') or contains(@class,'p-float-label')][1]",
            // Generic
            "ancestor::div[contains(@class,'form-group') or contains(@class,'field-wrapper') or contains(@class,'form-field')][1]"
    };

    @Override
    public boolean hasError(WebDriver driver, WebElement fieldElement) {
        WebElement wrapper = findFieldWrapper(fieldElement);
        WebElement scope = wrapper != null ? wrapper : fieldElement;

        for (String selector : FIELD_ERROR_SELECTORS) {
            List<WebElement> errors = scope.findElements(By.cssSelector(selector));
            for (WebElement err : errors) {
                try {
                    if (err.isDisplayed() && !err.getText().trim().isEmpty()) {
                        return true;
                    }
                } catch (Exception e) { /* next */ }
            }
        }

        // Check aria-describedby linked error
        String describedBy = fieldElement.getAttribute("aria-describedby");
        if (describedBy != null && !describedBy.isEmpty()) {
            try {
                WebElement described = driver.findElement(By.id(describedBy));
                if (described.isDisplayed()) {
                    String cls = described.getAttribute("class");
                    if (cls != null && (cls.contains("error") || cls.contains("invalid"))) {
                        return true;
                    }
                }
            } catch (Exception e) { /* no linked element */ }
        }

        return false;
    }

    @Override
    public String getErrorMessage(WebDriver driver, WebElement fieldElement) {
        WebElement wrapper = findFieldWrapper(fieldElement);
        WebElement scope = wrapper != null ? wrapper : fieldElement;

        for (String selector : FIELD_ERROR_SELECTORS) {
            List<WebElement> errors = scope.findElements(By.cssSelector(selector));
            for (WebElement err : errors) {
                try {
                    if (err.isDisplayed()) {
                        String text = err.getText().trim();
                        if (!text.isEmpty()) {
                            logger.debug("[FormValidation] Error found via '{}': {}", selector, text);
                            return text;
                        }
                    }
                } catch (Exception e) { /* next */ }
            }
        }

        // Fallback: aria-describedby
        String describedBy = fieldElement.getAttribute("aria-describedby");
        if (describedBy != null && !describedBy.isEmpty()) {
            try {
                WebElement described = driver.findElement(By.id(describedBy));
                if (described.isDisplayed()) {
                    String text = described.getText().trim();
                    if (!text.isEmpty()) return text;
                }
            } catch (Exception e) { /* no linked element */ }
        }

        return "";
    }

    @Override
    public List<String> getAllErrors(WebDriver driver, WebElement formOrContainer) {
        List<String> errors = new ArrayList<>();

        for (String selector : FORM_ERROR_SELECTORS) {
            List<WebElement> elements = formOrContainer.findElements(By.cssSelector(selector));
            for (WebElement el : elements) {
                try {
                    if (el.isDisplayed()) {
                        String text = el.getText().trim();
                        if (!text.isEmpty() && !errors.contains(text)) {
                            errors.add(text);
                        }
                    }
                } catch (Exception e) { /* skip stale/hidden */ }
            }
        }

        return errors;
    }

    @Override
    public boolean isFieldInvalid(WebDriver driver, WebElement fieldElement) {
        // Check 1: aria-invalid
        String ariaInvalid = fieldElement.getAttribute("aria-invalid");
        if ("true".equals(ariaInvalid)) return true;

        // Check 2: class-based invalid state
        String className = fieldElement.getAttribute("class");
        if (className != null) {
            if (className.contains("ng-invalid") || className.contains("is-invalid")
                    || className.contains("Mui-error") || className.contains("p-invalid")
                    || className.contains("ant-input-status-error")) {
                return true;
            }
        }

        // Check 3: wrapper class has error indicator
        WebElement wrapper = findFieldWrapper(fieldElement);
        if (wrapper != null) {
            String wrapperClass = wrapper.getAttribute("class");
            if (wrapperClass != null) {
                if (wrapperClass.contains("has-error") || wrapperClass.contains("ant-form-item-has-error")
                        || wrapperClass.contains("Mui-error") || wrapperClass.contains("mat-form-field-invalid")
                        || wrapperClass.contains("ng-invalid") || wrapperClass.contains("p-invalid")) {
                    return true;
                }
            }
        }

        // Check 4: HTML5 validity
        try {
            Object valid = ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("return arguments[0].validity && !arguments[0].validity.valid;", fieldElement);
            if (Boolean.TRUE.equals(valid)) return true;
        } catch (Exception e) { /* not an input or JS failed */ }

        return false;
    }

    private WebElement findFieldWrapper(WebElement fieldElement) {
        for (String xpath : WRAPPER_SELECTORS) {
            try {
                WebElement wrapper = fieldElement.findElement(By.xpath(xpath));
                if (wrapper != null) return wrapper;
            } catch (Exception e) { /* try next */ }
        }
        // Fallback: parent div
        try {
            return fieldElement.findElement(By.xpath("./.."));
        } catch (Exception e) {
            return null;
        }
    }
}
