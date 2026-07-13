package kroviq.wrapper.mui;

import kroviq.utils.DatePickerUtils;
import kroviq.wrapper.core.DatePickerHandler;
import kroviq.wrapper.core.HandlerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MUIDatePickerHandler implements DatePickerHandler {

    private static final Logger logger = LogManager.getLogger(MUIDatePickerHandler.class);
    private final WebDriver driver;

    public MUIDatePickerHandler(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void selectDate(WebDriver driver, WebElement pickerElement, LocalDate date) {
        int timeout = HandlerUtils.getTimeout("mui");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));

        WebElement input = resolveInput(pickerElement);

        // Strategy 1: Direct input (works for most MUI date pickers, skip if readonly)
        if (!isReadonly(input) && setDateDirectly(driver, input, date)) {
            logger.info("[MUI DatePicker] Date set via direct input: {}", date);
            return;
        }

        // Strategy 2: Open calendar and select day
        openCalendar(driver, pickerElement, wait);
        navigateToMonth(driver, date, wait);
        clickDay(driver, date, wait);
        logger.info("[MUI DatePicker] Date selected via calendar: {}", date);
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

        List<WebElement> inputs = element.findElements(By.cssSelector("input"));
        if (!inputs.isEmpty()) return inputs.get(0);

        try {
            return element.findElement(By.xpath(".//input"));
        } catch (Exception e) {
            return element;
        }
    }

    private boolean setDateDirectly(WebDriver driver, WebElement input, LocalDate date) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String formatted = date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));

            js.executeScript(
                    "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                    "nativeInputValueSetter.call(arguments[0], arguments[1]);" +
                    "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                    "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                    input, formatted);

            String actual = input.getAttribute("value");
            return actual != null && !actual.isEmpty() && !actual.equals("mm/dd/yyyy");
        } catch (Exception e) {
            logger.debug("[MUI DatePicker] Direct input failed: {}", e.getMessage());
            return false;
        }
    }

    private void openCalendar(WebDriver driver, WebElement pickerElement, WebDriverWait wait) {
        // MUI uses an icon button to open the calendar
        List<WebElement> buttons = pickerElement.findElements(
                By.cssSelector("button[aria-label*='date'], button[aria-label*='calendar'], button.MuiIconButton-root"));
        if (!buttons.isEmpty()) {
            buttons.get(0).click();
        } else {
            resolveInput(pickerElement).click();
        }
        wait.until(d -> !d.findElements(By.cssSelector(
                ".MuiPickersPopper-root, .MuiCalendarPicker-root, [role='dialog'][class*='Mui']")).isEmpty());
    }

    private void navigateToMonth(WebDriver driver, LocalDate target, WebDriverWait wait) {
        for (int i = 0; i < 24; i++) {
            String headerText = getCalendarHeaderText(driver);
            if (headerText.contains(target.getMonth().toString().substring(0, 3)) &&
                    headerText.contains(String.valueOf(target.getYear()))) {
                return;
            }
            if (headerText.contains(String.valueOf(target.getYear())) &&
                    headerText.toLowerCase().contains(target.getMonth().toString().toLowerCase().substring(0, 3))) {
                return;
            }

            WebElement nextBtn = driver.findElement(By.cssSelector(
                    "[aria-label*='next month'], button[title*='Next month'], .MuiPickersArrowSwitcher-root button:last-child"));
            nextBtn.click();
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    private void clickDay(WebDriver driver, LocalDate date, WebDriverWait wait) {
        String day = String.valueOf(date.getDayOfMonth());
        By daySelector = By.xpath(
                "//*[contains(@class,'MuiPickersDay') or contains(@class,'MuiDayCalendar')]" +
                "//button[not(contains(@class,'disabled'))][normalize-space(text())='" + day + "']");

        List<WebElement> days = driver.findElements(daySelector);
        for (WebElement d : days) {
            try {
                if (d.isDisplayed() && d.isEnabled()) {
                    d.click();
                    return;
                }
            } catch (Exception e) { /* try next */ }
        }

        // Fallback: aria-label based
        String ariaLabel = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        By ariaSelector = By.cssSelector("button[aria-label='" + ariaLabel + "']");
        List<WebElement> ariaMatches = driver.findElements(ariaSelector);
        if (!ariaMatches.isEmpty()) {
            ariaMatches.get(0).click();
            return;
        }

        throw new RuntimeException("[MUI DatePicker] Could not find day cell for: " + date);
    }

    private String getCalendarHeaderText(WebDriver driver) {
        List<WebElement> headers = driver.findElements(By.cssSelector(
                ".MuiPickersCalendarHeader-label, .MuiPickersFadeTransitionGroup-root"));
        if (!headers.isEmpty()) return headers.get(0).getText();
        return "";
    }

    private boolean isReadonly(WebElement input) {
        String readonly = input.getAttribute("readonly");
        return readonly != null && !"false".equals(readonly);
    }
}
