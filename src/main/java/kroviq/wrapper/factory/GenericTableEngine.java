package kroviq.wrapper.factory;

import kroviq.wrapper.core.AbstractTableEngine;
import kroviq.wrapper.core.HandlerUtils;
import kroviq.wrapper.core.TableEditor;
import kroviq.wrapper.core.TableExpander;
import kroviq.wrapper.core.TableFilter;
import kroviq.wrapper.core.TablePaginator;
import kroviq.wrapper.core.TableSelector;
import kroviq.wrapper.core.TableSorter;
import kroviq.wrapper.core.TableStateAssert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GenericTableEngine extends AbstractTableEngine implements TablePaginator, TableSorter, TableFilter, TableSelector, TableStateAssert, TableExpander, TableEditor {

    private static final Logger logger = LogManager.getLogger(GenericTableEngine.class);
    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("generic");

    public GenericTableEngine(WebElement tableRoot) {
        super(tableRoot);
    }

    @Override
    protected List<WebElement> getDataRows(WebDriver driver) {
        ensureTableRoot(driver);
        // Standard HTML: tbody > tr with td cells
        List<WebElement> rows = tableRoot.findElements(By.cssSelector("tbody tr"));
        if (!rows.isEmpty()) {
            return rows.stream()
                    .filter(row -> !row.findElements(By.cssSelector("td")).isEmpty())
                    .toList();
        }
        // Role-based fallback
        rows = tableRoot.findElements(By.cssSelector("[role='row']"));
        return rows.stream()
                .filter(row -> !row.findElements(By.cssSelector("[role='gridcell'], td")).isEmpty())
                .toList();
    }

    @Override
    protected Map<String, Integer> buildColumnMap(WebDriver driver) {
        ensureTableRoot(driver);
        Map<String, Integer> map = new LinkedHashMap<>();
        List<WebElement> headers = tableRoot.findElements(By.cssSelector("thead th"));
        if (headers.isEmpty()) {
            headers = tableRoot.findElements(By.cssSelector("[role='columnheader']"));
        }
        if (headers.isEmpty()) {
            headers = tableRoot.findElements(By.cssSelector("th"));
        }
        for (int i = 0; i < headers.size(); i++) {
            String text = headers.get(i).getText().trim();
            if (!text.isEmpty()) {
                map.put(text, i);
            }
        }
        return map;
    }

    @Override
    protected String extractCellText(WebElement cell) {
        String text = cell.getText().trim();
        if (!text.isEmpty()) return text;
        // Fallback: check input value
        try {
            WebElement input = cell.findElement(By.cssSelector("input, select, textarea"));
            String value = input.getAttribute("value");
            if (value != null && !value.isEmpty()) return value;
        } catch (Exception ignored) {}
        return text;
    }

    @Override
    protected void doPerformRowAction(WebDriver driver, WebElement row, String actionName) {
        WebElement actionElement = findActionInRow(driver, row, actionName);
        if (actionElement != null) {
            HandlerUtils.scrollIntoViewAndClick(driver, actionElement);
            return;
        }

        // Priority 6: meatball/ellipsis menu fallback
        WebElement menuTrigger = findMenuTrigger(row);
        if (menuTrigger != null) {
            HandlerUtils.scrollIntoViewAndClick(driver, menuTrigger);
            WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
            try {
                WebElement menuItem = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//li[normalize-space()='" + actionName + "'] | " +
                                "//a[normalize-space()='" + actionName + "'] | " +
                                "//span[normalize-space()='" + actionName + "'] | " +
                                "//button[normalize-space()='" + actionName + "']")));
                menuItem.click();
                return;
            } catch (Exception e) {
                logger.debug("Menu item '{}' not found after opening menu trigger", actionName);
            }
        }

        throw new RuntimeException("Action '" + actionName + "' not found in row. "
                + "Searched: button text, link text, title, aria-label, data-action, menu trigger");
    }

    @Override
    protected WebElement resolveTableRoot(WebDriver driver) {
        // Generic fallback: re-find by tag or role
        try {
            String id = tableRoot.getAttribute("id");
            if (id != null && !id.isEmpty()) {
                return driver.findElement(By.id(id));
            }
        } catch (Exception ignored) {}
        // Last resort: find first table on page
        return driver.findElement(By.cssSelector("table, [role='grid'], [role='table']"));
    }

    // --- TablePaginator ---

    @Override
    public void nextPage(WebDriver driver) {
        WebElement nextBtn = findPaginationButton(driver, "next");
        if (nextBtn == null || !nextBtn.isEnabled()) {
            throw new RuntimeException("No next page available");
        }
        nextBtn.click();
        waitForRowsRefresh(driver);
    }

    @Override
    public void previousPage(WebDriver driver) {
        WebElement prevBtn = findPaginationButton(driver, "previous");
        if (prevBtn == null || !prevBtn.isEnabled()) {
            throw new RuntimeException("No previous page available");
        }
        prevBtn.click();
        waitForRowsRefresh(driver);
    }

    @Override
    public void goToPage(WebDriver driver, int pageNumber) {
        // Try page number buttons
        List<WebElement> pageButtons = driver.findElements(By.cssSelector(
                ".pagination a, .pagination button, [class*='page'] a, [class*='page'] button, nav li a, nav li button"));
        for (WebElement btn : pageButtons) {
            if (String.valueOf(pageNumber).equals(btn.getText().trim())) {
                btn.click();
                waitForRowsRefresh(driver);
                return;
            }
        }
        throw new RuntimeException("Page " + pageNumber + " button not found");
    }

    @Override
    public boolean hasNextPage(WebDriver driver) {
        WebElement nextBtn = findPaginationButton(driver, "next");
        return nextBtn != null && nextBtn.isEnabled();
    }

    // --- TableSorter ---

    @Override
    public void sortByColumn(WebDriver driver, String columnName, SortOrder order) {
        WebElement header = findSortableHeader(driver, columnName);
        SortOrder current = readSortState(header);
        int maxClicks = 3;
        while (current != order && maxClicks-- > 0) {
            HandlerUtils.clickSafe(driver, header);
            waitForDomStabilization(driver);
            header = findSortableHeader(driver, columnName);
            current = readSortState(header);
        }
    }

    @Override
    public SortOrder getCurrentSort(WebDriver driver, String columnName) {
        return readSortState(findSortableHeader(driver, columnName));
    }

    @Override
    public boolean verifySortOrder(WebDriver driver, String columnName, SortOrder expected) {
        List<String> values = getVisibleColumnValues(driver, columnName);
        return verifyDataSortOrder(values, expected);
    }

    private WebElement findSortableHeader(WebDriver driver, String columnName) {
        ensureTableRoot(driver);
        List<WebElement> headers = tableRoot.findElements(By.cssSelector("thead th"));
        if (headers.isEmpty()) headers = tableRoot.findElements(By.cssSelector("[role='columnheader']"));
        for (WebElement h : headers) {
            if (columnName.equalsIgnoreCase(h.getText().trim())) return h;
        }
        throw new RuntimeException("Sortable header '" + columnName + "' not found in Generic HTML table");
    }

    private SortOrder readSortState(WebElement header) {
        String ariaSort = header.getAttribute("aria-sort");
        if (ariaSort != null) {
            if (ariaSort.equalsIgnoreCase("ascending")) return SortOrder.ASC;
            if (ariaSort.equalsIgnoreCase("descending")) return SortOrder.DESC;
        }
        String cls = header.getAttribute("class");
        if (cls != null) {
            if (cls.contains("sort-asc") || cls.contains("sorted-asc")) return SortOrder.ASC;
            if (cls.contains("sort-desc") || cls.contains("sorted-desc")) return SortOrder.DESC;
        }
        return SortOrder.NONE;
    }

    // --- TableFilter ---

    @Override
    public void filterColumnContains(WebDriver driver, String columnName, String value) {
        WebElement input = findColumnFilterInput(driver, columnName);
        HandlerUtils.clearAndType(driver, input, value);
        waitForDomStabilization(driver);
    }

    @Override
    public void clearColumnFilter(WebDriver driver, String columnName) {
        WebElement input = findColumnFilterInput(driver, columnName);
        HandlerUtils.clearAndType(driver, input, "");
        input.sendKeys(Keys.BACK_SPACE);
        waitForDomStabilization(driver);
    }

    @Override
    public void clearAllFilters(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> inputs = tableRoot.findElements(By.cssSelector(
                "thead input, [class*='filter'] input, input[type='search']"));
        for (WebElement input : inputs) {
            try { HandlerUtils.clearAndType(driver, input, ""); } catch (Exception ignored) {}
        }
        waitForDomStabilization(driver);
    }

    @Override
    public void globalSearch(WebDriver driver, String searchText) {
        WebElement searchInput = findGlobalSearchInput(driver);
        HandlerUtils.clearAndType(driver, searchInput, searchText);
        waitForDomStabilization(driver);
    }

    @Override
    public void clearGlobalSearch(WebDriver driver) {
        WebElement searchInput = findGlobalSearchInput(driver);
        HandlerUtils.clearAndType(driver, searchInput, "");
        searchInput.sendKeys(Keys.BACK_SPACE);
        waitForDomStabilization(driver);
    }

    private WebElement findColumnFilterInput(WebDriver driver, String columnName) {
        ensureTableRoot(driver);
        Map<String, Integer> colMap = getColumnMap(driver);
        Integer colIndex = colMap.get(columnName);
        if (colIndex == null) {
            throw new RuntimeException("Column '" + columnName + "' not found for filtering. Available: " + colMap.keySet());
        }
        // Look for filter inputs in thead row (second row) or filter row
        List<WebElement> filterInputs = tableRoot.findElements(By.cssSelector(
                "thead tr:nth-child(2) input, thead input[data-column], [class*='filter'] input"));
        if (colIndex < filterInputs.size()) return filterInputs.get(colIndex);
        // Try input with matching placeholder or data attribute
        for (WebElement input : filterInputs) {
            String placeholder = input.getAttribute("placeholder");
            String dataCol = input.getAttribute("data-column");
            if ((placeholder != null && placeholder.toLowerCase().contains(columnName.toLowerCase()))
                    || columnName.equals(dataCol)) {
                return input;
            }
        }
        throw new RuntimeException("Column filter input for '" + columnName + "' not found in Generic HTML table");
    }

    private WebElement findGlobalSearchInput(WebDriver driver) {
        String[] selectors = {
                "input[type='search']",
                "input[placeholder*='search' i]",
                "input[placeholder*='Search']",
                "input[aria-label*='search' i]",
                "[class*='search'] input",
                "[class*='global-filter'] input"
        };
        for (String sel : selectors) {
            List<WebElement> inputs = driver.findElements(By.cssSelector(sel));
            if (!inputs.isEmpty() && inputs.get(0).isDisplayed()) return inputs.get(0);
        }
        throw new RuntimeException("Global search input not found in Generic HTML table");
    }

    // --- TableSelector ---

    @Override
    public void selectRow(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findRowCheckbox(driver, row);
        if (checkbox == null) throw new RuntimeException("No checkbox found in row where " + column + "='" + value + "'");
        toggleCheckbox(driver, checkbox, true);
    }

    @Override
    public void deselectRow(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findRowCheckbox(driver, row);
        if (checkbox == null) throw new RuntimeException("No checkbox found in row where " + column + "='" + value + "'");
        toggleCheckbox(driver, checkbox, false);
    }

    @Override
    public void selectAll(WebDriver driver) {
        WebElement headerCheckbox = findHeaderCheckbox(driver);
        if (headerCheckbox == null) throw new RuntimeException("No header checkbox found for select all");
        toggleCheckbox(driver, headerCheckbox, true);
    }

    @Override
    public void deselectAll(WebDriver driver) {
        WebElement headerCheckbox = findHeaderCheckbox(driver);
        if (headerCheckbox == null) throw new RuntimeException("No header checkbox found for deselect all");
        toggleCheckbox(driver, headerCheckbox, false);
    }

    @Override
    public int getSelectedCount(WebDriver driver) {
        ensureTableRoot(driver);
        int count = 0;
        List<WebElement> rows = getDataRows(driver);
        for (WebElement row : rows) {
            WebElement checkbox = findRowCheckbox(driver, row);
            if (checkbox != null && isCheckboxChecked(checkbox)) count++;
        }
        return count;
    }

    @Override
    public boolean isRowSelected(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findRowCheckbox(driver, row);
        return checkbox != null && isCheckboxChecked(checkbox);
    }

    // --- TableExpander ---

    @Override
    public void expandRow(WebDriver driver, String column, String value) {
        if (isRowExpanded(driver, column, value)) return;
        WebElement row = findRow(driver, column, value);
        WebElement toggle = findExpandToggle(row);
        if (toggle == null) throw new RuntimeException("No expand toggle found in row where " + column + "='" + value + "'");
        HandlerUtils.clickSafe(driver, toggle);
        waitForExpandedContent(driver, row);
    }

    @Override
    public void collapseRow(WebDriver driver, String column, String value) {
        if (!isRowExpanded(driver, column, value)) return;
        WebElement row = findRow(driver, column, value);
        WebElement toggle = findExpandToggle(row);
        if (toggle == null) throw new RuntimeException("No expand toggle found in row where " + column + "='" + value + "'");
        HandlerUtils.clickSafe(driver, toggle);
        waitForCollapsed(driver, row);
    }

    @Override
    public boolean isRowExpanded(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement detail = findExpandedDetailRow(driver, row);
        return detail != null && detail.isDisplayed();
    }

    @Override
    public WebElement getExpandedContent(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement detail = findExpandedDetailRow(driver, row);
        if (detail == null || !detail.isDisplayed()) {
            throw new RuntimeException("Row is not expanded. Call expandRow() before requesting expanded content. "
                    + "Row: " + column + "='" + value + "'");
        }
        return detail;
    }

    // --- TableEditor ---

    @Override
    public void editCell(WebDriver driver, String rowColumn, String rowValue,
                         String targetColumn, String newValue) {
        WebElement row = findRow(driver, rowColumn, rowValue);
        WebElement cell = getCellByColumn(driver, row, targetColumn);
        activateCellForEdit(driver, cell);
        // Re-find cell after activation (DOM may have changed)
        row = findRow(driver, rowColumn, rowValue);
        cell = getCellByColumn(driver, row, targetColumn);
        TableEditor.CellEditType type = detectCellEditType(cell);
        performEditByType(driver, cell, newValue, type);
        commitCellEdit(driver, cell);
    }

    @Override
    public void editCellAs(WebDriver driver, String rowColumn, String rowValue,
                           String targetColumn, String newValue, CellEditType editType) {
        WebElement row = findRow(driver, rowColumn, rowValue);
        WebElement cell = getCellByColumn(driver, row, targetColumn);
        if (editType != CellEditType.CHECKBOX && editType != CellEditType.TOGGLE) {
            activateCellForEdit(driver, cell);
            row = findRow(driver, rowColumn, rowValue);
            cell = getCellByColumn(driver, row, targetColumn);
        }
        performEditByType(driver, cell, newValue, editType);
        if (editType == CellEditType.TEXT_INPUT || editType == CellEditType.DROPDOWN) {
            commitCellEdit(driver, cell);
        }
    }

    @Override
    public boolean isCellEditable(WebDriver driver, String rowColumn, String rowValue,
                                  String targetColumn) {
        WebElement row = findRow(driver, rowColumn, rowValue);
        WebElement cell = getCellByColumn(driver, row, targetColumn);
        // Check for editable indicators
        String contentEditable = cell.getAttribute("contenteditable");
        if ("true".equals(contentEditable)) return true;
        // Check for data-editable attribute
        String dataEditable = cell.getAttribute("data-editable");
        if ("true".equals(dataEditable)) return true;
        // Check for existing input/select/checkbox
        if (!cell.findElements(By.cssSelector("input, select, textarea, [contenteditable='true']")).isEmpty()) return true;
        // Check for editable class
        String cls = cell.getAttribute("class");
        return cls != null && (cls.contains("editable") || cls.contains("edit"));
    }

    private void performEditByType(WebDriver driver, WebElement cell, String value, CellEditType type) {
        switch (type) {
            case TEXT_INPUT -> performTextEdit(driver, cell, value);
            case DROPDOWN -> performDropdownEdit(driver, cell, value);
            case CHECKBOX -> performCheckboxEdit(driver, cell, value);
            case TOGGLE -> performToggleEdit(driver, cell, value);
        }
    }

    // --- TableStateAssert ---

    @Override
    public boolean isLoading(WebDriver driver) {
        return detectLoadingState(driver);
    }

    @Override
    public boolean isEmpty(WebDriver driver) {
        return detectEmptyState(driver);
    }

    @Override
    public String getEmptyStateMessage(WebDriver driver) {
        return extractEmptyStateMessage(driver);
    }

    // --- Private helpers ---

    private WebElement findMenuTrigger(WebElement row) {
        String[] menuSelectors = {
                ".//button[contains(@class,'more') or contains(@class,'menu') or contains(@class,'ellipsis') or contains(@class,'action')]",
                ".//*[contains(@class,'meatball') or contains(@class,'kebab') or contains(@class,'dots')]",
                ".//button[contains(@aria-label,'more') or contains(@aria-label,'action') or contains(@aria-label,'menu')]",
                ".//button[text()='⋮' or text()='...' or text()='⋯']"
        };
        for (String selector : menuSelectors) {
            List<WebElement> triggers = row.findElements(By.xpath(selector));
            if (!triggers.isEmpty()) return triggers.get(0);
        }
        return null;
    }

    private WebElement findPaginationButton(WebDriver driver, String direction) {
        String[] selectors;
        if ("next".equals(direction)) {
            selectors = new String[]{
                    "[class*='next']:not([disabled])",
                    "[aria-label='Next']",
                    "[aria-label='next']",
                    "a:has(> [class*='right'])",
                    "button:has(> [class*='right'])",
                    ".pagination .next a",
                    "li.next a"
            };
        } else {
            selectors = new String[]{
                    "[class*='prev']:not([disabled])",
                    "[aria-label='Previous']",
                    "[aria-label='previous']",
                    "a:has(> [class*='left'])",
                    "button:has(> [class*='left'])",
                    ".pagination .prev a",
                    "li.previous a"
            };
        }
        for (String selector : selectors) {
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                if (!elements.isEmpty() && elements.get(0).isDisplayed()) {
                    return elements.get(0);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void waitForRowsRefresh(WebDriver driver) {
        try {
            new WebDriverWait(driver, TIMEOUT).until(d -> !getDataRows(d).isEmpty());
        } catch (Exception e) {
            logger.debug("Row refresh wait timed out, proceeding");
        }
        invalidateColumnMap();
    }
}
