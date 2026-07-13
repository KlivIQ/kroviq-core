package kroviq.utils;

import kroviq.model.FlexiField;
import kroviq.model.FlexiFieldType;
import kroviq.reporting.StepReportingWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.List;

public class FlexiFieldValidator {

    private static final Logger logger = LogManager.getLogger(FlexiFieldValidator.class);

    // ============================================
    // Existing Validators
    // ============================================

    public static void validatePresence(FlexiField field) {
        StepReportingWrapper.executeStepWithContext(
                "Validate presence of flexi field " + field.identity(),
                field.getFieldLabel(), field.getPageName(), null,
                () -> FlexiFieldResolver.resolveElement(field));
    }

    public static void validateVisibility(FlexiField field, boolean expectedVisible) {
        StepReportingWrapper.executeStepWithContext(
                "Validate flexi field " + field.identity() + " is " + (expectedVisible ? "visible" : "hidden"),
                field.getFieldLabel(), field.getPageName(), String.valueOf(expectedVisible),
                () -> {
                    WebElement element = FlexiFieldResolver.resolveElement(field);
                    boolean actual = FlexiFieldResolver.isVisible(element);
                    if (actual != expectedVisible) {
                        throw new AssertionError("Field " + field.identity()
                                + " -- expected visibility=" + expectedVisible + ", actual=" + actual);
                    }
                });
    }

    public static void validateEditability(FlexiField field, boolean expectedEditable) {
        StepReportingWrapper.executeStepWithContext(
                "Validate flexi field " + field.identity() + " is " + (expectedEditable ? "editable" : "read-only"),
                field.getFieldLabel(), field.getPageName(), String.valueOf(expectedEditable),
                () -> {
                    WebElement element = FlexiFieldResolver.resolveElement(field);
                    boolean actual = FlexiFieldResolver.isEditable(element);
                    if (actual != expectedEditable) {
                        throw new AssertionError("Field " + field.identity()
                                + " -- expected editable=" + expectedEditable + ", actual=" + actual);
                    }
                });
    }

    public static void validateFieldType(FlexiField field) {
        FlexiFieldType expected = field.getFieldType();
        if (expected == null || expected == FlexiFieldType.UNKNOWN) {
            logger.debug("Skipping type validation for {} -- no expected type set", field.identity());
            return;
        }
        StepReportingWrapper.executeStepWithContext(
                "Validate flexi field " + field.identity() + " type is " + expected,
                field.getFieldLabel(), field.getPageName(), expected.name(),
                () -> {
                    WebElement element = FlexiFieldResolver.resolveElement(field);
                    FlexiFieldType actual = FlexiFieldResolver.detectFieldType(element);
                    if (actual != expected) {
                        throw new AssertionError("Field " + field.identity()
                                + " -- expected type=" + expected + ", actual=" + actual);
                    }
                });
    }

    public static void validateValue(FlexiField field, String expectedValue) {
        StepReportingWrapper.executeStepWithContext(
                "Validate flexi field " + field.identity() + " value is '" + expectedValue + "'",
                field.getFieldLabel(), field.getPageName(), expectedValue,
                () -> {
                    WebElement element = FlexiFieldResolver.resolveElement(field);
                    String actual = extractValue(element, field.getFieldType());
                    if (expectedValue == null && actual == null) return;
                    if (expectedValue != null && expectedValue.equals(actual)) return;
                    throw new AssertionError("Field " + field.identity()
                            + " -- expected value='" + expectedValue + "', actual='" + actual + "'");
                });
    }

    // ============================================
    // Phase 3 -- Passive Validators
    // ============================================

    /**
     * Validates mandatory flag from SelectTypes against DOM indicators.
     * Checks: required attribute, required CSS class, asterisk in label.
     * Skips if selectTypes or mandatory is null.
     */
    public static void validateMandatory(FlexiField field) {
        if (field.getSelectTypes() == null || field.getSelectTypes().getMandatory() == null) {
            logger.debug("Skipping mandatory validation for {} -- mandatory config is null", field.identity());
            return;
        }
        boolean expected = field.getSelectTypes().getMandatory();
        StepReportingWrapper.executeStepWithContext(
                "Validate flexi field " + field.identity() + " mandatory=" + expected,
                field.getFieldLabel(), field.getPageName(), String.valueOf(expected),
                () -> {
                    WebElement element = FlexiFieldResolver.resolveElement(field);
                    boolean actual = isMandatoryInDom(element);
                    if (actual != expected) {
                        throw new AssertionError("Field " + field.identity()
                                + " -- expected mandatory=" + expected + ", actual=" + actual);
                    }
                });
    }

    /**
     * Validates hide flag from SelectTypes against DOM visibility.
     * hide=true -> element must NOT be visible. hide=false -> element must be visible.
     * Skips if selectTypes or hide is null.
     */
    public static void validateHidden(FlexiField field) {
        if (field.getSelectTypes() == null || field.getSelectTypes().getHide() == null) {
            logger.debug("Skipping hidden validation for {} -- hide config is null", field.identity());
            return;
        }
        boolean hideExpected = field.getSelectTypes().getHide();
        StepReportingWrapper.executeStepWithContext(
                "Validate flexi field " + field.identity() + " hide=" + hideExpected,
                field.getFieldLabel(), field.getPageName(), String.valueOf(hideExpected),
                () -> {
                    WebElement element;
                    try {
                        element = FlexiFieldResolver.resolveElement(field);
                    } catch (RuntimeException e) {
                        String msg = e.getMessage() != null ? e.getMessage() : "";
                        boolean isResolutionFailure = msg.contains("not resolvable")
                                || msg.contains("not found on page");
                        if (hideExpected && isResolutionFailure) {
                            logger.debug("Field {} not resolvable in DOM -- consistent with hide=true", field.identity());
                            return;
                        }
                        throw new AssertionError("Field " + field.identity()
                                + " -- expected hide=" + hideExpected
                                + ", but element resolution failed: " + msg);
                    }
                    boolean actualVisible = FlexiFieldResolver.isVisible(element);
                    if (hideExpected && actualVisible) {
                        throw new AssertionError("Field " + field.identity()
                                + " -- expected hide=true (not visible), actual=visible");
                    }
                    if (!hideExpected && !actualVisible) {
                        throw new AssertionError("Field " + field.identity()
                                + " -- expected hide=false (visible), actual=not visible");
                    }
                });
    }

    /**
     * Validates default value from BusinessRules against current DOM value.
     * Skips if businessRules or defaultValue is null.
     */
    public static void validateDefaultValue(FlexiField field) {
        if (field.getBusinessRules() == null || field.getBusinessRules().getDefaultValue() == null) {
            logger.debug("Skipping defaultValue validation for {} -- defaultValue config is null", field.identity());
            return;
        }
        String expected = field.getBusinessRules().getDefaultValue();
        StepReportingWrapper.executeStepWithContext(
                "Validate flexi field " + field.identity() + " defaultValue='" + expected + "'",
                field.getFieldLabel(), field.getPageName(), expected,
                () -> {
                    WebElement element = FlexiFieldResolver.resolveElement(field);
                    String actual = extractValue(element, field.getFieldType());
                    if (!expected.equals(actual)) {
                        throw new AssertionError("Field " + field.identity()
                                + " -- expected defaultValue='" + expected + "', actual='" + actual + "'");
                    }
                });
    }

    /**
     * Validates min/max from CoreConfig against DOM attributes (min, max, aria-valuemin, aria-valuemax).
     * Skips if coreConfig is null or both minimumValue and maximumValue are null.
     * Skips individual check with debug log if DOM attribute not present.
     */
    public static void validateMinMax(FlexiField field) {
        if (field.getCoreConfig() == null) {
            logger.debug("Skipping min/max validation for {} -- coreConfig is null", field.identity());
            return;
        }
        String expectedMin = field.getCoreConfig().getMinimumValue();
        String expectedMax = field.getCoreConfig().getMaximumValue();
        if (expectedMin == null && expectedMax == null) {
            logger.debug("Skipping min/max validation for {} -- both min and max are null", field.identity());
            return;
        }
        StepReportingWrapper.executeStepWithContext(
                "Validate flexi field " + field.identity() + " min/max constraints",
                field.getFieldLabel(), field.getPageName(), null,
                () -> {
                    WebElement element = FlexiFieldResolver.resolveElement(field);
                    if (expectedMin != null) {
                        String domMin = readDomAttribute(element, "min", "aria-valuemin");
                        if (domMin == null) {
                            logger.debug("Field {} -- min attribute not found in DOM, skipping min check", field.identity());
                        } else if (!expectedMin.equals(domMin)) {
                            throw new AssertionError("Field " + field.identity()
                                    + " -- expected min='" + expectedMin + "', actual DOM min='" + domMin + "'");
                        }
                    }
                    if (expectedMax != null) {
                        String domMax = readDomAttribute(element, "max", "aria-valuemax");
                        if (domMax == null) {
                            logger.debug("Field {} -- max attribute not found in DOM, skipping max check", field.identity());
                        } else if (!expectedMax.equals(domMax)) {
                            throw new AssertionError("Field " + field.identity()
                                    + " -- expected max='" + expectedMax + "', actual DOM max='" + domMax + "'");
                        }
                    }
                });
    }

    /**
     * Validates date range from CoreConfig against DOM attributes (data-date-from, data-date-to, min, max).
     * Skips if coreConfig is null or both dateRangeFrom and dateRangeTo are null.
     * Skips individual check with debug log if DOM attribute not present.
     */
    public static void validateDateRange(FlexiField field) {
        if (field.getCoreConfig() == null) {
            logger.debug("Skipping dateRange validation for {} -- coreConfig is null", field.identity());
            return;
        }
        String expectedFrom = field.getCoreConfig().getDateRangeFrom();
        String expectedTo = field.getCoreConfig().getDateRangeTo();
        if (expectedFrom == null && expectedTo == null) {
            logger.debug("Skipping dateRange validation for {} -- both from and to are null", field.identity());
            return;
        }
        StepReportingWrapper.executeStepWithContext(
                "Validate flexi field " + field.identity() + " date range constraints",
                field.getFieldLabel(), field.getPageName(), null,
                () -> {
                    WebElement element = FlexiFieldResolver.resolveElement(field);
                    if (expectedFrom != null) {
                        String domFrom = readDomAttribute(element, "data-date-from", "min");
                        if (domFrom == null) {
                            logger.debug("Field {} -- date-from attribute not found in DOM, skipping from check", field.identity());
                        } else if (!expectedFrom.equals(domFrom)) {
                            throw new AssertionError("Field " + field.identity()
                                    + " -- expected dateRangeFrom='" + expectedFrom + "', actual DOM='" + domFrom + "'");
                        }
                    }
                    if (expectedTo != null) {
                        String domTo = readDomAttribute(element, "data-date-to", "max");
                        if (domTo == null) {
                            logger.debug("Field {} -- date-to attribute not found in DOM, skipping to check", field.identity());
                        } else if (!expectedTo.equals(domTo)) {
                            throw new AssertionError("Field " + field.identity()
                                    + " -- expected dateRangeTo='" + expectedTo + "', actual DOM='" + domTo + "'");
                        }
                    }
                });
    }

    /**
     * Structural LOV/parent validation only:
     * - If parentLov is set, verifies parent field exists in same block
     * - Verifies child field (this field) is a dropdown
     * Does NOT validate dynamic dependency behavior.
     * Skips if coreConfig or parentLov is null.
     */
    public static void validateLovRelationship(FlexiField field, List<FlexiField> blockFields) {
        if (field.getCoreConfig() == null || field.getCoreConfig().getParentLov() == null) {
            logger.debug("Skipping LOV validation for {} -- parentLov is null", field.identity());
            return;
        }
        String parentLov = field.getCoreConfig().getParentLov();
        StepReportingWrapper.executeStepWithContext(
                "Validate flexi field " + field.identity() + " LOV relationship with parent '" + parentLov + "'",
                field.getFieldLabel(), field.getPageName(), parentLov,
                () -> {
                    // Structural check 1: parent field exists in same block AND is not hidden
                    FlexiField parentField = null;
                    for (FlexiField f : blockFields) {
                        boolean matchByKey = f.getFieldKey() != null && f.getFieldKey().equals(parentLov);
                        boolean matchByLabel = f.getFieldLabel() != null && f.getFieldLabel().equals(parentLov);
                        if (matchByKey || matchByLabel) {
                            parentField = f;
                            break;
                        }
                    }
                    if (parentField == null) {
                        throw new AssertionError("Field " + field.identity()
                                + " -- parentLov='" + parentLov + "' not found in block fields");
                    }
                    if (parentField.getSelectTypes() != null
                            && Boolean.TRUE.equals(parentField.getSelectTypes().getHide())) {
                        throw new AssertionError("Field " + field.identity()
                                + " -- parentLov='" + parentLov + "' exists but is configured as hidden");
                    }

                    // Structural check 2: this field should be a dropdown
                    WebElement element = FlexiFieldResolver.resolveElement(field);
                    FlexiFieldType actualType = FlexiFieldResolver.detectFieldType(element);
                    if (actualType != FlexiFieldType.DROPDOWN) {
                        throw new AssertionError("Field " + field.identity()
                                + " -- has parentLov but is not a DROPDOWN, actual type=" + actualType);
                    }
                });
    }

    /**
     * Validates that the field element resolves strictly within its declared block container.
     * Skips if blockName is null.
     */
    public static void validateBlockBelonging(FlexiField field) {
        if (field.getBlockName() == null || field.getBlockName().isEmpty()) {
            logger.debug("Skipping block belonging validation for {} -- blockName is null", field.identity());
            return;
        }
        StepReportingWrapper.executeStepWithContext(
                "Validate flexi field " + field.identity() + " belongs to block '" + field.getBlockName() + "'",
                field.getFieldLabel(), field.getPageName(), field.getBlockName(),
                () -> {
                    // resolveElement already scopes within block -- if it succeeds, field belongs to block
                    FlexiFieldResolver.resolveElement(field);
                });
    }

    // ============================================
    // Composite Validator
    // ============================================

    /**
     * Runs all applicable validators based on non-null model fields.
     * Each validator independently skips if its config is null.
     * No orchestration or conditional branching -- just calls all.
     */
    public static void validateAll(FlexiField field, List<FlexiField> blockFields) {
        logger.info("Running composite validation for {}", field.identity());

        validatePresence(field);
        validateBlockBelonging(field);
        validateFieldType(field);
        validateMandatory(field);
        validateHidden(field);
        validateDefaultValue(field);
        validateMinMax(field);
        validateDateRange(field);
        validateLovRelationship(field, blockFields);
    }

    // ============================================
    // Private Helpers
    // ============================================

    private static boolean isMandatoryInDom(WebElement element) {
        // Check 1: required attribute
        if ("true".equals(element.getAttribute("required"))
                || element.getAttribute("required") != null && !"false".equals(element.getAttribute("required"))) {
            String req = element.getAttribute("required");
            if (req != null && !"false".equalsIgnoreCase(req)) return true;
        }

        // Check 2: aria-required
        if ("true".equals(element.getAttribute("aria-required"))) return true;

        // Check 3: required CSS class on element or wrapper
        String elementClass = element.getAttribute("class");
        if (elementClass == null) elementClass = "";
        WebElement wrapper = findComponentWrapper(element);
        String wrapperClass = wrapper != null ? wrapper.getAttribute("class") : "";
        String combined = elementClass + " " + wrapperClass;
        if (combined.contains("ant-form-item-required") || combined.contains("required")) return true;

        // Check 4: asterisk in associated label
        if (wrapper != null) {
            try {
                WebElement label = wrapper.findElement(By.cssSelector("label"));
                String labelClass = label.getAttribute("class");
                if (labelClass != null && labelClass.contains("ant-form-item-required")) return true;
                String labelText = label.getText();
                if (labelText != null && labelText.contains("*")) return true;
            } catch (NoSuchElementException e) { /* no label */ }
        }

        return false;
    }

    /**
     * Reads a DOM attribute, trying primary then fallback attribute name.
     * Returns null if neither is present.
     */
    private static String readDomAttribute(WebElement element, String primary, String fallback) {
        String value = element.getAttribute(primary);
        if (value != null && !value.isEmpty()) return value;
        value = element.getAttribute(fallback);
        if (value != null && !value.isEmpty()) return value;
        return null;
    }

    private static WebElement findComponentWrapper(WebElement element) {
        try {
            return element.findElement(By.xpath(
                    "ancestor-or-self::div[contains(@class,'component-main-wrapper')][1]"));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    // -- Value Extraction --

    public static String extractValue(WebElement element, FlexiFieldType type) {
        try {
            if (type == null) type = FlexiFieldResolver.detectFieldType(element);

            switch (type) {
                case DROPDOWN:
                    return getDropdownValue(element);
                case TOGGLE:
                    return isToggleOn(element) ? "true" : "false";
                case CHECKBOX:
                    return isChecked(element) ? "true" : "false";
                case RADIO:
                    return getSelectedRadioText(element);
                case DATE_PICKER:
                case TEXTBOX:
                case NUMBER:
                    return element.getAttribute("value");
                default:
                    return element.getText();
            }
        } catch (Exception e) {
            logger.debug("Value extraction failed for {}: {}", type, e.getMessage());
            return null;
        }
    }

    private static String getDropdownValue(WebElement element) {
        try {
            WebElement selection = element.findElement(
                    By.cssSelector(".ant-select-selection-item"));
            return selection.getText().trim();
        } catch (Exception e) {
            return element.getText().trim();
        }
    }

    private static boolean isToggleOn(WebElement element) {
        String cls = element.getAttribute("class");
        if (cls != null && cls.contains("ant-switch-checked")) return true;
        return "true".equals(element.getAttribute("aria-checked"));
    }

    private static boolean isChecked(WebElement element) {
        String cls = element.getAttribute("class");
        if (cls != null && cls.contains("ant-checkbox-checked")) return true;
        return "true".equals(element.getAttribute("aria-checked"))
                || element.getAttribute("checked") != null;
    }

    private static String getSelectedRadioText(WebElement element) {
        try {
            WebElement group = element.findElement(
                    By.xpath("ancestor::*[contains(@class,'ant-radio-group')][1]"));
            WebElement checked = group.findElement(
                    By.cssSelector(".ant-radio-wrapper-checked"));
            return checked.getText().trim();
        } catch (Exception e) { /* try next */ }

        try {
            WebElement container = element.findElement(
                    By.xpath("ancestor::*[contains(@class,'component-main-wrapper')][1]"));
            WebElement checked = container.findElement(
                    By.xpath(".//*[@aria-checked='true']/ancestor-or-self::label[1]"));
            return checked.getText().trim();
        } catch (Exception e) { /* fallback */ }

        return element.getText().trim();
    }
}
