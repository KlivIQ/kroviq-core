package kroviq.wrapper.core;

import kroviq.utils.LoadProperties;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractTableEngine implements TableEngine, GridHandler {

    private static final Logger logger = LogManager.getLogger(AbstractTableEngine.class);

    private static final int MAX_RETRIES = LoadProperties.getInt("table.maxRetries", 3);
    private static final int MAX_PAGES_TO_SEARCH = LoadProperties.getInt("table.maxPagesToSearch", 10);
    private static final long MAX_SEARCH_TIME_MS = LoadProperties.getInt("table.maxRowSearchTimeSeconds", 30) * 1000L;
    protected static final int SORT_TIMEOUT_SECONDS = LoadProperties.getInt("table.sort.timeout.seconds", 10);

    protected WebElement tableRoot;
    private Map<String, Integer> columnMapCache;

    protected AbstractTableEngine(WebElement tableRoot) {
        this.tableRoot = tableRoot;
    }

    // --- Abstract methods subclasses must implement ---

    protected abstract List<WebElement> getDataRows(WebDriver driver);

    protected abstract Map<String, Integer> buildColumnMap(WebDriver driver);

    protected abstract String extractCellText(WebElement cell);

    protected abstract void doPerformRowAction(WebDriver driver, WebElement row, String actionName);

    protected abstract WebElement resolveTableRoot(WebDriver driver);

    // --- TableEngine implementation ---

    @Override
    public WebElement findRow(WebDriver driver, String columnHeader, String value) {
        logger.info("[TableEngine] Searching row where {}='{}'", columnHeader, value);
        return withRetry(driver, () -> {
            WebElement row = searchCurrentPage(driver, columnHeader, value);
            if (row != null) {
                logger.info("[TableEngine] Row found on current page");
                return row;
            }

            if (this instanceof TablePaginator paginator) {
                row = searchAcrossPages(driver, paginator, columnHeader, value);
                if (row != null) return row;
            }

            String scope = this instanceof TablePaginator ? MAX_PAGES_TO_SEARCH + " pages" : "current page";
            logger.warn("[TableEngine] Row not found where {}='{}' after searching {}", columnHeader, value, scope);
            throw new RuntimeException("Row not found where " + columnHeader + "='" + value
                    + "' after searching " + scope);
        });
    }

    @Override
    public WebElement findRowByCriteria(WebDriver driver, Map<String, String> criteria) {
        logger.info("[TableEngine] Searching row by criteria: {}", criteria);
        return withRetry(driver, () -> {
            WebElement row = searchCurrentPageByCriteria(driver, criteria);
            if (row != null) {
                logger.info("[TableEngine] Row found on current page");
                return row;
            }

            if (this instanceof TablePaginator paginator) {
                row = searchAcrossPagesByCriteria(driver, paginator, criteria);
                if (row != null) return row;
            }

            String scope = this instanceof TablePaginator ? MAX_PAGES_TO_SEARCH + " pages" : "current page";
            logger.warn("[TableEngine] Row not found matching criteria {} after searching {}", criteria, scope);
            throw new RuntimeException("Row not found matching criteria " + criteria
                    + " after searching " + scope);
        });
    }

    @Override
    public boolean rowExists(WebDriver driver, String columnHeader, String value) {
        try {
            findRow(driver, columnHeader, value);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public String getCellValue(WebDriver driver, WebElement row, String columnHeader) {
        return withRetry(driver, () -> {
            Map<String, Integer> colMap = getColumnMap(driver);
            Integer colIndex = colMap.get(columnHeader);
            if (colIndex == null) {
                throw new RuntimeException("Column '" + columnHeader + "' not found. Available: " + colMap.keySet());
            }
            List<WebElement> cells = getCellsFromRow(row);
            if (colIndex >= cells.size()) {
                throw new RuntimeException("Column index " + colIndex + " out of bounds for row with " + cells.size() + " cells");
            }
            return extractCellText(cells.get(colIndex));
        });
    }

    @Override
    public int getRowCount(WebDriver driver) {
        return withRetry(driver, () -> getDataRows(driver).size());
    }

    @Override
    public void performRowAction(WebDriver driver, WebElement row, String actionName) {
        logger.info("[TableEngine] Performing action '{}' on row", actionName);
        withRetry(driver, () -> {
            doPerformRowAction(driver, row, actionName);
            return null;
        });
    }

    // --- GridHandler implementation (backward compat) ---

    @Override
    public int getColumnIndex(WebDriver driver, String columnHeader) {
        Map<String, Integer> colMap = getColumnMap(driver);
        Integer index = colMap.get(columnHeader);
        if (index == null) {
            throw new org.openqa.selenium.NoSuchElementException(
                    "Column '" + columnHeader + "' not found. Available: " + colMap.keySet());
        }
        return index;
    }

    @Override
    public String getCellValue(WebDriver driver, String columnHeader, int rowIndex) {
        WebElement row = getRow(driver, rowIndex);
        return getCellValue(driver, row, columnHeader);
    }

    @Override
    public void clickCell(WebDriver driver, String columnHeader, int rowIndex) {
        Map<String, Integer> colMap = getColumnMap(driver);
        Integer colIndex = colMap.get(columnHeader);
        if (colIndex == null) {
            throw new org.openqa.selenium.NoSuchElementException("Column '" + columnHeader + "' not found");
        }
        WebElement row = getRow(driver, rowIndex);
        List<WebElement> cells = getCellsFromRow(row);
        if (colIndex < cells.size()) {
            cells.get(colIndex).click();
        }
    }

    @Override
    public void selectRow(WebDriver driver, int rowIndex) {
        getRow(driver, rowIndex).click();
    }

    @Override
    public WebElement getRow(WebDriver driver, int rowIndex) {
        List<WebElement> rows = getDataRows(driver);
        if (rowIndex < rows.size()) return rows.get(rowIndex);
        throw new org.openqa.selenium.NoSuchElementException(
                "Row at index " + rowIndex + " not found. Total rows: " + rows.size());
    }

    // --- Protected utilities for subclasses (selection support v2.2) ---

    protected WebElement findRowCheckbox(WebDriver driver, WebElement row) {
        String[] selectors = {
                "input[type='checkbox']",
                ".ag-selection-checkbox input, .ag-checkbox-input",
                ".ant-checkbox-input",
                ".p-checkbox-box, p-tableCheckbox input",
                "mat-checkbox input",
                "[class*='MuiCheckbox'] input"
        };
        for (String sel : selectors) {
            List<WebElement> checkboxes = row.findElements(By.cssSelector(sel));
            if (!checkboxes.isEmpty()) return checkboxes.get(0);
        }
        return null;
    }

    protected WebElement findHeaderCheckbox(WebDriver driver) {
        ensureTableRoot(driver);
        String[] selectors = {
                "thead input[type='checkbox']",
                ".ag-header-select-all input, .ag-header-select-all .ag-checkbox-input",
                ".ant-table-selection .ant-checkbox-input",
                "th p-tableHeaderCheckbox input, th p-tableHeaderCheckbox .p-checkbox-box",
                "th mat-checkbox input",
                "[class*='MuiDataGrid-columnHeaderCheckbox'] input"
        };
        for (String sel : selectors) {
            List<WebElement> checkboxes = tableRoot.findElements(By.cssSelector(sel));
            if (!checkboxes.isEmpty()) return checkboxes.get(0);
        }
        return null;
    }

    protected boolean isCheckboxChecked(WebElement checkbox) {
        if (checkbox == null) return false;
        // Direct checked attribute
        String checked = checkbox.getAttribute("checked");
        if ("true".equals(checked)) return true;
        // aria-checked
        String ariaChecked = checkbox.getAttribute("aria-checked");
        if ("true".equals(ariaChecked)) return true;
        // Check parent/ancestor for checked class patterns
        try {
            WebElement parent = checkbox.findElement(By.xpath(".."));
            String cls = parent.getAttribute("class");
            if (cls != null && (cls.contains("checked") || cls.contains("Mui-checked")
                    || cls.contains("p-highlight") || cls.contains("mat-mdc-checkbox-checked"))) {
                return true;
            }
        } catch (Exception ignored) {}
        // isSelected() as final fallback
        try { return checkbox.isSelected(); } catch (Exception e) { return false; }
    }

    protected void toggleCheckbox(WebDriver driver, WebElement checkbox, boolean targetState) {
        if (checkbox == null) throw new RuntimeException("Checkbox element not found");
        boolean current = isCheckboxChecked(checkbox);
        if (current != targetState) {
            HandlerUtils.clickSafe(driver, checkbox);
            // Verify state changed
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            boolean after = isCheckboxChecked(checkbox);
            if (after == current) {
                // Try clicking parent (some frameworks wrap checkbox)
                try {
                    WebElement parent = checkbox.findElement(By.xpath(".."));
                    HandlerUtils.clickSafe(driver, parent);
                } catch (Exception e) {
                    HandlerUtils.jsClick(driver, checkbox);
                }
            }
        }
    }

    // --- Protected utilities for subclasses (state assertion v2.2) ---

    protected boolean detectLoadingState(WebDriver driver) {
        ensureTableRoot(driver);
        String[] loadingSelectors = {
                ".loading:not([style*='display: none'])",
                "[aria-busy='true']",
                ".ag-overlay-loading-wrapper:not([style*='display: none'])",
                ".ant-spin-spinning",
                ".ant-table-loading",
                ".p-datatable-loading-overlay",
                "mat-progress-bar, mat-spinner",
                ".MuiDataGrid-overlay .MuiCircularProgress-root",
                "[class*='spinner']:not([style*='display: none'])",
                "[class*='loading']:not([style*='display: none'])"
        };
        for (String sel : loadingSelectors) {
            List<WebElement> indicators = tableRoot.findElements(By.cssSelector(sel));
            for (WebElement ind : indicators) {
                try { if (ind.isDisplayed()) return true; } catch (Exception ignored) {}
            }
        }
        return false;
    }

    protected boolean detectEmptyState(WebDriver driver) {
        if (detectLoadingState(driver)) return false;
        List<WebElement> rows = getDataRows(driver);
        if (!rows.isEmpty()) return false;
        // Check for explicit empty state indicators
        ensureTableRoot(driver);
        String[] emptySelectors = {
                ".empty-state, .no-data, .no-records",
                ".ag-overlay-no-rows-wrapper",
                ".ant-table-placeholder, .ant-empty",
                ".p-datatable-emptymessage",
                ".MuiDataGrid-overlay"
        };
        for (String sel : emptySelectors) {
            List<WebElement> indicators = tableRoot.findElements(By.cssSelector(sel));
            for (WebElement ind : indicators) {
                try { if (ind.isDisplayed()) return true; } catch (Exception ignored) {}
            }
        }
        return true;
    }

    protected String extractEmptyStateMessage(WebDriver driver) {
        ensureTableRoot(driver);
        String[] selectors = {
                ".empty-state, .no-data, .no-records",
                ".ag-overlay-no-rows-wrapper",
                ".ant-table-placeholder, .ant-empty-description",
                ".p-datatable-emptymessage td",
                ".MuiDataGrid-overlay"
        };
        for (String sel : selectors) {
            List<WebElement> elements = tableRoot.findElements(By.cssSelector(sel));
            for (WebElement el : elements) {
                try {
                    if (el.isDisplayed()) {
                        String text = el.getText().trim();
                        if (!text.isEmpty()) return text;
                    }
                } catch (Exception ignored) {}
            }
        }
        return "";
    }

    protected void waitForStable(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(
                    LoadProperties.getInt("table.stable.timeout.seconds", 10))).until(d -> {
                return !detectLoadingState(d);
            });
        } catch (Exception ignored) {}
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
    }

    // --- Protected utilities for subclasses (expansion v2.2-P2) ---

    protected WebElement findExpandToggle(WebElement row) {
        String[] selectors = {
                "button[class*='expand'], button[aria-label*='expand' i]",
                ".ag-group-expanded, .ag-group-contracted",
                ".ant-table-row-expand-icon",
                "[pRowToggler], button[class*='p-row-toggler']",
                "button[class*='detail'], button[aria-label*='detail' i]",
                "button[class*='toggle'], [class*='expand-icon']"
        };
        for (String sel : selectors) {
            List<WebElement> toggles = row.findElements(By.cssSelector(sel));
            if (!toggles.isEmpty()) return toggles.get(0);
        }
        // Fallback: first button/icon in first cell
        List<WebElement> firstCellBtns = row.findElements(By.cssSelector("td:first-child button, td:first-child [role='button']"));
        if (!firstCellBtns.isEmpty()) return firstCellBtns.get(0);
        return null;
    }

    protected WebElement findExpandedDetailRow(WebDriver driver, WebElement row) {
        String[] detailSelectors = {
                "following-sibling::tr[contains(@class,'expanded') or contains(@class,'detail') or contains(@class,'expansion')]",
                "following-sibling::tr[.//td[@colspan]]",
                "following-sibling::tr[contains(@class,'ag-full-width-row')]",
                "following-sibling::tr[contains(@class,'ant-table-expanded-row')]",
                "following-sibling::tr[contains(@class,'p-rowexpansion')]"
        };
        for (String xpath : detailSelectors) {
            List<WebElement> details = row.findElements(By.xpath(xpath));
            if (!details.isEmpty()) {
                WebElement detail = details.get(0);
                try { if (detail.isDisplayed()) return detail; } catch (Exception ignored) {}
            }
        }
        return null;
    }

    protected void waitForExpandedContent(WebDriver driver, WebElement row) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> {
                WebElement detail = findExpandedDetailRow(d, row);
                return detail != null && detail.isDisplayed();
            });
        } catch (Exception ignored) {}
    }

    protected void waitForCollapsed(WebDriver driver, WebElement row) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> {
                WebElement detail = findExpandedDetailRow(d, row);
                return detail == null || !detail.isDisplayed();
            });
        } catch (Exception ignored) {}
    }

    // --- Protected utilities for subclasses (editing v2.2-P2) ---

    protected WebElement getCellByColumn(WebDriver driver, WebElement row, String targetColumn) {
        Map<String, Integer> colMap = getColumnMap(driver);
        Integer colIndex = colMap.get(targetColumn);
        if (colIndex == null) {
            throw new RuntimeException("Column '" + targetColumn + "' not found. Available: " + colMap.keySet());
        }
        List<WebElement> cells = getCellsFromRow(row);
        if (colIndex >= cells.size()) {
            throw new RuntimeException("Column index " + colIndex + " out of bounds for row with " + cells.size() + " cells");
        }
        return cells.get(colIndex);
    }

    protected void activateCellForEdit(WebDriver driver, WebElement cell) {
        // Default: double-click to enter edit mode
        new org.openqa.selenium.interactions.Actions(driver).doubleClick(cell).perform();
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
    }

    protected TableEditor.CellEditType detectCellEditType(WebElement cell) {
        // Check for existing input elements (already in edit mode)
        if (!cell.findElements(By.cssSelector("select, [role='listbox']")).isEmpty()) {
            return TableEditor.CellEditType.DROPDOWN;
        }
        if (!cell.findElements(By.cssSelector("input[type='checkbox']")).isEmpty()) {
            return TableEditor.CellEditType.CHECKBOX;
        }
        String[] toggleSelectors = {
                "[class*='toggle'], [class*='switch'], [role='switch']",
                ".ant-switch, .p-inputswitch, .mat-slide-toggle, .MuiSwitch"
        };
        for (String sel : toggleSelectors) {
            if (!cell.findElements(By.cssSelector(sel)).isEmpty()) {
                return TableEditor.CellEditType.TOGGLE;
            }
        }
        return TableEditor.CellEditType.TEXT_INPUT;
    }

    protected void performTextEdit(WebDriver driver, WebElement cell, String value) {
        List<WebElement> inputs = cell.findElements(By.cssSelector(
                "input[type='text'], input:not([type]), textarea, [contenteditable='true']"));
        if (inputs.isEmpty()) {
            inputs = cell.findElements(By.cssSelector("input"));
        }
        if (inputs.isEmpty()) {
            throw new RuntimeException("No text input found in cell after activation");
        }
        WebElement input = inputs.get(0);
        HandlerUtils.clearAndType(driver, input, value);
    }

    protected void performDropdownEdit(WebDriver driver, WebElement cell, String value) {
        // Try native <select>
        List<WebElement> selects = cell.findElements(By.cssSelector("select"));
        if (!selects.isEmpty()) {
            new org.openqa.selenium.support.ui.Select(selects.get(0)).selectByVisibleText(value);
            return;
        }
        // Try custom dropdown: click to open, then find option
        List<WebElement> triggers = cell.findElements(By.cssSelector(
                "[role='combobox'], [class*='select'], [class*='dropdown']"));
        if (!triggers.isEmpty()) {
            HandlerUtils.clickSafe(driver, triggers.get(0));
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            // Find option in overlay/dropdown panel
            WebElement option = new WebDriverWait(driver, Duration.ofSeconds(3)).until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//*[contains(@class,'option') or contains(@class,'item') or @role='option']" +
                                    "[normalize-space()='" + value + "']")));
            option.click();
            return;
        }
        throw new RuntimeException("No dropdown found in cell for edit");
    }

    protected void performCheckboxEdit(WebDriver driver, WebElement cell, String value) {
        List<WebElement> checkboxes = cell.findElements(By.cssSelector("input[type='checkbox']"));
        if (checkboxes.isEmpty()) {
            throw new RuntimeException("No checkbox found in cell for edit");
        }
        WebElement cb = checkboxes.get(0);
        boolean targetState = "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)
                || "checked".equalsIgnoreCase(value) || "1".equals(value);
        toggleCheckbox(driver, cb, targetState);
    }

    protected void performToggleEdit(WebDriver driver, WebElement cell, String value) {
        List<WebElement> toggles = cell.findElements(By.cssSelector(
                "[class*='toggle'], [class*='switch'], [role='switch'], " +
                ".ant-switch, .p-inputswitch, .mat-slide-toggle, .MuiSwitch-root"));
        if (toggles.isEmpty()) {
            throw new RuntimeException("No toggle/switch found in cell for edit");
        }
        WebElement toggle = toggles.get(0);
        boolean targetState = "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)
                || "yes".equalsIgnoreCase(value) || "1".equals(value);
        // Determine current state
        String cls = toggle.getAttribute("class");
        String ariaChecked = toggle.getAttribute("aria-checked");
        boolean currentState = (cls != null && (cls.contains("checked") || cls.contains("active")))
                || "true".equals(ariaChecked);
        if (currentState != targetState) {
            HandlerUtils.clickSafe(driver, toggle);
        }
    }

    protected void commitCellEdit(WebDriver driver, WebElement cell) {
        // Default: press Enter then click table root to blur
        try {
            List<WebElement> inputs = cell.findElements(By.cssSelector("input, textarea"));
            if (!inputs.isEmpty()) {
                inputs.get(0).sendKeys(Keys.ENTER);
            }
        } catch (Exception ignored) {}
        try {
            ensureTableRoot(driver).click();
        } catch (Exception ignored) {}
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        invalidateColumnMap();
    }

    // --- Protected utilities for subclasses (sort/filter support) ---

    protected WebElement findHeaderCell(WebDriver driver, String columnName) {
        Map<String, Integer> colMap = getColumnMap(driver);
        Integer colIndex = colMap.get(columnName);
        if (colIndex == null) {
            throw new RuntimeException("Column '" + columnName + "' not found. Available: " + colMap.keySet());
        }
        ensureTableRoot(driver);
        List<WebElement> headers = tableRoot.findElements(By.cssSelector(
                "thead th, [role='columnheader'], mat-header-cell"));
        if (colIndex < headers.size()) return headers.get(colIndex);
        throw new RuntimeException("Header cell at index " + colIndex + " not found");
    }

    protected Duration getSortTimeoutDuration() {
        return Duration.ofSeconds(SORT_TIMEOUT_SECONDS);
    }

    protected void waitForDomStabilization(WebDriver driver) {
        try {
            new WebDriverWait(driver, getSortTimeoutDuration()).until(d -> {
                List<WebElement> spinners = tableRoot.findElements(By.cssSelector(
                        ".ant-spin-spinning, .loading, [class*='loading'], [class*='spinner']"));
                return spinners.isEmpty() || spinners.stream().noneMatch(s -> {
                    try { return s.isDisplayed(); } catch (Exception e) { return false; }
                });
            });
        } catch (Exception ignored) {}
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
    }

    protected List<String> getVisibleColumnValues(WebDriver driver, String columnName) {
        Map<String, Integer> colMap = getColumnMap(driver);
        Integer colIndex = colMap.get(columnName);
        if (colIndex == null) {
            throw new RuntimeException("Column '" + columnName + "' not found. Available: " + colMap.keySet());
        }
        List<WebElement> rows = getDataRows(driver);
        List<String> values = new ArrayList<>();
        for (WebElement row : rows) {
            List<WebElement> cells = getCellsFromRow(row);
            if (colIndex < cells.size()) {
                values.add(extractCellText(cells.get(colIndex)));
            }
        }
        return values;
    }

    protected boolean verifyDataSortOrder(List<String> values, TableSorter.SortOrder expected) {
        if (values.size() <= 1 || expected == TableSorter.SortOrder.NONE) return true;

        // Detect type from first non-empty value
        ValueType type = detectValueType(values);

        for (int i = 0; i < values.size() - 1; i++) {
            String a = values.get(i);
            String b = values.get(i + 1);
            if (a.isEmpty() || b.isEmpty()) continue;

            int cmp = compareValues(a, b, type);
            if (expected == TableSorter.SortOrder.ASC && cmp > 0) return false;
            if (expected == TableSorter.SortOrder.DESC && cmp < 0) return false;
        }
        return true;
    }

    // --- Value type detection and comparison ---

    protected enum ValueType { NUMERIC, CURRENCY, DATE, STRING }

    private ValueType detectValueType(List<String> values) {
        for (String v : values) {
            if (v == null || v.isEmpty()) continue;
            if (isCurrency(v)) return ValueType.CURRENCY;
            if (isNumeric(v)) return ValueType.NUMERIC;
            if (isDate(v)) return ValueType.DATE;
            return ValueType.STRING;
        }
        return ValueType.STRING;
    }

    private boolean isNumeric(String s) {
        try { Double.parseDouble(s.replace(",", "")); return true; }
        catch (NumberFormatException e) { return false; }
    }

    private boolean isCurrency(String s) {
        return s.matches("^[\\$€£¥₹]?[\\d,]+\\.?\\d*$") && s.replaceAll("[^\\d.]", "").length() > 0;
    }

    private boolean isDate(String s) {
        return parseDate(s) != null;
    }

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MMM dd, yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy")
    };

    private LocalDate parseDate(String s) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try { return LocalDate.parse(s.trim(), fmt); } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private int compareValues(String a, String b, ValueType type) {
        return switch (type) {
            case NUMERIC -> Double.compare(
                    Double.parseDouble(a.replace(",", "")),
                    Double.parseDouble(b.replace(",", "")));
            case CURRENCY -> Double.compare(
                    Double.parseDouble(a.replaceAll("[^\\d.]", "")),
                    Double.parseDouble(b.replaceAll("[^\\d.]", "")));
            case DATE -> {
                LocalDate da = parseDate(a);
                LocalDate db = parseDate(b);
                yield (da != null && db != null) ? da.compareTo(db) : a.compareToIgnoreCase(b);
            }
            case STRING -> a.compareToIgnoreCase(b);
        };
    }

    // --- Protected utilities for subclasses ---

    protected Map<String, Integer> getColumnMap(WebDriver driver) {
        if (columnMapCache == null) {
            columnMapCache = buildColumnMap(driver);
        }
        return columnMapCache;
    }

    protected void invalidateColumnMap() {
        columnMapCache = null;
    }

    protected WebElement ensureTableRoot(WebDriver driver) {
        try {
            tableRoot.isDisplayed();
            return tableRoot;
        } catch (StaleElementReferenceException e) {
            logger.debug("Table root stale, re-resolving");
            tableRoot = resolveTableRoot(driver);
            invalidateColumnMap();
            return tableRoot;
        }
    }

    protected List<WebElement> getCellsFromRow(WebElement row) {
        List<WebElement> cells = row.findElements(By.cssSelector("td, [role='gridcell']"));
        if (cells.isEmpty()) {
            cells = row.findElements(By.cssSelector("td"));
        }
        return cells;
    }

    protected WebElement findActionInRow(WebDriver driver, WebElement row, String actionName) {
        // Priority 1: exact button text
        List<WebElement> buttons = row.findElements(By.xpath(
                ".//button[normalize-space()='" + actionName + "']"));
        if (!buttons.isEmpty()) return buttons.get(0);

        // Priority 2: exact link text
        List<WebElement> links = row.findElements(By.xpath(
                ".//a[normalize-space()='" + actionName + "']"));
        if (!links.isEmpty()) return links.get(0);

        // Priority 3: title attribute
        List<WebElement> titled = row.findElements(By.xpath(
                ".//*[@title='" + actionName + "']"));
        if (!titled.isEmpty()) return titled.get(0);

        // Priority 4: aria-label
        List<WebElement> ariaLabeled = row.findElements(By.xpath(
                ".//*[@aria-label='" + actionName + "']"));
        if (!ariaLabeled.isEmpty()) return ariaLabeled.get(0);

        // Priority 5: data-action attribute
        List<WebElement> dataAction = row.findElements(By.xpath(
                ".//*[@data-action='" + actionName + "']"));
        if (!dataAction.isEmpty()) return dataAction.get(0);

        return null;
    }

    // --- Private helpers ---

    private WebElement searchCurrentPage(WebDriver driver, String columnHeader, String value) {
        Map<String, Integer> colMap = getColumnMap(driver);
        Integer colIndex = colMap.get(columnHeader);
        if (colIndex == null) {
            throw new RuntimeException("Column '" + columnHeader + "' not found. Available: " + colMap.keySet());
        }

        List<WebElement> rows = getDataRows(driver);
        for (WebElement row : rows) {
            List<WebElement> cells = getCellsFromRow(row);
            if (colIndex < cells.size()) {
                String cellText = extractCellText(cells.get(colIndex));
                if (value.equalsIgnoreCase(cellText)) return row;
            }
        }
        return null;
    }

    private WebElement searchCurrentPageByCriteria(WebDriver driver, Map<String, String> criteria) {
        Map<String, Integer> colMap = getColumnMap(driver);
        for (Map.Entry<String, String> entry : criteria.entrySet()) {
            if (!colMap.containsKey(entry.getKey())) {
                throw new RuntimeException("Column '" + entry.getKey() + "' not found. Available: " + colMap.keySet());
            }
        }

        List<WebElement> rows = getDataRows(driver);
        for (WebElement row : rows) {
            List<WebElement> cells = getCellsFromRow(row);
            boolean allMatch = true;
            for (Map.Entry<String, String> entry : criteria.entrySet()) {
                int colIndex = colMap.get(entry.getKey());
                if (colIndex >= cells.size()) { allMatch = false; break; }
                String cellText = extractCellText(cells.get(colIndex));
                if (!entry.getValue().equalsIgnoreCase(cellText)) { allMatch = false; break; }
            }
            if (allMatch) return row;
        }
        return null;
    }

    private WebElement searchAcrossPages(WebDriver driver, TablePaginator paginator,
                                         String columnHeader, String value) {
        long startTime = System.currentTimeMillis();
        int pagesSearched = 1; // current page already searched
        for (int page = 0; page < MAX_PAGES_TO_SEARCH - 1; page++) {
            if (System.currentTimeMillis() - startTime > MAX_SEARCH_TIME_MS) {
                logger.warn("[TableEngine] Cross-page search timed out after {}ms", MAX_SEARCH_TIME_MS);
                break;
            }
            if (!paginator.hasNextPage(driver)) {
                logger.debug("[TableEngine] No more pages available after page {}", pagesSearched);
                break;
            }
            paginator.nextPage(driver);
            pagesSearched++;
            logger.debug("[TableEngine] Searching page {}/{}", pagesSearched, MAX_PAGES_TO_SEARCH);
            invalidateColumnMap();
            WebElement row = searchCurrentPage(driver, columnHeader, value);
            if (row != null) {
                logger.info("[TableEngine] Row found after {} pages", pagesSearched);
                return row;
            }
        }
        logger.debug("[TableEngine] Row not found after {} pages", pagesSearched);
        return null;
    }

    private WebElement searchAcrossPagesByCriteria(WebDriver driver, TablePaginator paginator,
                                                   Map<String, String> criteria) {
        long startTime = System.currentTimeMillis();
        int pagesSearched = 1;
        for (int page = 0; page < MAX_PAGES_TO_SEARCH - 1; page++) {
            if (System.currentTimeMillis() - startTime > MAX_SEARCH_TIME_MS) {
                logger.warn("[TableEngine] Cross-page search timed out after {}ms", MAX_SEARCH_TIME_MS);
                break;
            }
            if (!paginator.hasNextPage(driver)) break;
            paginator.nextPage(driver);
            pagesSearched++;
            logger.debug("[TableEngine] Searching page {}/{}", pagesSearched, MAX_PAGES_TO_SEARCH);
            invalidateColumnMap();
            WebElement row = searchCurrentPageByCriteria(driver, criteria);
            if (row != null) {
                logger.info("[TableEngine] Row found after {} pages", pagesSearched);
                return row;
            }
        }
        return null;
    }

    private <T> T withRetry(WebDriver driver, RetryableAction<T> action) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return action.execute();
            } catch (StaleElementReferenceException e) {
                logger.debug("[TableEngine] Stale element on attempt {}/{}, recovering table root", attempt, MAX_RETRIES);
                if (attempt == MAX_RETRIES) {
                    logger.error("[TableEngine] Operation failed after {} retries due to stale elements", MAX_RETRIES);
                    throw new RuntimeException("Operation failed after " + MAX_RETRIES + " retries due to stale elements", e);
                }
                ensureTableRoot(driver);
            }
        }
        throw new RuntimeException("Unreachable");
    }

    @FunctionalInterface
    private interface RetryableAction<T> {
        T execute();
    }
}
