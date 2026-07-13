package kroviq.wrapper.angularmaterial;

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

public class AngularMaterialDatePickerHandler implements DatePickerHandler {

    private static final Logger logger = LogManager.getLogger(AngularMaterialDatePickerHandler.class);
    private final WebDriver driver;

    public AngularMaterialDatePickerHandler(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void selectDate(WebDriver driver, WebElement pickerElement, LocalDate date) {
        int timeout = HandlerUtils.getTimeout("angularmaterial");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));

        WebElement input = resolveInput(pickerElement);

        // Strategy 1: Direct input (skip if readonly)
        if (!isReadonly(input) && setDateDirectly(driver, input, date)) {
            logger.info("[Angular Material DatePicker] Date set via direct input: {}", date);
            return;
        }

        // Strategy 2: Open calendar overlay and select day
        openCalendar(driver, pickerElement, wait);
        navigateToMonth(driver, date, wait);
        clickDay(driver, date, wait);
        logger.info("[Angular Material DatePicker] Date selected via calendar: {}", date);
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

        List<WebElement> inputs = element.findElements(By.cssSelector("input[matInput], input[matDatepicker], input"));
        if (!inputs.isEmpty()) return inputs.get(0);

        return element;
    }

    private boolean setDateDirectly(WebDriver driver, WebElement input, LocalDate date) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String formatted = date.format(DateTimeFormatter.ofPattern("M/d/yyyy"));

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
            logger.debug("[Angular Material DatePicker] Direct input failed: {}", e.getMessage());
            return false;
        }
    }

    private void openCalendar(WebDriver driver, WebElement pickerElement, WebDriverWait wait) {
        // Angular Material uses mat-datepicker-toggle button
        List<WebElement> toggles = pickerElement.findElements(
                By.cssSelector("mat-datepicker-toggle button, [matDatepickerToggle] button, button[aria-label*='calendar']"));

        if (toggles.isEmpty()) {
            // Try sibling toggle
            try {
                WebElement parent = pickerElement.findElement(By.xpath("./.."));
                toggles = parent.findElements(By.cssSelector("mat-datepicker-toggle button"));
            } catch (Exception e) { /* fallback to click input */ }
        }

        if (!toggles.isEmpty()) {
            toggles.get(0).click();
        } else {
            resolveInput(pickerElement).click();
        }

        wait.until(d -> !d.findElements(By.cssSelector(
                "mat-datepicker-content, .mat-datepicker-content, .cdk-overlay-pane mat-calendar")).isEmpty());
    }

    private void navigateToMonth(WebDriver driver, LocalDate target, WebDriverWait wait) {
        // Click period button to switch to year view for fast navigation
        List<WebElement> periodBtns = driver.findElements(By.cssSelector(
                ".mat-calendar-period-button, button[aria-label*='Choose month and year']"));
        if (!periodBtns.isEmpty()) {
            periodBtns.get(0).click();
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            // Select year
            By yearCell = By.xpath("//*[contains(@class,'mat-calendar-body-cell')]" +
                    "[normalize-space(.)='" + target.getYear() + "']");
            List<WebElement> years = driver.findElements(yearCell);
            if (!years.isEmpty()) {
                years.get(0).click();
                try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }

            // Select month
            String monthAbbr = target.getMonth().toString().substring(0, 3);
            monthAbbr = monthAbbr.substring(0, 1).toUpperCase() + monthAbbr.substring(1).toLowerCase();
            By monthCell = By.xpath("//*[contains(@class,'mat-calendar-body-cell')]" +
                    "[contains(normalize-space(.),'" + monthAbbr + "')]");
            List<WebElement> months = driver.findElements(monthCell);
            if (!months.isEmpty()) {
                months.get(0).click();
                try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return;
            }
        }

        // Fallback: sequential month navigation
        for (int i = 0; i < 24; i++) {
            String headerText = getCalendarHeaderText(driver);
            if (containsMonthYear(headerText, target)) return;

            List<WebElement> nextBtns = driver.findElements(By.cssSelector(
                    ".mat-calendar-next-button, button[aria-label*='Next month']"));
            if (!nextBtns.isEmpty()) {
                nextBtns.get(0).click();
                try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    private void clickDay(WebDriver driver, LocalDate date, WebDriverWait wait) {
        String day = String.valueOf(date.getDayOfMonth());

        // mat-calendar-body-cell with aria-label
        String ariaLabel = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        By ariaSelector = By.cssSelector("[aria-label='" + ariaLabel + "']");
        List<WebElement> ariaMatches = driver.findElements(ariaSelector);
        if (!ariaMatches.isEmpty()) {
            ariaMatches.get(0).click();
            return;
        }

        // Fallback: text-based day selection
        By daySelector = By.xpath("//*[contains(@class,'mat-calendar-body-cell') and not(contains(@class,'disabled'))]" +
                "[normalize-space(.//*[contains(@class,'mat-calendar-body-cell-content')])='" + day + "']");
        List<WebElement> days = driver.findElements(daySelector);
        for (WebElement d : days) {
            if (d.isDisplayed()) {
                d.click();
                return;
            }
        }

        throw new RuntimeException("[Angular Material DatePicker] Could not find day cell for: " + date);
    }

    private String getCalendarHeaderText(WebDriver driver) {
        List<WebElement> labels = driver.findElements(By.cssSelector(
                ".mat-calendar-period-button, .mat-calendar-header button span"));
        if (!labels.isEmpty()) return labels.get(0).getText();
        return "";
    }

    private boolean containsMonthYear(String text, LocalDate date) {
        String month = date.getMonth().toString();
        String monthLower = month.toLowerCase();
        String textLower = text.toLowerCase();
        return (textLower.contains(monthLower) || textLower.contains(monthLower.substring(0, 3)))
                && text.contains(String.valueOf(date.getYear()));
    }

    private boolean isReadonly(WebElement input) {
        String readonly = input.getAttribute("readonly");
        return readonly != null && !"false".equals(readonly);
    }
}
