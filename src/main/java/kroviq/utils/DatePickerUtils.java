package kroviq.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import kroviq.wrapper.GenericWrapper;
import kroviq.reporting.StepReportingWrapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class DatePickerUtils {
    
    private static final Logger logger = LogManager.getLogger(DatePickerUtils.class);
    
    private static final List<DateTimeFormatter> INPUT_FORMATS = Arrays.asList(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy")
    );
    
    public static boolean isDatePickerElement(WebElement element) {
        logger.warn("DOM-based DatePicker detection called - this should only be used for confirmation, not classification");
        try {
            String className = element.getAttribute("class");
            String readonly = element.getAttribute("readonly");
            String id = element.getAttribute("id");
            
            boolean isDatePicker = (className != null && className.contains("ant-picker")) ||
                                 (readonly != null && readonly.equals("true")) ||
                                 (id != null && (id.contains("datePicker") || id.contains("DatePicker")));
            
            logger.debug("DatePicker detection: class='{}', readonly='{}', id='{}', result={}", 
                className, readonly, id, isDatePicker);
            
            return isDatePicker;
        } catch (Exception e) {
            logger.debug("DatePicker detection failed: {}", e.getMessage());
            return false;
        }
    }
    
    public static void setDatePickerValue(WebElement element, String dateValue) {
        try {
            logger.info("Setting DatePicker with date: {}", dateValue);
            LocalDate date = parseDate(dateValue);
            
            // Strategy 1: Direct input (FAST)
            if (setDateDirectly(element, date)) {
                logger.info("Date set directly: {}", dateValue);
                StepReportingWrapper.recordManualStep("Successfully set date '" + dateValue + "' using direct input", "INFO");
                return;
            }
            
            // Strategy 2: Calendar fallback (RELIABLE)
            logger.warn("Direct input failed, using calendar fallback");
            WebDriver driver = GenericWrapper.getDriver();
            if (selectFromCalendar(element, date, driver)) {
                StepReportingWrapper.recordManualStep("Successfully selected date '" + dateValue + "' using calendar picker", "INFO");
                return;
            }
            
            throw new RuntimeException("Both direct input and calendar selection failed");
            
        } catch (Exception e) {
            StepReportingWrapper.recordManualStep("Failed to set DatePicker value '" + dateValue + "': " + e.getMessage(), "FAIL");
            logger.error("Failed to set DatePicker value: {}", e.getMessage());
            throw new RuntimeException("DatePicker interaction failed: " + e.getMessage());
        }
    }
    
    public static LocalDate parseDate(String dateValue) {
        String trimmedDate = dateValue.trim();
        
        // Handle Date object string format: "Mon Apr 07 00:00:00 IST 2025"
        if (trimmedDate.contains(" ") && trimmedDate.contains(":")) {
            try {
                // Extract year, month, day from Date string
                String[] parts = trimmedDate.split(" ");
                if (parts.length >= 6) {
                    String monthStr = parts[1];
                    String dayStr = parts[2];
                    String yearStr = parts[5];
                    
                    int month = getMonthNumber(monthStr);
                    int day = Integer.parseInt(dayStr);
                    int year = Integer.parseInt(yearStr);
                    
                    return LocalDate.of(year, month, day);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse Date object format: {}", e.getMessage());
            }
        }
        
        // Try standard formats
        for (DateTimeFormatter formatter : INPUT_FORMATS) {
            try {
                return LocalDate.parse(trimmedDate, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        throw new RuntimeException("Could not parse date: " + dateValue);
    }
    
    private static int getMonthNumber(String monthStr) {
        switch (monthStr.toLowerCase()) {
            case "jan": return 1;
            case "feb": return 2;
            case "mar": return 3;
            case "apr": return 4;
            case "may": return 5;
            case "jun": return 6;
            case "jul": return 7;
            case "aug": return 8;
            case "sep": return 9;
            case "oct": return 10;
            case "nov": return 11;
            case "dec": return 12;
            default: throw new RuntimeException("Unknown month: " + monthStr);
        }
    }
    
    private static boolean selectFromCalendar(WebElement element, LocalDate date, WebDriver driver) {
        try {
            element.click();
            
            if (!waitForCalendarPopup(driver)) {
                return false;
            }
            
            // Navigate to correct month/year if needed
            navigateToDate(date, driver);
            
            // Select the date cell
            String dayXPath = String.format("//td[@title='%s']//div[text()='%d'] | //td[contains(@class,'ant-picker-cell')]//div[text()='%d']", 
                date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), date.getDayOfMonth(), date.getDayOfMonth());
            
            WebElement dayCell = driver.findElement(By.xpath(dayXPath));
            dayCell.click();
            
            // Wait for input value to be populated after day selection
            try {
                new WebDriverWait(driver, Duration.ofSeconds(2)).until(d -> {
                    String v = element.getAttribute("value");
                    return v != null && !v.isEmpty();
                });
            } catch (TimeoutException te) {
                logger.debug("Calendar day selection value not populated within 2s");
            }
            
            // Verify selection
            String actualValue = element.getAttribute("value");
            if (actualValue != null && !actualValue.isEmpty()) {
                logger.info("Calendar selection successful: {}", actualValue);
                return true;
            }
            
        } catch (Exception e) {
            logger.warn("Calendar selection failed: {}", e.getMessage());
        }
        return false;
    }
    
    private static boolean setDateDirectly(WebElement pickerElement, LocalDate date) {
        try {
            WebDriver driver = GenericWrapper.getDriver();
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Find input field (generic selector)
            WebElement input = pickerElement.findElement(By.xpath(".//input"));
            
            // Wait for input value attribute to exist (React bound)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(300));
            wait.until(d -> input.getAttribute("value") != null);
            
            // Capture previous value before change
            String previousValue = input.getAttribute("value");
            
            // Format date
            String formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            
            // Set value directly
            js.executeScript("arguments[0].value = arguments[1];", input, formattedDate);
            
            // Dispatch events
            js.executeScript(
                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));",
                input
            );
            
            // Wait up to 500ms for React to update the value
            WebDriverWait changeWait = new WebDriverWait(driver, Duration.ofMillis(500));
            try {
                changeWait.until(d -> {
                    String currentValue = input.getAttribute("value");
                    return currentValue != null && 
                           !currentValue.isEmpty() && 
                           !currentValue.equals(previousValue);
                });
            } catch (TimeoutException e) {
                logger.warn("React did not update value within 500ms. Previous: '{}', Current: '{}'", 
                    previousValue, input.getAttribute("value"));
                return false;
            }
            
            // Get the updated value
            String actualValue = input.getAttribute("value");
            
            // Define expected formats
            String[] expectedFormats = {
                date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                date.format(DateTimeFormatter.ofPattern("d/M/yyyy")),
                date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            };
            
            // Strict comparison - must match one of the expected formats
            for (String expected : expectedFormats) {
                if (expected.equals(actualValue)) {
                    logger.debug("Date value verified: expected='{}', actual='{}'", expected, actualValue);
                    return true;
                }
            }
            
            logger.warn("Date verification failed: expected one of {}, got '{}'", 
                Arrays.toString(expectedFormats), actualValue);
            return false;
            
        } catch (Exception e) {
            logger.debug("Direct date input failed: {}", e.getMessage());
            return false;
        }
    }
    
    private static boolean waitForCalendarPopup(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement popup = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class,'ant-picker-dropdown')] | //div[contains(@class,'ant-calendar')]"))
            );
            // Wait for popup to be fully visible (not just present in DOM)
            new WebDriverWait(driver, Duration.ofSeconds(2)).until(ExpectedConditions.visibilityOf(popup));
            return true;
        } catch (Exception e) {
            logger.warn("Calendar popup not detected: {}", e.getMessage());
            return false;
        }
    }
    
    private static void navigateToDate(LocalDate targetDate, WebDriver driver) {
        try {
            // Fast navigation: Click header to open year picker
            WebElement header = driver.findElement(By.xpath(
                "//div[contains(@class,'ant-picker-header-view')]"
            ));
            header.click();
            waitForPickerContent(driver, "ant-picker-year-panel");
            
            // Select target year
            String targetYear = String.valueOf(targetDate.getYear());
            WebElement yearCell = driver.findElement(By.xpath(
                "//td[contains(@class,'ant-picker-cell')]//div[text()='" + targetYear + "']"
            ));
            yearCell.click();
            waitForPickerContent(driver, "ant-picker-month-panel");
            
            // Select target month
            String targetMonth = targetDate.getMonth().toString().substring(0, 3);
            targetMonth = targetMonth.substring(0, 1).toUpperCase() + targetMonth.substring(1).toLowerCase();
            WebElement monthCell = driver.findElement(By.xpath(
                "//td[contains(@class,'ant-picker-cell')]//div[text()='" + targetMonth + "']"
            ));
            monthCell.click();
            waitForPickerContent(driver, "ant-picker-date-panel");
            
        } catch (Exception e) {
            logger.warn("Fast navigation failed, falling back to month-by-month: {}", e.getMessage());
            navigateToDateLegacy(targetDate, driver);
        }
    }
    
    private static void navigateToDateLegacy(LocalDate targetDate, WebDriver driver) {
        try {
            for (int i = 0; i < 12; i++) {
                try {
                    WebElement header = driver.findElement(By.xpath(
                        "//div[contains(@class,'ant-picker-header')] | //div[contains(@class,'ant-calendar-header')]"
                    ));
                    
                    String headerText = header.getText();
                    if (headerText.contains(targetDate.getMonth().toString()) && 
                        headerText.contains(String.valueOf(targetDate.getYear()))) {
                        break;
                    }
                    
                    WebElement nextBtn = driver.findElement(By.xpath(
                        "//button[contains(@class,'ant-picker-header-next-btn')] | " +
                        "//a[contains(@class,'ant-calendar-next-month-btn')]"
                    ));
                    nextBtn.click();
                    // Wait for header text to change (month navigated)
                    final String headerBefore = headerText;
                    try {
                        new WebDriverWait(driver, Duration.ofSeconds(2)).until(d -> {
                            try {
                                String current = d.findElement(By.xpath(
                                    "//div[contains(@class,'ant-picker-header')] | //div[contains(@class,'ant-calendar-header')]"
                                )).getText();
                                return !current.equals(headerBefore);
                            } catch (Exception ex) { return false; }
                        });
                    } catch (TimeoutException te) { /* proceed */ }
                    
                } catch (Exception e) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("Date navigation failed: {}", e.getMessage());
        }
    }
    
    private static void waitForPickerContent(WebDriver driver, String panelClassFragment) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2)).until(d ->
                !d.findElements(By.cssSelector("div[class*='" + panelClassFragment + "']")).isEmpty()
            );
        } catch (TimeoutException e) {
            // best-effort
        }
    }
    
    public static String convertDateFormat(String inputDate) {
        if (inputDate == null || inputDate.trim().isEmpty()) {
            return inputDate;
        }
        
        try {
            LocalDate date = parseDate(inputDate);
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            logger.warn("Could not parse date format: '{}', using as-is", inputDate);
            return inputDate;
        }
    }
}