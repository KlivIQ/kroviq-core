package kroviq.utils;

import kroviq.model.FlexiField;
import kroviq.model.FlexiFieldType;
import kroviq.reporting.StepReportingWrapper;
import kroviq.wrapper.AntDUtils;
import kroviq.wrapper.GenericWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class FlexiFieldInteractor {

    private static final Logger logger = LogManager.getLogger(FlexiFieldInteractor.class);
    private static final int MAX_SYNC_RETRIES = 2;

    /**
     * Interact with a flexi field using auto-mapped action based on field type.
     * Pre-validates visibility and editability before performing the action.
     */
    public static void interact(FlexiField field, String value) {
        interact(field, null, value);
    }

    /**
     * Interact with a flexi field using explicit or auto-mapped action.
     * @param actionOverride if null, action is determined from field type
     */
    public static void interact(FlexiField field, String actionOverride, String value) {
        FlexiFieldType type = field.getFieldType();
        String action = actionOverride != null ? actionOverride.toLowerCase() : resolveAction(type);

        StepReportingWrapper.executeStepWithContext(
                "Interact with flexi field " + field.identity() + " -- action='" + action + "', value='" + value + "'",
                field.getFieldLabel(), field.getPageName(), value,
                () -> {
                    WebElement element = resolveAndPreValidate(field);
                    performAction(element, field, action, value);
                });
    }

    // -- Auto Action Mapping --

    private static String resolveAction(FlexiFieldType type) {
        if (type == null) return "enter";
        switch (type) {
            case DROPDOWN:    return "select";
            case TEXTBOX:     return "enter";
            case NUMBER:      return "enter";
            case TOGGLE:      return "toggle";
            case CHECKBOX:    return "check";
            case RADIO:       return "select";
            case DATE_PICKER: return "date";
            default:          return "enter";
        }
    }

    // -- Pre-Validation --

    private static WebElement resolveAndPreValidate(FlexiField field) {
        WebElement element = FlexiFieldResolver.resolveElement(field);

        if (!FlexiFieldResolver.isVisible(element)) {
            throw new RuntimeException("Flexi field " + field.identity() + " is not visible -- cannot interact");
        }
        if (!FlexiFieldResolver.isEditable(element)) {
            throw new RuntimeException("Flexi field " + field.identity() + " is not editable -- cannot interact");
        }
        return element;
    }

    // -- Action Dispatch --

    private static void performAction(WebElement element, FlexiField field, String action, String value) {
        ensureInViewport(element);

        switch (action) {
            case "select":
                if (field.getFieldType() == FlexiFieldType.RADIO) {
                    handleRadio(element, field, value);
                } else {
                    handleSelect(element, field, value);
                }
                break;
            case "enter":
                handleEnter(element, field, value);
                break;
            case "toggle":
                handleToggle(element, value);
                break;
            case "check":
                handleCheckbox(element, value);
                break;
            case "date":
                handleDate(element, field, value);
                break;
            case "click":
                handleClick(element);
                break;
            default:
                throw new RuntimeException("Unknown action '" + action + "' for flexi field " + field.identity());
        }
    }

    // -- Action Handlers --

    private static void handleSelect(WebElement element, FlexiField field, String value) {
        if (value == null || value.trim().isEmpty()) {
            logger.debug("Skipping dropdown select for {} -- empty value", field.identity());
            return;
        }

        try {
            WebElement selectWrapper = findAntSelectWrapper(element);
            selectWrapper.click(); // native click first

            AntDUtils antDUtils = new AntDUtils(GenericWrapper.getDriver(), 10);
            antDUtils.selectAntDOptionByText(value, field.getFieldLabel(), element);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Dropdown selection interrupted for " + field.identity(), e);
        }
    }

    private static void handleRadio(WebElement element, FlexiField field, String value) {
        if (value == null || value.trim().isEmpty()) {
            logger.debug("Skipping radio select for {} -- empty value", field.identity());
            return;
        }

        // Strategy 1: find radio group wrapper, then locate option by label text
        WebElement option = findRadioOption(element, value);
        if (option != null) {
            clickWithRetry(option, "radio");
            return;
        }

        throw new RuntimeException("Radio option '" + value + "' not found for " + field.identity());
    }

    private static WebElement findRadioOption(WebElement element, String value) {
        // Try ant-radio-group ancestor first
        WebElement group = findAncestor(element, "ant-radio-group");
        if (group == null) {
            // Fallback: try parent container with radio role
            group = findAncestor(element, "radio");
        }
        if (group == null) {
            // Last resort: use component-main-wrapper
            try {
                group = element.findElement(By.xpath(
                        "ancestor-or-self::div[contains(@class,'component-main-wrapper')][1]"));
            } catch (NoSuchElementException e) {
                group = element;
            }
        }

        // Search for matching option by label text within the group
        try {
            return group.findElement(By.xpath(
                    ".//label[normalize-space()='" + value + "'] | " +
                    ".//span[contains(@class,'ant-radio-wrapper')][normalize-space()='" + value + "'] | " +
                    ".//div[contains(@class,'radio-option')][normalize-space()='" + value + "']"));
        } catch (NoSuchElementException e) {
            // Fallback: case-insensitive partial match
            try {
                return group.findElement(By.xpath(
                        ".//*[contains(normalize-space(),'" + value + "') and " +
                        "(self::label or contains(@class,'radio'))]"));
            } catch (NoSuchElementException ex) {
                return null;
            }
        }
    }

    private static WebElement findAncestor(WebElement element, String classFragment) {
        try {
            return element.findElement(By.xpath(
                    "ancestor::*[contains(@class,'" + classFragment + "')][1]"));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private static void handleEnter(WebElement element, FlexiField field, String value) {
        if (value == null || value.trim().isEmpty()) {
            logger.debug("Skipping text entry for {} -- empty value", field.identity());
            return;
        }

        WebElement input = resolveInputElement(element);
        input.clear();
        input.sendKeys(value);

        // Verify value was committed
        try {
            new WebDriverWait(GenericWrapper.getDriver(), Duration.ofSeconds(2)).until(d -> {
                try {
                    String actual = input.getAttribute("value");
                    return actual != null && !actual.isEmpty();
                } catch (StaleElementReferenceException e) { return false; }
            });
        } catch (TimeoutException te) {
            logger.debug("Input value verification timed out for {}, proceeding", field.identity());
        }
    }

    private static void handleToggle(WebElement element, String value) {
        boolean targetState = "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)
                || "on".equalsIgnoreCase(value);

        if (isToggleOn(element) == targetState) return;

        clickWithRetry(element, "toggle");

        // Verify state actually changed
        try {
            new WebDriverWait(GenericWrapper.getDriver(), Duration.ofSeconds(2)).until(d -> {
                try { return isToggleOn(element) == targetState; }
                catch (StaleElementReferenceException e) { return false; }
            });
        } catch (TimeoutException te) {
            logger.debug("Toggle state verification timed out, proceeding");
        }
    }

    private static boolean isToggleOn(WebElement element) {
        return "true".equals(element.getAttribute("aria-checked"))
                || (element.getAttribute("class") != null && element.getAttribute("class").contains("ant-switch-checked"));
    }

    private static void handleCheckbox(WebElement element, String value) {
        boolean targetState = "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);

        if (isCheckboxChecked(element) == targetState) return;

        // Click the wrapper label if available (AntD checkbox pattern)
        WebElement clickTarget = element;
        try {
            clickTarget = element.findElement(By.xpath(
                    "ancestor::label[contains(@class,'ant-checkbox-wrapper')][1]"));
        } catch (NoSuchElementException e) { /* use element directly */ }

        clickWithRetry(clickTarget, "checkbox");

        // Verify state actually changed
        final WebElement cbRef = element;
        try {
            new WebDriverWait(GenericWrapper.getDriver(), Duration.ofSeconds(2)).until(d -> {
                try { return isCheckboxChecked(cbRef) == targetState; }
                catch (StaleElementReferenceException e) { return false; }
            });
        } catch (TimeoutException te) {
            logger.debug("Checkbox state verification timed out, proceeding");
        }
    }

    private static boolean isCheckboxChecked(WebElement element) {
        return "true".equals(element.getAttribute("aria-checked"))
                || element.isSelected()
                || (element.getAttribute("class") != null && element.getAttribute("class").contains("ant-checkbox-checked"));
    }

    private static void handleDate(WebElement element, FlexiField field, String value) {
        if (value == null || value.trim().isEmpty()) {
            logger.debug("Skipping date entry for {} -- empty value", field.identity());
            return;
        }

        WebElement input = resolveInputElement(element);
        DatePickerUtils.setDatePickerValue(input, value);
    }

    private static void handleClick(WebElement element) {
        clickWithRetry(element, "click");
    }

    // -- Helpers --

    private static WebElement findAntSelectWrapper(WebElement element) {
        try {
            return element.findElement(By.xpath(
                    "ancestor-or-self::*[contains(@class,'ant-select')][1]"));
        } catch (NoSuchElementException e) {
            return element;
        }
    }

    private static WebElement resolveInputElement(WebElement element) {
        String tag = element.getTagName().toLowerCase();
        if ("input".equals(tag) || "textarea".equals(tag)) return element;
        try {
            return element.findElement(By.xpath(".//input | .//textarea"));
        } catch (NoSuchElementException e) {
            return element;
        }
    }

    private static void clickWithRetry(WebElement element, String actionName) {
        for (int attempt = 1; attempt <= MAX_SYNC_RETRIES; attempt++) {
            try {
                element.click();
                return;
            } catch (ElementClickInterceptedException e) {
                JavascriptExecutor js = (JavascriptExecutor) GenericWrapper.getDriver();
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", element);
                js.executeScript("arguments[0].click();", element);
                return;
            } catch (StaleElementReferenceException e) {
                logger.debug("[Sync] Retrying action (attempt {}) for element {}: {}", attempt, actionName, e.getClass().getSimpleName());
                if (attempt == MAX_SYNC_RETRIES) throw e;
            }
        }
    }

    private static void ensureInViewport(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) GenericWrapper.getDriver();
            Boolean inView = (Boolean) js.executeScript(
                    "var r=arguments[0].getBoundingClientRect();" +
                    "return r.top>=0&&r.left>=0&&r.bottom<=window.innerHeight&&r.right<=window.innerWidth;",
                    element);
            if (!Boolean.TRUE.equals(inView)) {
                js.executeScript("arguments[0].scrollIntoView({block:'center',inline:'nearest'});", element);
            }
        } catch (Exception e) {
            logger.debug("Viewport alignment skipped: {}", e.getMessage());
        }
    }
}
