package kroviq.wrapper.primeng;

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

public class PrimeNGTableEngine extends AbstractTableEngine implements TablePaginator, TableSorter, TableFilter, TableSelector, TableStateAssert, TableExpander, TableEditor {

    private static final Logger logger = LogManager.getLogger(PrimeNGTableEngine.class);
    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("primeng");

    private final WebDriver driver;

    public PrimeNGTableEngine(WebElement tableRoot, WebDriver driver) {
        super(tableRoot);
        this.driver = driver;
    }

    @Override
    protected List<WebElement> getDataRows(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> rows = tableRoot.findElements(By.cssSelector(
                "tbody.p-datatable-tbody tr:not(.p-datatable-emptymessage)"));
        if (rows.isEmpty()) {
            rows = tableRoot.findElements(By.cssSelector(
                    "[class*='p-datatable-tbody'] tr"));
        }
        if (rows.isEmpty()) {
            rows = tableRoot.findElements(By.cssSelector("tbody tr"));
        }
        return rows.stream()
                .filter(row -> !row.findElements(By.cssSelector("td")).isEmpty())
                .toList();
    }

    @Override
    protected Map<String, Integer> buildColumnMap(WebDriver driver) {
        ensureTableRoot(driver);
        Map<String, Integer> map = new LinkedHashMap<>();
        List<WebElement> headers = tableRoot.findElements(By.cssSelector(
                "thead th, thead [role='columnheader']"));
        if (headers.isEmpty()) {
            headers = tableRoot.findElements(By.cssSelector(
                    ".p-datatable-thead th"));
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
        // PrimeNG cells may use p-tag or span wrappers
        try {
            WebElement tag = cell.findElement(By.cssSelector("[class*='p-tag']"));
            String tagText = tag.getText().trim();
            if (!tagText.isEmpty()) return tagText;
        } catch (Exception ignored) {}
        try {
            WebElement span = cell.findElement(By.cssSelector("span"));
            String spanText = span.getText().trim();
            if (!spanText.isEmpty()) return spanText;
        } catch (Exception ignored) {}
        return cell.getText().trim();
    }

    @Override
    protected void doPerformRowAction(WebDriver driver, WebElement row, String actionName) {
        WebElement actionElement = findActionInRow(driver, row, actionName);
        if (actionElement != null) {
            HandlerUtils.scrollIntoViewAndClick(driver, actionElement);
            return;
        }

        // PrimeNG menu button fallback (p-menu, p-tieredmenu, p-splitbutton)
        List<WebElement> menuBtns = row.findElements(By.cssSelector(
                "button[class*='p-button'], button[class*='menu'], [class*='p-splitbutton'] button"));
        if (menuBtns.isEmpty()) {
            menuBtns = row.findElements(By.xpath(
                    ".//button[.//span[contains(@class,'pi-ellipsis') or contains(@class,'pi-bars') or contains(@class,'pi-cog')]]"));
        }

        if (!menuBtns.isEmpty()) {
            WebElement menuBtn = menuBtns.get(menuBtns.size() - 1);
            HandlerUtils.scrollIntoViewAndClick(driver, menuBtn);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
            try {
                WebElement menuItem = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//ul[contains(@class,'p-menu-list') or contains(@class,'p-tieredmenu')]" +
                                "//span[normalize-space()='" + actionName + "'] | " +
                                "//div[contains(@class,'p-menu')]//a[normalize-space()='" + actionName + "'] | " +
                                "//div[contains(@class,'p-menu')]//*[normalize-space()='" + actionName + "']")));
                menuItem.click();
                return;
            } catch (Exception e) {
                logger.debug("PrimeNG menu item '{}' not found", actionName);
            }
        }

        throw new RuntimeException("Action '" + actionName + "' not found in PrimeNG table row. "
                + "Searched: button text, link text, title, aria-label, data-action, menu button");
    }

    @Override
    protected WebElement resolveTableRoot(WebDriver driver) {
        try {
            String id = tableRoot.getAttribute("id");
            if (id != null && !id.isEmpty()) {
                return driver.findElement(By.id(id));
            }
        } catch (Exception ignored) {}
        try {
            return driver.findElement(By.cssSelector("[class*='p-datatable']"));
        } catch (Exception ignored) {}
        return driver.findElement(By.cssSelector("table"));
    }

    // --- TableSorter ---

    @Override
    public void sortByColumn(WebDriver driver, String columnName, SortOrder order) {
        WebElement header = findPrimeNGSortableHeader(driver, columnName);
        SortOrder current = readPrimeNGSortState(header);
        int maxClicks = 3;
        while (current != order && maxClicks-- > 0) {
            HandlerUtils.clickSafe(driver, header);
            waitForTableRefresh(driver);
            header = findPrimeNGSortableHeader(driver, columnName);
            current = readPrimeNGSortState(header);
        }
    }

    @Override
    public SortOrder getCurrentSort(WebDriver driver, String columnName) {
        return readPrimeNGSortState(findPrimeNGSortableHeader(driver, columnName));
    }

    @Override
    public boolean verifySortOrder(WebDriver driver, String columnName, SortOrder expected) {
        List<String> values = getVisibleColumnValues(driver, columnName);
        return verifyDataSortOrder(values, expected);
    }

    private WebElement findPrimeNGSortableHeader(WebDriver driver, String columnName) {
        ensureTableRoot(driver);
        List<WebElement> headers = tableRoot.findElements(By.cssSelector(
                "th[pSortableColumn], th[psortablecolumn], thead th"));
        for (WebElement h : headers) {
            if (columnName.equalsIgnoreCase(h.getText().trim())) return h;
        }
        throw new RuntimeException("PrimeNG sortable header '" + columnName + "' not found");
    }

    private SortOrder readPrimeNGSortState(WebElement header) {
        String ariaSort = header.getAttribute("aria-sort");
        if (ariaSort != null) {
            if (ariaSort.equalsIgnoreCase("ascending")) return SortOrder.ASC;
            if (ariaSort.equalsIgnoreCase("descending")) return SortOrder.DESC;
        }
        // Check sort icon classes
        List<WebElement> ascIcons = header.findElements(By.cssSelector(
                ".pi-sort-amount-up-alt, .pi-sort-alpha-up, .pi-sort-numeric-up"));
        if (!ascIcons.isEmpty()) return SortOrder.ASC;
        List<WebElement> descIcons = header.findElements(By.cssSelector(
                ".pi-sort-amount-down, .pi-sort-alpha-down, .pi-sort-numeric-down"));
        if (!descIcons.isEmpty()) return SortOrder.DESC;
        return SortOrder.NONE;
    }

    // --- TableFilter ---

    @Override
    public void filterColumnContains(WebDriver driver, String columnName, String value) {
        WebElement filterInput = findPrimeNGColumnFilter(driver, columnName);
        HandlerUtils.clearAndType(driver, filterInput, value);
        waitForTableRefresh(driver);
    }

    @Override
    public void clearColumnFilter(WebDriver driver, String columnName) {
        WebElement filterInput = findPrimeNGColumnFilter(driver, columnName);
        HandlerUtils.clearAndType(driver, filterInput, "");
        filterInput.sendKeys(Keys.BACK_SPACE);
        waitForTableRefresh(driver);
    }

    @Override
    public void clearAllFilters(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> inputs = tableRoot.findElements(By.cssSelector(
                "input[class*='p-column-filter'], input[class*='p-inputtext'], thead input"));
        for (WebElement input : inputs) {
            try { HandlerUtils.clearAndType(driver, input, ""); } catch (Exception ignored) {}
        }
        waitForTableRefresh(driver);
    }

    @Override
    public void globalSearch(WebDriver driver, String searchText) {
        WebElement searchInput = findPrimeNGGlobalFilter(driver);
        HandlerUtils.clearAndType(driver, searchInput, searchText);
        waitForTableRefresh(driver);
    }

    @Override
    public void clearGlobalSearch(WebDriver driver) {
        WebElement searchInput = findPrimeNGGlobalFilter(driver);
        HandlerUtils.clearAndType(driver, searchInput, "");
        searchInput.sendKeys(Keys.BACK_SPACE);
        waitForTableRefresh(driver);
    }

    private WebElement findPrimeNGColumnFilter(WebDriver driver, String columnName) {
        ensureTableRoot(driver);
        Map<String, Integer> colMap = getColumnMap(driver);
        Integer colIndex = colMap.get(columnName);
        if (colIndex == null) {
            throw new RuntimeException("Column '" + columnName + "' not found for filtering. Available: " + colMap.keySet());
        }
        // PrimeNG inline column filters
        List<WebElement> filterInputs = tableRoot.findElements(By.cssSelector(
                "input[class*='p-column-filter'], input[class*='p-inputtext'][pColumnFilter]"));
        if (colIndex < filterInputs.size()) return filterInputs.get(colIndex);
        // Try filter row inputs
        List<WebElement> filterRow = tableRoot.findElements(By.cssSelector(
                "thead tr:last-child input, thead tr.p-datatable-filter-row input"));
        if (colIndex < filterRow.size()) return filterRow.get(colIndex);
        throw new RuntimeException("PrimeNG column filter for '" + columnName + "' not found. "
                + "Ensure column filters are configured in the table.");
    }

    private WebElement findPrimeNGGlobalFilter(WebDriver driver) {
        String[] selectors = {
                "input[pInputText][placeholder*='search' i]",
                "input[placeholder*='search' i]",
                ".p-input-icon-left input",
                "input[type='search']"
        };
        for (String sel : selectors) {
            List<WebElement> inputs = driver.findElements(By.cssSelector(sel));
            if (!inputs.isEmpty() && inputs.get(0).isDisplayed()) return inputs.get(0);
        }
        throw new RuntimeException("PrimeNG global filter input not found");
    }

    // --- TablePaginator ---

    @Override
    public void nextPage(WebDriver driver) {
        WebElement nextBtn = findPaginatorButton("next");
        if (nextBtn == null || !nextBtn.isEnabled() || isDisabled(nextBtn)) {
            throw new RuntimeException("PrimeNG table: no next page available");
        }
        nextBtn.click();
        waitForTableRefresh(driver);
    }

    @Override
    public void previousPage(WebDriver driver) {
        WebElement prevBtn = findPaginatorButton("prev");
        if (prevBtn == null || !prevBtn.isEnabled() || isDisabled(prevBtn)) {
            throw new RuntimeException("PrimeNG table: no previous page available");
        }
        prevBtn.click();
        waitForTableRefresh(driver);
    }

    @Override
    public void goToPage(WebDriver driver, int pageNumber) {
        List<WebElement> pageButtons = findPaginatorContainer()
                .findElements(By.cssSelector(".p-paginator-page, button[class*='p-paginator-page']"));
        for (WebElement btn : pageButtons) {
            if (String.valueOf(pageNumber).equals(btn.getText().trim())) {
                btn.click();
                waitForTableRefresh(driver);
                return;
            }
        }
        throw new RuntimeException("PrimeNG pagination: page " + pageNumber + " not found");
    }

    @Override
    public boolean hasNextPage(WebDriver driver) {
        WebElement nextBtn = findPaginatorButton("next");
        if (nextBtn == null) return false;
        return !isDisabled(nextBtn);
    }

    // --- Private helpers ---

    private WebElement findPaginatorButton(String direction) {
        WebElement container = findPaginatorContainer();
        if (container == null) return null;
        String selector = "next".equals(direction)
                ? "button[class*='p-paginator-next'], [class*='next']"
                : "button[class*='p-paginator-prev'], [class*='prev']";
        List<WebElement> btns = container.findElements(By.cssSelector(selector));
        return btns.isEmpty() ? null : btns.get(0);
    }

    private WebElement findPaginatorContainer() {
        List<WebElement> paginator = tableRoot.findElements(By.cssSelector(
                ".p-paginator, [class*='p-paginator']"));
        if (!paginator.isEmpty()) return paginator.get(0);
        try {
            return driver.findElement(By.cssSelector(".p-paginator, [class*='p-paginator']"));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isDisabled(WebElement element) {
        String className = element.getAttribute("class");
        String disabled = element.getAttribute("disabled");
        return "true".equals(disabled)
                || (className != null && className.contains("p-disabled"));
    }

    private void waitForTableRefresh(WebDriver driver) {
        try {
            new WebDriverWait(driver, TIMEOUT).until(d -> !getDataRows(d).isEmpty());
        } catch (Exception e) {
            logger.debug("PrimeNG table refresh wait completed");
        }
        invalidateColumnMap();
    }

    // --- TableSelector ---

    @Override
    public void selectRow(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findPrimeNGRowCheckbox(row);
        if (checkbox == null) throw new RuntimeException("No checkbox found in PrimeNG row where " + column + "='" + value + "'");
        togglePrimeNGCheckbox(driver, checkbox, true);
    }

    @Override
    public void deselectRow(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findPrimeNGRowCheckbox(row);
        if (checkbox == null) throw new RuntimeException("No checkbox found in PrimeNG row where " + column + "='" + value + "'");
        togglePrimeNGCheckbox(driver, checkbox, false);
    }

    @Override
    public void selectAll(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> headerCheckboxes = tableRoot.findElements(By.cssSelector(
                "th .p-checkbox-box, th p-tableHeaderCheckbox .p-checkbox-box"));
        if (headerCheckboxes.isEmpty()) {
            headerCheckboxes = tableRoot.findElements(By.cssSelector("thead input[type='checkbox']"));
        }
        if (headerCheckboxes.isEmpty()) throw new RuntimeException("No header checkbox found in PrimeNG table");
        togglePrimeNGCheckbox(driver, headerCheckboxes.get(0), true);
    }

    @Override
    public void deselectAll(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> headerCheckboxes = tableRoot.findElements(By.cssSelector(
                "th .p-checkbox-box, th p-tableHeaderCheckbox .p-checkbox-box"));
        if (headerCheckboxes.isEmpty()) {
            headerCheckboxes = tableRoot.findElements(By.cssSelector("thead input[type='checkbox']"));
        }
        if (headerCheckboxes.isEmpty()) throw new RuntimeException("No header checkbox found in PrimeNG table");
        togglePrimeNGCheckbox(driver, headerCheckboxes.get(0), false);
    }

    @Override
    public int getSelectedCount(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> highlighted = tableRoot.findElements(By.cssSelector(
                "tbody .p-checkbox-box.p-highlight, tbody .p-highlight .p-checkbox-box"));
        if (!highlighted.isEmpty()) return highlighted.size();
        // Fallback: count checked inputs
        List<WebElement> rows = getDataRows(driver);
        int count = 0;
        for (WebElement row : rows) {
            WebElement cb = findPrimeNGRowCheckbox(row);
            if (cb != null && isPrimeNGCheckboxChecked(cb)) count++;
        }
        return count;
    }

    @Override
    public boolean isRowSelected(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        // PrimeNG adds p-highlight class to selected rows
        String rowClass = row.getAttribute("class");
        if (rowClass != null && rowClass.contains("p-highlight")) return true;
        WebElement checkbox = findPrimeNGRowCheckbox(row);
        return checkbox != null && isPrimeNGCheckboxChecked(checkbox);
    }

    private WebElement findPrimeNGRowCheckbox(WebElement row) {
        List<WebElement> checkboxes = row.findElements(By.cssSelector(
                ".p-checkbox-box, p-tableCheckbox .p-checkbox-box"));
        if (!checkboxes.isEmpty()) return checkboxes.get(0);
        checkboxes = row.findElements(By.cssSelector("input[type='checkbox']"));
        return checkboxes.isEmpty() ? null : checkboxes.get(0);
    }

    private void togglePrimeNGCheckbox(WebDriver driver, WebElement checkbox, boolean targetState) {
        boolean current = isPrimeNGCheckboxChecked(checkbox);
        if (current != targetState) {
            HandlerUtils.clickSafe(driver, checkbox);
        }
    }

    private boolean isPrimeNGCheckboxChecked(WebElement checkbox) {
        String cls = checkbox.getAttribute("class");
        if (cls != null && cls.contains("p-highlight")) return true;
        // Check parent
        try {
            WebElement parent = checkbox.findElement(By.xpath(".."));
            String parentCls = parent.getAttribute("class");
            if (parentCls != null && parentCls.contains("p-highlight")) return true;
        } catch (Exception ignored) {}
        return isCheckboxChecked(checkbox);
    }

    // --- TableStateAssert ---

    @Override
    public boolean isLoading(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> overlays = tableRoot.findElements(By.cssSelector(
                ".p-datatable-loading-overlay, [class*='p-datatable-loading']"));
        for (WebElement overlay : overlays) {
            try { if (overlay.isDisplayed()) return true; } catch (Exception ignored) {}
        }
        return false;
    }

    @Override
    public boolean isEmpty(WebDriver driver) {
        if (isLoading(driver)) return false;
        ensureTableRoot(driver);
        List<WebElement> emptyMsg = tableRoot.findElements(By.cssSelector(
                ".p-datatable-emptymessage, tr.p-datatable-emptymessage"));
        for (WebElement el : emptyMsg) {
            try { if (el.isDisplayed()) return true; } catch (Exception ignored) {}
        }
        return getDataRows(driver).isEmpty();
    }

    @Override
    public String getEmptyStateMessage(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> emptyMsg = tableRoot.findElements(By.cssSelector(
                ".p-datatable-emptymessage td, .p-datatable-emptymessage"));
        for (WebElement el : emptyMsg) {
            try {
                if (el.isDisplayed()) {
                    String text = el.getText().trim();
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
        List<WebElement> togglers = row.findElements(By.cssSelector(
                "[pRowToggler], button[class*='p-row-toggler'], button[class*='row-toggler']"));
        if (togglers.isEmpty()) togglers = row.findElements(By.cssSelector("td:first-child button"));
        if (togglers.isEmpty()) throw new RuntimeException("No expand toggler in PrimeNG row where " + column + "='" + value + "'");
        HandlerUtils.clickSafe(driver, togglers.get(0));
        waitForExpandedContent(driver, row);
    }

    @Override
    public void collapseRow(WebDriver driver, String column, String value) {
        if (!isRowExpanded(driver, column, value)) return;
        WebElement row = findRow(driver, column, value);
        List<WebElement> togglers = row.findElements(By.cssSelector(
                "[pRowToggler], button[class*='p-row-toggler'], button[class*='row-toggler']"));
        if (togglers.isEmpty()) togglers = row.findElements(By.cssSelector("td:first-child button"));
        if (togglers.isEmpty()) throw new RuntimeException("No expand toggler in PrimeNG row where " + column + "='" + value + "'");
        HandlerUtils.clickSafe(driver, togglers.get(0));
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
        return cls != null && (cls.contains("editable") || cls.contains("p-editable-column"));
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
