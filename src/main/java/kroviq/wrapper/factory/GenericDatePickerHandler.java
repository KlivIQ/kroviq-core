package kroviq.wrapper.factory;

import kroviq.utils.DatePickerUtils;
import kroviq.wrapper.core.DatePickerHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GenericDatePickerHandler implements DatePickerHandler {

    private static final Logger logger = LogManager.getLogger(GenericDatePickerHandler.class);
    private final WebDriver driver;

    public GenericDatePickerHandler(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void selectDate(WebDriver driver, WebElement pickerElement, LocalDate date) {
        WebElement input = resolveInput(pickerElement);
        String inputType = input.getAttribute("type");

        // Strategy 1: HTML5 date input — sendKeys with ISO format
        if ("date".equalsIgnoreCase(inputType)) {
            setHtml5DateInput(driver, input, date);
            logger.info("[Generic DatePicker] Date set via HTML5 date input: {}", date);
            return;
        }

        // Strategy 2: JS value set with event dispatch (works for most custom date inputs)
        if (setDateViaJs(driver, input, date)) {
            logger.info("[Generic DatePicker] Date set via JS: {}", date);
            return;
        }

        // Strategy 3: Clear and type formatted date
        setDateViaKeyboard(input, date);
        logger.info("[Generic DatePicker] Date set via keyboard: {}", date);
    }

    @Override
    public void setDateValue(WebDriver driver, WebElement pickerElement, String dateString) {
        LocalDate date = DatePickerUtils.parseDate(dateString);
        selectDate(driver, pickerElement, date);
    }

    @Override
    public String getCurrentValue(WebDriver driver, WebElement pickerElement) {
        WebElement input = resolveInput(pickerElement);
        String value = input.getAttribute("value");
        return value != null ? value.trim() : "";
    }

    private WebElement resolveInput(WebElement element) {
        String tagName = element.getTagName().toLowerCase();
        if ("input".equals(tagName)) return element;

        List<WebElement> inputs = element.findElements(By.cssSelector("input[type='date'], input[type='text'], input"));
        if (!inputs.isEmpty()) return inputs.get(0);

        return element;
    }

    private void setHtml5DateInput(WebDriver driver, WebElement input, LocalDate date) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String isoDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

        js.executeScript(
                "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "nativeInputValueSetter.call(arguments[0], arguments[1]);" +
                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                input, isoDate);
    }

    private boolean setDateViaJs(WebDriver driver, WebElement input, LocalDate date) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String formatted = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            js.executeScript("arguments[0].value = arguments[1];", input, formatted);
            js.executeScript(
                    "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                    "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                    "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));",
                    input);

            String actual = input.getAttribute("value");
            return actual != null && !actual.isEmpty() && actual.equals(formatted);
        } catch (Exception e) {
            logger.debug("[Generic DatePicker] JS value set failed: {}", e.getMessage());
            return false;
        }
    }

    private void setDateViaKeyboard(WebElement input, LocalDate date) {
        String formatted = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(formatted);
        input.sendKeys(Keys.TAB);
    }
}
