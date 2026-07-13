package kroviq.wrapper.aggrid;

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
import org.openqa.selenium.JavascriptExecutor;
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

public class AgGridTableEngine extends AbstractTableEngine implements TablePaginator, TableSorter, TableFilter, TableSelector, TableStateAssert, TableExpander, TableEditor {

    private static final Logger logger = LogManager.getLogger(AgGridTableEngine.class);
    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("aggrid");
    private static final int MAX_SCROLL_ATTEMPTS = HandlerUtils.getMaxScrollAttempts("aggrid");

    // AG Grid pagination selectors
    private static final String PAGING_PANEL = ".ag-paging-panel";
    private static final String NEXT_BUTTON = ".ag-paging-button[ref='btNext'], button[ref='btNext'], .ag-paging-panel button:last-child";
    private static final String PREV_BUTTON = ".ag-paging-button[ref='btPrevious'], button[ref='btPrevious'], .ag-paging-panel button:first-child";
    private static final String PAGE_BUTTONS = ".ag-paging-panel .ag-paging-page-summary-panel";

    public AgGridTableEngine(WebElement tableRoot) {
        super(tableRoot);
    }

    @Override
    protected List<WebElement> getDataRows(WebDriver driver) {
        ensureTableRoot(driver);
        return tableRoot.findElements(By.cssSelector(AgGridLocators.ROW));
    }

    @Override
    protected Map<String, Integer> buildColumnMap(WebDriver driver) {
        ensureTableRoot(driver);
        Map<String, Integer> map = new LinkedHashMap<>();
        List<WebElement> headers = tableRoot.findElements(By.cssSelector(AgGridLocators.HEADER_CELL));
        for (int i = 0; i < headers.size(); i++) {
            try {
                WebElement textEl = headers.get(i).findElement(By.cssSelector(AgGridLocators.HEADER_CELL_TEXT));
                String text = textEl.getText().trim();
                if (!text.isEmpty()) {
                    map.put(text, i);
                }
            } catch (Exception e) {
                // Header cell without text element (e.g., checkbox column)
            }
        }
        return map;
    }

    @Override
    protected String extractCellText(WebElement cell) {
        String text = cell.getText().trim();
        if (!text.isEmpty()) return text;
        // AG Grid cell renderers may use inner elements
        try {
            WebElement inner = cell.findElement(By.cssSelector(".ag-cell-value, [ref='eValue']"));
            text = inner.getText().trim();
            if (!text.isEmpty()) return text;
        } catch (Exception ignored) {}
        // Check for input inside cell (editable grid)
        try {
            WebElement input = cell.findElement(By.cssSelector("input"));
            String value = input.getAttribute("value");
            if (value != null && !value.isEmpty()) return value;
        } catch (Exception ignored) {}
        return "";
    }

    @Override
    protected void doPerformRowAction(WebDriver driver, WebElement row, String actionName) {
        // Priority 1-5: standard action resolution
        WebElement actionElement = findActionInRow(driver, row, actionName);
        if (actionElement != null) {
            HandlerUtils.scrollIntoViewAndClick(driver, actionElement);
            return;
        }

        // Priority 6: AG Grid context menu or cell renderer buttons
        List<WebElement> cellButtons = row.findElements(By.cssSelector(
                ".ag-cell button, .ag-cell a, .ag-cell [role='button']"));
        for (WebElement btn : cellButtons) {
            String btnText = btn.getText().trim();
            String title = btn.getAttribute("title");
            String ariaLabel = btn.getAttribute("aria-label");
            if (actionName.equalsIgnoreCase(btnText)
                    || actionName.equals(title)
                    || actionName.equals(ariaLabel)) {
                HandlerUtils.scrollIntoViewAndClick(driver, btn);
                return;
            }
        }

        // Priority 7: meatball/ellipsis menu in AG Grid
        List<WebElement> menuTriggers = row.findElements(By.cssSelector(
                "button[class*='menu'], button[class*='action'], [class*='kebab'], [class*='more']"));
        if (!menuTriggers.isEmpty()) {
            HandlerUtils.scrollIntoViewAndClick(driver, menuTriggers.get(0));
            WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
            try {
                WebElement menuItem = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//*[contains(@class,'ag-menu') or contains(@class,'dropdown') or contains(@class,'popup')]"
                                + "//*[normalize-space()='" + actionName + "']")));
                menuItem.click();
                return;
            } catch (Exception e) {
                logger.debug("AG Grid menu item '{}' not found", actionName);
            }
        }

        throw new RuntimeException("Action '" + actionName + "' not found in AG Grid row. "
                + "Searched: button text, link text, title, aria-label, data-action, cell buttons, menu trigger");
    }

    @Override
    protected WebElement resolveTableRoot(WebDriver driver) {
        try {
            String id = tableRoot.getAttribute("id");
            if (id != null && !id.isEmpty()) {
                return driver.findElement(By.id(id));
            }
        } catch (Exception ignored) {}
        // Re-find by AG Grid markers
        return driver.findElement(By.cssSelector(
                AgGridLocators.ROOT_WRAPPER + ", [class*='ag-root'], [class*='ag-grid']"));
    }

    @Override
    protected List<WebElement> getCellsFromRow(WebElement row) {
        return row.findElements(By.cssSelector(AgGridLocators.CELL));
    }

    // --- Override findRow to handle AG Grid virtualization ---

    @Override
    public WebElement findRow(WebDriver driver, String columnHeader, String value) {
        logger.info("[TableEngine] AG Grid: searching row where {}='{}'", columnHeader, value);
        // First try without scrolling (current viewport)
        WebElement row = searchVisibleRows(driver, columnHeader, value);
        if (row != null) {
            logger.info("[TableEngine] Row found in current viewport");
            return row;
        }

        // Try scrolling through virtualized rows
        row = searchWithScroll(driver, columnHeader, value);
        if (row != null) {
            logger.info("[TableEngine] Row found after scroll");
            return row;
        }

        // Try pagination if available
        if (hasPagination(driver)) {
            row = searchWithPagination(driver, columnHeader, value);
            if (row != null) return row;
        }

        throw new RuntimeException("Row not found where " + columnHeader + "='" + value
                + "' in AG Grid after scroll + pagination search");
    }

    @Override
    public WebElement findRowByCriteria(WebDriver driver, Map<String, String> criteria) {
        logger.info("[TableEngine] AG Grid: searching row by criteria: {}", criteria);
        // Search current viewport
        WebElement row = searchVisibleRowsByCriteria(driver, criteria);
        if (row != null) {
            logger.info("[TableEngine] Row found in current viewport");
            return row;
        }

        // Try scrolling
        row = searchWithScrollByCriteria(driver, criteria);
        if (row != null) {
            logger.info("[TableEngine] Row found after scroll");
            return row;
        }

        // Try pagination
        if (hasPagination(driver)) {
            row = searchWithPaginationByCriteria(driver, criteria);
            if (row != null) return row;
        }

        throw new RuntimeException("Row not found matching criteria " + criteria
                + " in AG Grid after scroll + pagination search");
    }

    // --- TablePaginator ---

    @Override
    public void nextPage(WebDriver driver) {
        ensureTableRoot(driver);
        WebElement nextBtn = findNextButton(driver);
        if (nextBtn == null || !nextBtn.isEnabled() || "true".equals(nextBtn.getAttribute("disabled"))) {
            throw new RuntimeException("AG Grid: no next page available");
        }
        nextBtn.click();
        waitForGridRefresh(driver);
    }

    @Override
    public void previousPage(WebDriver driver) {
        ensureTableRoot(driver);
        WebElement prevBtn = findPrevButton(driver);
        if (prevBtn == null || !prevBtn.isEnabled() || "true".equals(prevBtn.getAttribute("disabled"))) {
            throw new RuntimeException("AG Grid: no previous page available");
        }
        prevBtn.click();
        waitForGridRefresh(driver);
    }

    @Override
    public void goToPage(WebDriver driver, int pageNumber) {
        // AG Grid uses page size + offset; direct page navigation via input if available
        ensureTableRoot(driver);
        List<WebElement> pageInputs = tableRoot.findElements(By.cssSelector(
                ".ag-paging-panel input[type='number'], .ag-paging-panel input[ref='lbCurrent']"));
        if (!pageInputs.isEmpty()) {
            WebElement input = pageInputs.get(0);
            input.clear();
            input.sendKeys(String.valueOf(pageNumber));
            input.sendKeys(org.openqa.selenium.Keys.ENTER);
            waitForGridRefresh(driver);
            return;
        }
        // Fallback: navigate sequentially
        // Go to first page first, then forward
        while (hasPrevPage(driver)) {
            previousPage(driver);
        }
        for (int i = 1; i < pageNumber; i++) {
            nextPage(driver);
        }
    }

    @Override
    public boolean hasNextPage(WebDriver driver) {
        ensureTableRoot(driver);
        WebElement nextBtn = findNextButton(driver);
        if (nextBtn == null) return false;
        String disabled = nextBtn.getAttribute("disabled");
        String ariaDisabled = nextBtn.getAttribute("aria-disabled");
        return !"true".equals(disabled) && !"true".equals(ariaDisabled)
                && !nextBtn.getAttribute("class").contains("ag-disabled");
    }

    // --- TableSorter ---

    @Override
    public void sortByColumn(WebDriver driver, String columnName, SortOrder order) {
        WebElement header = findAgGridHeaderCell(driver, columnName);
        SortOrder current = readAgGridSortState(header);
        int maxClicks = 3;
        while (current != order && maxClicks-- > 0) {
            WebElement label = header.findElement(By.cssSelector(AgGridLocators.HEADER_CELL_TEXT));
            HandlerUtils.clickSafe(driver, label);
            waitForGridRefresh(driver);
            header = findAgGridHeaderCell(driver, columnName);
            current = readAgGridSortState(header);
        }
    }

    @Override
    public SortOrder getCurrentSort(WebDriver driver, String columnName) {
        return readAgGridSortState(findAgGridHeaderCell(driver, columnName));
    }

    @Override
    public boolean verifySortOrder(WebDriver driver, String columnName, SortOrder expected) {
        List<String> values = getVisibleColumnValues(driver, columnName);
        return verifyDataSortOrder(values, expected);
    }

    private WebElement findAgGridHeaderCell(WebDriver driver, String columnName) {
        ensureTableRoot(driver);
        List<WebElement> headers = tableRoot.findElements(By.cssSelector(AgGridLocators.HEADER_CELL));
        for (WebElement h : headers) {
            try {
                WebElement textEl = h.findElement(By.cssSelector(AgGridLocators.HEADER_CELL_TEXT));
                if (columnName.equalsIgnoreCase(textEl.getText().trim())) return h;
            } catch (Exception ignored) {}
        }
        throw new RuntimeException("AG Grid header '" + columnName + "' not found");
    }

    private SortOrder readAgGridSortState(WebElement header) {
        String ariaSort = header.getAttribute("aria-sort");
        if (ariaSort != null) {
            if (ariaSort.equalsIgnoreCase("ascending")) return SortOrder.ASC;
            if (ariaSort.equalsIgnoreCase("descending")) return SortOrder.DESC;
        }
        // Fallback: check sort icon classes
        List<WebElement> ascIcons = header.findElements(By.cssSelector(".ag-sort-ascending-icon:not(.ag-hidden)"));
        if (!ascIcons.isEmpty()) return SortOrder.ASC;
        List<WebElement> descIcons = header.findElements(By.cssSelector(".ag-sort-descending-icon:not(.ag-hidden)"));
        if (!descIcons.isEmpty()) return SortOrder.DESC;
        return SortOrder.NONE;
    }

    // --- TableFilter ---

    @Override
    public void filterColumnContains(WebDriver driver, String columnName, String value) {
        WebElement filterInput = findFloatingFilterInput(driver, columnName);
        HandlerUtils.clearAndType(driver, filterInput, value);
        waitForGridRefresh(driver);
    }

    @Override
    public void clearColumnFilter(WebDriver driver, String columnName) {
        WebElement filterInput = findFloatingFilterInput(driver, columnName);
        HandlerUtils.clearAndType(driver, filterInput, "");
        filterInput.sendKeys(Keys.BACK_SPACE);
        waitForGridRefresh(driver);
    }

    @Override
    public void clearAllFilters(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> inputs = tableRoot.findElements(By.cssSelector(
                ".ag-floating-filter-input input, .ag-floating-filter input"));
        for (WebElement input : inputs) {
            try {
                HandlerUtils.clearAndType(driver, input, "");
                input.sendKeys(Keys.BACK_SPACE);
            } catch (Exception ignored) {}
        }
        waitForGridRefresh(driver);
    }

    @Override
    public void globalSearch(WebDriver driver, String searchText) {
        WebElement quickFilter = findQuickFilterInput(driver);
        HandlerUtils.clearAndType(driver, quickFilter, searchText);
        waitForGridRefresh(driver);
    }

    @Override
    public void clearGlobalSearch(WebDriver driver) {
        WebElement quickFilter = findQuickFilterInput(driver);
        HandlerUtils.clearAndType(driver, quickFilter, "");
        quickFilter.sendKeys(Keys.BACK_SPACE);
        waitForGridRefresh(driver);
    }

    private WebElement findFloatingFilterInput(WebDriver driver, String columnName) {
        ensureTableRoot(driver);
        Map<String, Integer> colMap = getColumnMap(driver);
        Integer colIndex = colMap.get(columnName);
        if (colIndex == null) {
            throw new RuntimeException("Column '" + columnName + "' not found for filtering. Available: " + colMap.keySet());
        }
        // AG Grid floating filters are aligned with header columns
        List<WebElement> floatingFilters = tableRoot.findElements(By.cssSelector(
                ".ag-floating-filter"));
        if (colIndex < floatingFilters.size()) {
            List<WebElement> inputs = floatingFilters.get(colIndex).findElements(By.cssSelector(
                    "input, .ag-floating-filter-input input, .ag-text-field-input"));
            if (!inputs.isEmpty()) return inputs.get(0);
        }
        throw new RuntimeException("AG Grid floating filter for '" + columnName + "' not found. "
                + "Ensure floating filters are enabled in the grid configuration.");
    }

    private WebElement findQuickFilterInput(WebDriver driver) {
        String[] selectors = {
                "input.ag-text-field-input[id*='quick']",
                "input[placeholder*='search' i]",
                "input[placeholder*='filter' i]",
                ".ag-text-field-input",
                "input[type='search']"
        };
        for (String sel : selectors) {
            List<WebElement> inputs = driver.findElements(By.cssSelector(sel));
            for (WebElement input : inputs) {
                // Exclude floating filter inputs (they're inside the grid)
                try {
                    if (input.isDisplayed() && !isInsideGrid(input)) return input;
                } catch (Exception ignored) {}
            }
        }
        throw new RuntimeException("AG Grid quick filter input not found. "
                + "Ensure a quick filter input is configured outside the grid.");
    }

    private boolean isInsideGrid(WebElement element) {
        try {
            String outerHtml = element.findElement(By.xpath("ancestor::*[contains(@class,'ag-root')]")).getTagName();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // --- Private helpers ---

    private WebElement searchVisibleRows(WebDriver driver, String columnHeader, String value) {
        Map<String, Integer> colMap = getColumnMap(driver);
        Integer colIndex = colMap.get(columnHeader);
        if (colIndex == null) {
            throw new RuntimeException("Column '" + columnHeader + "' not found in AG Grid. Available: " + colMap.keySet());
        }
        List<WebElement> rows = getDataRows(driver);
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.cssSelector(AgGridLocators.CELL));
            if (colIndex < cells.size()) {
                String cellText = extractCellText(cells.get(colIndex));
                if (value.equalsIgnoreCase(cellText)) return row;
            }
        }
        return null;
    }

    private WebElement searchWithScroll(WebDriver driver, String columnHeader, String value) {
        WebElement viewport = null;
        try {
            viewport = tableRoot.findElement(By.cssSelector(AgGridLocators.BODY_VIEWPORT));
        } catch (Exception e) {
            return null;
        }

        JavascriptExecutor js = (JavascriptExecutor) driver;
        // Reset scroll to top
        js.executeScript("arguments[0].scrollTop = 0;", viewport);
        waitForGridRefresh(driver);

        int scrollStep = viewport.getSize().getHeight();
        long prevScrollTop = -1;

        for (int attempt = 0; attempt < MAX_SCROLL_ATTEMPTS; attempt++) {
            WebElement row = searchVisibleRows(driver, columnHeader, value);
            if (row != null) return row;

            Long currentScrollTop = (Long) js.executeScript("return arguments[0].scrollTop;", viewport);
            if (currentScrollTop.equals(prevScrollTop)) break; // Reached bottom
            prevScrollTop = currentScrollTop;

            js.executeScript("arguments[0].scrollTop += arguments[1];", viewport, scrollStep);
            waitForGridRefresh(driver);
        }
        return null;
    }

    private WebElement searchWithPagination(WebDriver driver, String columnHeader, String value) {
        int maxPages = kroviq.utils.LoadProperties.getInt("table.maxPagesToSearch", 10);
        long maxTime = kroviq.utils.LoadProperties.getInt("table.maxRowSearchTimeSeconds", 30) * 1000L;
        long startTime = System.currentTimeMillis();

        for (int page = 0; page < maxPages - 1; page++) {
            if (System.currentTimeMillis() - startTime > maxTime) break;
            if (!hasNextPage(driver)) break;
            nextPage(driver);
            invalidateColumnMap();
            WebElement row = searchVisibleRows(driver, columnHeader, value);
            if (row != null) {
                logger.info("[TableEngine] Row found after {} pages", page + 2);
                return row;
            }
        }
        return null;
    }

    private WebElement searchVisibleRowsByCriteria(WebDriver driver, Map<String, String> criteria) {
        Map<String, Integer> colMap = getColumnMap(driver);
        for (String col : criteria.keySet()) {
            if (!colMap.containsKey(col)) {
                throw new RuntimeException("Column '" + col + "' not found in AG Grid. Available: " + colMap.keySet());
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

    private WebElement searchWithScrollByCriteria(WebDriver driver, Map<String, String> criteria) {
        WebElement viewport;
        try {
            viewport = tableRoot.findElement(By.cssSelector(AgGridLocators.BODY_VIEWPORT));
        } catch (Exception e) {
            return null;
        }

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollTop = 0;", viewport);
        waitForGridRefresh(driver);

        int scrollStep = viewport.getSize().getHeight();
        long prevScrollTop = -1;

        for (int attempt = 0; attempt < MAX_SCROLL_ATTEMPTS; attempt++) {
            WebElement row = searchVisibleRowsByCriteria(driver, criteria);
            if (row != null) return row;

            Long currentScrollTop = (Long) js.executeScript("return arguments[0].scrollTop;", viewport);
            if (currentScrollTop.equals(prevScrollTop)) break;
            prevScrollTop = currentScrollTop;

            js.executeScript("arguments[0].scrollTop += arguments[1];", viewport, scrollStep);
            waitForGridRefresh(driver);
        }
        return null;
    }

    private WebElement searchWithPaginationByCriteria(WebDriver driver, Map<String, String> criteria) {
        int maxPages = kroviq.utils.LoadProperties.getInt("table.maxPagesToSearch", 10);
        long maxTime = kroviq.utils.LoadProperties.getInt("table.maxRowSearchTimeSeconds", 30) * 1000L;
        long startTime = System.currentTimeMillis();

        for (int page = 0; page < maxPages - 1; page++) {
            if (System.currentTimeMillis() - startTime > maxTime) break;
            if (!hasNextPage(driver)) break;
            nextPage(driver);
            invalidateColumnMap();
            WebElement row = searchVisibleRowsByCriteria(driver, criteria);
            if (row != null) {
                logger.info("[TableEngine] Row found after {} pages", page + 2);
                return row;
            }
        }
        return null;
    }

    private boolean hasPagination(WebDriver driver) {
        return !tableRoot.findElements(By.cssSelector(PAGING_PANEL)).isEmpty();
    }

    private boolean hasPrevPage(WebDriver driver) {
        WebElement prevBtn = findPrevButton(driver);
        if (prevBtn == null) return false;
        String disabled = prevBtn.getAttribute("disabled");
        return !"true".equals(disabled) && !prevBtn.getAttribute("class").contains("ag-disabled");
    }

    private WebElement findNextButton(WebDriver driver) {
        for (String selector : NEXT_BUTTON.split(", ")) {
            List<WebElement> btns = tableRoot.findElements(By.cssSelector(selector.trim()));
            if (!btns.isEmpty()) return btns.get(0);
        }
        return null;
    }

    private WebElement findPrevButton(WebDriver driver) {
        for (String selector : PREV_BUTTON.split(", ")) {
            List<WebElement> btns = tableRoot.findElements(By.cssSelector(selector.trim()));
            if (!btns.isEmpty()) return btns.get(0);
        }
        return null;
    }

    private void waitForGridRefresh(WebDriver driver) {
        try {
            Thread.sleep(200); // Brief settle for AG Grid virtual DOM update
            new WebDriverWait(driver, Duration.ofSeconds(2)).until(d -> {
                List<WebElement> rows = tableRoot.findElements(By.cssSelector(AgGridLocators.ROW));
                return !rows.isEmpty();
            });
        } catch (Exception e) {
            logger.debug("AG Grid refresh wait completed");
        }
        invalidateColumnMap();
    }

    // --- TableSelector ---

    @Override
    public void selectRow(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findAgGridRowCheckbox(row);
        if (checkbox == null) throw new RuntimeException("No checkbox found in AG Grid row where " + column + "='" + value + "'");
        toggleCheckbox(driver, checkbox, true);
    }

    @Override
    public void deselectRow(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findAgGridRowCheckbox(row);
        if (checkbox == null) throw new RuntimeException("No checkbox found in AG Grid row where " + column + "='" + value + "'");
        toggleCheckbox(driver, checkbox, false);
    }

    @Override
    public void selectAll(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> headerCheckboxes = tableRoot.findElements(By.cssSelector(
                ".ag-header-select-all .ag-checkbox-input, .ag-header-select-all input[type='checkbox']"));
        if (headerCheckboxes.isEmpty()) throw new RuntimeException("No header checkbox found in AG Grid");
        toggleCheckbox(driver, headerCheckboxes.get(0), true);
    }

    @Override
    public void deselectAll(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> headerCheckboxes = tableRoot.findElements(By.cssSelector(
                ".ag-header-select-all .ag-checkbox-input, .ag-header-select-all input[type='checkbox']"));
        if (headerCheckboxes.isEmpty()) throw new RuntimeException("No header checkbox found in AG Grid");
        toggleCheckbox(driver, headerCheckboxes.get(0), false);
    }

    @Override
    public int getSelectedCount(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> rows = getDataRows(driver);
        int count = 0;
        for (WebElement row : rows) {
            WebElement checkbox = findAgGridRowCheckbox(row);
            if (checkbox != null && isCheckboxChecked(checkbox)) count++;
        }
        return count;
    }

    @Override
    public boolean isRowSelected(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findAgGridRowCheckbox(row);
        if (checkbox == null) return false;
        // AG Grid also uses row-level class for selection
        String rowClass = row.getAttribute("class");
        if (rowClass != null && rowClass.contains("ag-row-selected")) return true;
        return isCheckboxChecked(checkbox);
    }

    private WebElement findAgGridRowCheckbox(WebElement row) {
        List<WebElement> checkboxes = row.findElements(By.cssSelector(
                ".ag-selection-checkbox .ag-checkbox-input, .ag-selection-checkbox input[type='checkbox']"));
        if (!checkboxes.isEmpty()) return checkboxes.get(0);
        checkboxes = row.findElements(By.cssSelector("input[type='checkbox']"));
        return checkboxes.isEmpty() ? null : checkboxes.get(0);
    }

    // --- TableStateAssert ---

    @Override
    public boolean isLoading(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> overlays = tableRoot.findElements(By.cssSelector(
                ".ag-overlay-loading-wrapper"));
        for (WebElement overlay : overlays) {
            try { if (overlay.isDisplayed()) return true; } catch (Exception ignored) {}
        }
        return false;
    }

    @Override
    public boolean isEmpty(WebDriver driver) {
        if (isLoading(driver)) return false;
        ensureTableRoot(driver);
        List<WebElement> noRows = tableRoot.findElements(By.cssSelector(
                ".ag-overlay-no-rows-wrapper"));
        for (WebElement overlay : noRows) {
            try { if (overlay.isDisplayed()) return true; } catch (Exception ignored) {}
        }
        return getDataRows(driver).isEmpty();
    }

    @Override
    public String getEmptyStateMessage(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> overlays = tableRoot.findElements(By.cssSelector(
                ".ag-overlay-no-rows-wrapper, .ag-overlay-no-rows-center"));
        for (WebElement overlay : overlays) {
            try {
                if (overlay.isDisplayed()) {
                    String text = overlay.getText().trim();
                    if (!text.isEmpty()) return text;
                }
            } catch (Exception ignored) {}
        }
        return "";
    }

    // --- TableExpander ---

    @Override
    public void expandRow(WebDriver driver, String column, String value) {
        if (isRowExpanded(driver, column, value)) return;
        WebElement row = findRow(driver, column, value);
        WebElement toggle = findAgGridExpandToggle(row);
        if (toggle == null) throw new RuntimeException("No expand toggle in AG Grid row where " + column + "='" + value + "'");
        HandlerUtils.clickSafe(driver, toggle);
        waitForExpandedContent(driver, row);
    }

    @Override
    public void collapseRow(WebDriver driver, String column, String value) {
        if (!isRowExpanded(driver, column, value)) return;
        WebElement row = findRow(driver, column, value);
        WebElement toggle = findAgGridExpandToggle(row);
        if (toggle == null) throw new RuntimeException("No expand toggle in AG Grid row where " + column + "='" + value + "'");
        HandlerUtils.clickSafe(driver, toggle);
        waitForCollapsed(driver, row);
    }

    @Override
    public boolean isRowExpanded(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        // AG Grid uses ag-row-group-expanded class or detail row presence
        String cls = row.getAttribute("class");
        if (cls != null && cls.contains("ag-row-group-expanded")) return true;
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

    private WebElement findAgGridExpandToggle(WebElement row) {
        List<WebElement> toggles = row.findElements(By.cssSelector(
                ".ag-group-expanded, .ag-group-contracted, .ag-row-group-leaf-indent + .ag-group-child-count"));
        if (!toggles.isEmpty()) return toggles.get(0);
        return findExpandToggle(row);
    }

    // --- TableEditor ---

    @Override
    public void editCell(WebDriver driver, String rowColumn, String rowValue,
                         String targetColumn, String newValue) {
        WebElement row = findRow(driver, rowColumn, rowValue);
        WebElement cell = getCellByColumn(driver, row, targetColumn);
        activateCellForEdit(driver, cell);
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
        String cls = cell.getAttribute("class");
        return cls != null && (cls.contains("ag-cell-editable") || cls.contains("editable"));
    }

    private void performEditByType(WebDriver driver, WebElement cell, String value, CellEditType type) {
        switch (type) {
            case TEXT_INPUT -> performTextEdit(driver, cell, value);
            case DROPDOWN -> performDropdownEdit(driver, cell, value);
            case CHECKBOX -> performCheckboxEdit(driver, cell, value);
            case TOGGLE -> performToggleEdit(driver, cell, value);
        }
    }
}
