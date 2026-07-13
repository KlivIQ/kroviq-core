package kroviq.wrapper;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AntDDatePicker {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private static final By DROPDOWNS = By.cssSelector("div.ant-picker-dropdown");
    private static final By NEXT_MONTH = By.cssSelector(".ant-picker-header-next-btn");
    private static final By PREV_MONTH = By.cssSelector(".ant-picker-header-prev-btn");
    private static final By INPUT_IN_WRAPPER = By.cssSelector(".ant-picker-input > input");

    private static final DateTimeFormatter CELL_KEY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public AntDDatePicker(WebDriver driver, int timeoutSeconds) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }

    /** pickerWrapper can point to either the .ant-picker wrapper or any child (even the <input>). */
    public void selectDate(By pickerWrapper, LocalDate date) {
        System.out.println("AntDDatePicker - Setting date: " + date);

        // Resolve the .ant-picker wrapper for the given locator
        WebElement wrapper = resolveWrapper(
            wait.until(ExpectedConditions.elementToBeClickable(pickerWrapper))
        );

        // Open and grab the NEWLY created dropdown panel that belongs to THIS open
        WebElement dropdown = openPanelAndGetNewDropdown(wrapper);

        // Work strictly inside this dropdown (prevents "clicking the wrong panel")
        goToMonth(dropdown, date);
        clickDay(dropdown, date);

        // Verify the bound input received the value
        WebElement input = wrapper.findElement(INPUT_IN_WRAPPER);
        waitUntilValuePopulated(input);

        String actual   = input.getAttribute("value");
        String expected = date.format(DISPLAY);
        System.out.println("AntDDatePicker - Final field value: " + actual);

        if (!expected.equals(actual) && !actual.startsWith(expected)) {
            System.out.printf("[WARN] Display mismatch. Expected: %s, Got: %s%n", expected, actual);
        }
    }

    // ----------------- Internals -----------------

    /**
     * Open the given wrapper and return the NEW dropdown instance that appears as a result.
     * This avoids stale/previous panels that AntD leaves in the DOM.
     */
    private WebElement openPanelAndGetNewDropdown(WebElement wrapper) {
        int before = driver.findElements(DROPDOWNS).size();

        // Bring into view to avoid intercepted click
        try {
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'})", wrapper
            );
        } catch (Exception ignored) {}

        wrapper.click();

        // Wait until a NEW dropdown is added
        wait.until(d -> driver.findElements(DROPDOWNS).size() > before);

        List<WebElement> all = driver.findElements(DROPDOWNS);
        WebElement dropdown = all.get(all.size() - 1);
        return wait.until(ExpectedConditions.visibilityOf(dropdown));
    }

    private WebElement resolveWrapper(WebElement node) {
        if (hasClass(node, "ant-picker")) return node;

        // climb up (input -> wrapper)
        try {
            WebElement up = node.findElement(By.xpath("./ancestor::div[contains(@class,'ant-picker')]"));
            return wait.until(ExpectedConditions.visibilityOf(up));
        } catch (NoSuchElementException ignored) {}

        // or search down (container -> wrapper)
        List<WebElement> downs = node.findElements(By.cssSelector("div.ant-picker"));
        if (!downs.isEmpty()) return wait.until(ExpectedConditions.visibilityOf(downs.get(0)));

        throw new NoSuchElementException("Could not resolve .ant-picker wrapper from given locator");
    }

    private void goToMonth(WebElement dropdown, LocalDate target) {
        if (isDayPresent(dropdown, target)) return;

        // Try fast navigation first
        try {
            goToMonthFast(dropdown, target);
            return;
        } catch (Exception e) {
            System.out.println("Fast navigation failed, using month-by-month: " + e.getMessage());
        }

        // Fallback: bounded loop to avoid infinite navigation
        goToMonthLegacy(dropdown, target);
    }

    /** Fast navigation: Click year button, then month button to select directly */
    private void goToMonthFast(WebElement dropdown, LocalDate target) {
        // Click year button to open year picker
        WebElement yearButton = dropdown.findElement(By.cssSelector(".ant-picker-year-btn"));
        yearButton.click();
        waitForPickerPanelChange(dropdown, "ant-picker-year-panel");

        // Select target year (this automatically opens month picker)
        String targetYear = String.valueOf(target.getYear());
        WebElement yearCell = dropdown.findElement(By.xpath(
            ".//td[contains(@class,'ant-picker-cell')]//div[contains(@class,'ant-picker-cell-inner')][text()='" + targetYear + "']"
        ));
        yearCell.click();
        waitForPickerPanelChange(dropdown, "ant-picker-month-panel");

        // Month picker is now open automatically, select target month
        String targetMonth = target.getMonth().toString().substring(0, 3);
        targetMonth = targetMonth.substring(0, 1).toUpperCase() + targetMonth.substring(1).toLowerCase();
        WebElement monthCell = dropdown.findElement(By.xpath(
            ".//td[contains(@class,'ant-picker-cell')]//div[contains(@class,'ant-picker-cell-inner')][text()='" + targetMonth + "']"
        ));
        monthCell.click();
        // Wait until day grid is back (date panel)
        waitForPickerPanelChange(dropdown, "ant-picker-date-panel");
    }

    /** Legacy month-by-month navigation */
    private void goToMonthLegacy(WebElement dropdown, LocalDate target) {
        for (int i = 0; i < 24; i++) {
            MonthYear current = inferVisibleMonth(dropdown);
            if (current.equals(target.getYear(), target.getMonthValue())) break;

            try {
                WebElement navButton;
                if (current.isBefore(target)) {
                    navButton = dropdown.findElement(NEXT_MONTH);
                } else {
                    navButton = dropdown.findElement(PREV_MONTH);
                }

                try {
                    wait.until(ExpectedConditions.elementToBeClickable(navButton)).click();
                } catch (ElementClickInterceptedException e) {
                    // fallback to JS click
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", navButton);
                }

                // Wait for month grid to update (condition already below handles final check)

            } catch (Exception e) {
                System.out.println("Navigation button click failed: " + e.getMessage());
                break;
            }

            // wait until header/grid actually changes
            MonthYear before = current;
            wait.until(d -> !inferVisibleMonth(dropdown).equals(before));

            if (isDayPresent(dropdown, target)) break;
        }
    }

    /** CLICK DAY -- throws explicit error if the cell exists but is DISABLED */
    private void clickDay(WebElement dropdown, LocalDate date) {
        String key = date.format(CELL_KEY);

        By enabled = By.cssSelector("td.ant-picker-cell-in-view[title='" + key + "']:not(.ant-picker-cell-disabled)");
        List<WebElement> ok = dropdown.findElements(enabled);
        if (!ok.isEmpty()) {
            wait.until(ExpectedConditions.elementToBeClickable(ok.get(0))).click();
            return;
        }

        // Present but disabled?
        By any = By.cssSelector("td.ant-picker-cell-in-view[title='" + key + "']");
        if (!dropdown.findElements(any).isEmpty()) {
            throw new IllegalStateException("AntD cell for " + key + " is present but DISABLED. "
                + "Likely violates min/max (e.g., To < From).");
        }

        // Fallback: some builds use data-value
        By dv = By.cssSelector("td.ant-picker-cell-in-view[data-value='" + key + "']:not(.ant-picker-cell-disabled)");
        List<WebElement> ok2 = dropdown.findElements(dv);
        if (!ok2.isEmpty()) {
            wait.until(ExpectedConditions.elementToBeClickable(ok2.get(0))).click();
            return;
        }

        // Fallback: match by inner text day number
        String day = String.valueOf(date.getDayOfMonth());
        By byText = By.xpath(".//td[contains(@class,'ant-picker-cell-in-view') and not(contains(@class,'ant-picker-cell-disabled'))]"
                           + "[.//div[contains(@class,'ant-picker-cell-inner')][normalize-space(text())='" + day + "']]");
        List<WebElement> ok3 = dropdown.findElements(byText);
        if (!ok3.isEmpty()) {
            wait.until(ExpectedConditions.elementToBeClickable(ok3.get(0))).click();
            return;
        }

        throw new NoSuchElementException("No clickable day cell for " + key + " in current month grid.");
    }

    private boolean isDayPresent(WebElement dropdown, LocalDate date) {
        String key = date.format(CELL_KEY);
        return !dropdown.findElements(By.cssSelector("td.ant-picker-cell-in-view[title='" + key + "']")).isEmpty()
            || !dropdown.findElements(By.cssSelector("td.ant-picker-cell-in-view[data-value='" + key + "']")).isEmpty();
    }

    private MonthYear inferVisibleMonth(WebElement dropdown) {
        List<WebElement> inView = dropdown.findElements(By.cssSelector("td.ant-picker-cell-in-view[title]"));
        String key = null;
        if (!inView.isEmpty()) {
            key = inView.get(0).getAttribute("title");          // e.g., 2025-09-01
        } else {
            List<WebElement> dv = dropdown.findElements(By.cssSelector("td.ant-picker-cell-in-view[data-value]"));
            if (!dv.isEmpty()) key = dv.get(0).getAttribute("data-value");
        }
        if (key == null || key.length() < 7) return new MonthYear(1970, 1);
        int y = Integer.parseInt(key.substring(0, 4));
        int m = Integer.parseInt(key.substring(5, 7));
        return new MonthYear(y, m);
    }

    private void waitUntilValuePopulated(WebElement input) {
        wait.until(d -> {
            String v = input.getAttribute("value");
            return v != null && !v.trim().isEmpty();
        });
    }

    private boolean hasClass(WebElement el, String cls) {
        String c = el.getAttribute("class");
        return c != null && c.contains(cls);
    }

    /** Wait until the dropdown contains a panel matching the given class fragment. */
    private void waitForPickerPanelChange(WebElement dropdown, String panelClassFragment) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2)).until(d ->
                !dropdown.findElements(By.cssSelector("div[class*='" + panelClassFragment + "']")).isEmpty()
            );
        } catch (TimeoutException e) {
            // best-effort: proceed even if panel didn't switch in time
        }
    }

    // Simple struct for comparing month/year
    public static class MonthYear {
        final int y, m;
        public MonthYear(int y, int m) { this.y = y; this.m = m; }
        boolean equals(int yy, int mm) { return y == yy && m == mm; }
        boolean isBefore(LocalDate d) { return y < d.getYear() || (y == d.getYear() && m < d.getMonthValue()); }
        @Override public boolean equals(Object o) { return (o instanceof MonthYear) && ((MonthYear)o).y == y && ((MonthYear)o).m == m; }
        @Override public int hashCode() { return y * 100 + m; }
    }
}