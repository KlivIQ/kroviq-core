package kroviq.wrapper.factory;

import kroviq.utils.DatePickerUtils;
import kroviq.wrapper.core.DateTimeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class GenericDateTimeHandler implements DateTimeHandler {

    private static final Logger logger = LogManager.getLogger(GenericDateTimeHandler.class);
    private final WebDriver driver;

    private static final DateTimeFormatter[] DATETIME_FORMATS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    };

    public GenericDateTimeHandler(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void selectDateTime(WebDriver driver, WebElement pickerElement, LocalDateTime dateTime) {
        WebElement input = resolveInput(pickerElement);
        String inputType = input.getAttribute("type");

        // Strategy 1: HTML5 datetime-local input
        if ("datetime-local".equalsIgnoreCase(inputType)) {
            setHtml5DateTimeInput(driver, input, dateTime);
            logger.info("[DateTime] Set via HTML5 datetime-local: {}", dateTime);
            return;
        }

        // Strategy 2: JS value set with formatted string
        if (setDateTimeViaJs(driver, input, dateTime)) {
            logger.info("[DateTime] Set via JS: {}", dateTime);
            return;
        }

        // Strategy 3: Keyboard input
        setDateTimeViaKeyboard(input, dateTime);
        logger.info("[DateTime] Set via keyboard: {}", dateTime);
    }

    @Override
    public void setDateTimeValue(WebDriver driver, WebElement pickerElement, String dateTimeString) {
        LocalDateTime dateTime = parseDateTime(dateTimeString);
        selectDateTime(driver, pickerElement, dateTime);
    }

    @Override
    public String getCurrentValue(WebDriver driver, WebElement pickerElement) {
        WebElement input = resolveInput(pickerElement);
        String value = input.getAttribute("value");
        return value != null ? value.trim() : "";
    }

    public static LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            throw new IllegalArgumentException("DateTime string cannot be null or empty");
        }
        String trimmed = dateTimeString.trim();

        for (DateTimeFormatter fmt : DATETIME_FORMATS) {
            try {
                return LocalDateTime.parse(trimmed, fmt);
            } catch (DateTimeParseException e) { /* try next */ }
        }

        // Fallback: try parsing date-only and default time to 00:00
        try {
            LocalDate dateOnly = DatePickerUtils.parseDate(trimmed);
            return dateOnly.atStartOfDay();
        } catch (Exception e) { /* not a date-only */ }

        throw new RuntimeException("Could not parse datetime: " + dateTimeString);
    }

    private WebElement resolveInput(WebElement element) {
        String tagName = element.getTagName().toLowerCase();
        if ("input".equals(tagName)) return element;
        List<WebElement> inputs = element.findElements(By.cssSelector(
                "input[type='datetime-local'], input[type='text'], input"));
        if (!inputs.isEmpty()) return inputs.get(0);
        return element;
    }

    private void setHtml5DateTimeInput(WebDriver driver, WebElement input, LocalDateTime dateTime) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String isoDateTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        js.executeScript(
                "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "nativeInputValueSetter.call(arguments[0], arguments[1]);" +
                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                input, isoDateTime);
    }

    private boolean setDateTimeViaJs(WebDriver driver, WebElement input, LocalDateTime dateTime) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String formatted = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            js.executeScript("arguments[0].value = arguments[1];", input, formatted);
            js.executeScript(
                    "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                    "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                    "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));",
                    input);
            String actual = input.getAttribute("value");
            return actual != null && !actual.isEmpty() && actual.equals(formatted);
        } catch (Exception e) {
            logger.debug("[DateTime] JS value set failed: {}", e.getMessage());
            return false;
        }
    }

    private void setDateTimeViaKeyboard(WebElement input, LocalDateTime dateTime) {
        String formatted = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(formatted);
        input.sendKeys(Keys.TAB);
    }
}
