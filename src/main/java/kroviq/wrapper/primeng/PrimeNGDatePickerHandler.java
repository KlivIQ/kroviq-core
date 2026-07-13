package kroviq.wrapper.primeng;

import kroviq.utils.DatePickerUtils;
import kroviq.wrapper.core.DatePickerHandler;
import kroviq.wrapper.core.HandlerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PrimeNGDatePickerHandler implements DatePickerHandler {

    private static final Logger logger = LogManager.getLogger(PrimeNGDatePickerHandler.class);
    private final WebDriver driver;

    public PrimeNGDatePickerHandler(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void selectDate(WebDriver driver, WebElement pickerElement, LocalDate date) {
        int timeout = HandlerUtils.getTimeout("primeng");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));

        WebElement input = resolveInput(pickerElement);

        // Strategy 1: Direct input (skip if readonly)
        if (!isReadonly(input) && setDateDirectly(driver, input, date)) {
            logger.info("[PrimeNG Calendar] Date set via direct input: {}", date);
            return;
        }

        // Strategy 2: Open calendar panel and select day
        openCalendar(driver, pickerElement, wait);
        navigateToMonth(driver, date, wait);
        clickDay(driver, date, wait);
        logger.info("[PrimeNG Calendar] Date selected via calendar: {}", date);
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

        List<WebElement> inputs = element.findElements(By.cssSelector("input.p-inputtext, input"));
        if (!inputs.isEmpty()) return inputs.get(0);

        return element;
    }

    private boolean setDateDirectly(WebDriver driver, WebElement input, LocalDate date) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String formatted = date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));

            input.clear();
            input.sendKeys(formatted);

            js.executeScript(
                    "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                    "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                    "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));",
                    input);

            String actual = input.getAttribute("value");
            return actual != null && !actual.isEmpty();
        } catch (Exception e) {
            logger.debug("[PrimeNG Calendar] Direct input failed: {}", e.getMessage());
            return false;
        }
    }

    private void openCalendar(WebDriver driver, WebElement pickerElement, WebDriverWait wait) {
        // PrimeNG uses a button icon or clicking the input
        List<WebElement> buttons = pickerElement.findElements(
                By.cssSelector("button.p-datepicker-trigger, .p-calendar-button, button[aria-label*='calendar']"));
        if (!buttons.isEmpty()) {
            buttons.get(0).click();
        } else {
            resolveInput(pickerElement).click();
        }
        wait.until(d -> !d.findElements(By.cssSelector(
                ".p-datepicker, .p-datepicker-panel")).isEmpty());
    }

    private void navigateToMonth(WebDriver driver, LocalDate target, WebDriverWait wait) {
        for (int i = 0; i < 24; i++) {
            String headerText = getCalendarHeaderText(driver);
            if (containsMonthYear(headerText, target)) return;

            List<WebElement> nextBtns = driver.findElements(By.cssSelector(
                    ".p-datepicker-next, button[aria-label='Next Month']"));
            if (!nextBtns.isEmpty()) {
                nextBtns.get(0).click();
                try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            } else {
                break;
            }
        }
    }

    private void clickDay(WebDriver driver, LocalDate date, WebDriverWait wait) {
        String day = String.valueOf(date.getDayOfMonth());

        // PrimeNG day cells: td with span containing day number
        By daySelector = By.xpath(
                "//*[contains(@class,'p-datepicker')]//td[not(contains(@class,'p-datepicker-other-month')) and not(contains(@class,'p-disabled'))]" +
                "//span[normalize-space(text())='" + day + "']");

        List<WebElement> days = driver.findElements(daySelector);
        for (WebElement d : days) {
            if (d.isDisplayed()) {
                d.click();
                return;
            }
        }

        // Fallback: data-date attribute
        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        By dataSelector = By.cssSelector("[data-date='" + dateStr + "']");
        List<WebElement> dataMatches = driver.findElements(dataSelector);
        if (!dataMatches.isEmpty()) {
            dataMatches.get(0).click();
            return;
        }

        throw new RuntimeException("[PrimeNG Calendar] Could not find day cell for: " + date);
    }

    private String getCalendarHeaderText(WebDriver driver) {
        List<WebElement> headers = driver.findElements(By.cssSelector(
                ".p-datepicker-title, .p-datepicker-month, .p-datepicker-year"));
        StringBuilder sb = new StringBuilder();
        for (WebElement h : headers) {
            sb.append(h.getText()).append(" ");
        }
        return sb.toString().trim();
    }

    private boolean containsMonthYear(String text, LocalDate date) {
        String month = date.getMonth().toString();
        String textLower = text.toLowerCase();
        return (textLower.contains(month.toLowerCase()) || textLower.contains(month.toLowerCase().substring(0, 3)))
                && text.contains(String.valueOf(date.getYear()));
    }

    private boolean isReadonly(WebElement input) {
        String readonly = input.getAttribute("readonly");
        return readonly != null && !"false".equals(readonly);
    }
}
