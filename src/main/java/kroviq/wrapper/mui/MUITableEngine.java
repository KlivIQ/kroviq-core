package kroviq.wrapper.mui;

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

public class MUITableEngine extends AbstractTableEngine implements TablePaginator, TableSorter, TableFilter, TableSelector, TableStateAssert, TableExpander, TableEditor {

    private static final Logger logger = LogManager.getLogger(MUITableEngine.class);
    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("mui");

    private final WebDriver driver;

    public MUITableEngine(WebElement tableRoot, WebDriver driver) {
        super(tableRoot);
        this.driver = driver;
    }

    @Override
    protected List<WebElement> getDataRows(WebDriver driver) {
        ensureTableRoot(driver);
        // MUI DataGrid uses role='row' inside the data container
        List<WebElement> rows = tableRoot.findElements(By.cssSelector(
                "[class*='MuiDataGrid-row'], .MuiDataGrid-row"));
        if (rows.isEmpty()) {
            rows = tableRoot.findElements(By.cssSelector(
                    "[role='row']:not([class*='header']):not([class*='columnHeaders'])"));
            rows = rows.stream()
                    .filter(row -> !row.findElements(By.cssSelector("[role='gridcell'], td")).isEmpty())
                    .toList();
        }
        if (rows.isEmpty()) {
            rows = tableRoot.findElements(By.cssSelector("tbody tr"));
            rows = rows.stream()
                    .filter(row -> !row.findElements(By.cssSelector("td")).isEmpty())
                    .toList();
        }
        return rows;
    }

    @Override
    protected Map<String, Integer> buildColumnMap(WebDriver driver) {
        ensureTableRoot(driver);
        Map<String, Integer> map = new LinkedHashMap<>();
        // MUI DataGrid column headers (exclude the container which also matches)
        List<WebElement> headers = tableRoot.findElements(By.cssSelector(
                ".MuiDataGrid-columnHeader[role='columnheader']"));
        if (headers.isEmpty()) {
            headers = tableRoot.findElements(By.cssSelector(
                    "[class*='MuiDataGrid-columnHeader']:not([class*='MuiDataGrid-columnHeaders'])"));
        }
        if (!headers.isEmpty()) {
            for (int i = 0; i < headers.size(); i++) {
                try {
                    WebElement titleEl = headers.get(i).findElement(By.cssSelector(
                            ".MuiDataGrid-columnHeaderTitle, [class*='MuiDataGrid-columnHeaderTitle']"));
                    String text = titleEl.getText().trim();
                    if (!text.isEmpty()) map.put(text, i);
                } catch (Exception e) {
                    String text = headers.get(i).getText().trim();
                    if (!text.isEmpty()) map.put(text, i);
                }
            }
            return map;
        }
        // Fallback: standard table headers
        List<WebElement> ths = tableRoot.findElements(By.cssSelector("thead th"));
        if (ths.isEmpty()) {
            ths = tableRoot.findElements(By.cssSelector("[role='columnheader']"));
        }
        for (int i = 0; i < ths.size(); i++) {
            String text = ths.get(i).getText().trim();
            if (!text.isEmpty()) map.put(text, i);
        }
        return map;
    }

    @Override
    protected String extractCellText(WebElement cell) {
        // MUI Chip component
        try {
            WebElement chip = cell.findElement(By.cssSelector(
                    "[class*='MuiChip-label'], .MuiChip-label"));
            String chipText = chip.getText().trim();
            if (!chipText.isEmpty()) return chipText;
        } catch (Exception ignored) {}
        // MUI cell content wrapper
        try {
            WebElement content = cell.findElement(By.cssSelector(
                    "[class*='MuiDataGrid-cellContent']"));
            String text = content.getText().trim();
            if (!text.isEmpty()) return text;
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

        // MUI IconButton / Menu trigger fallback
        List<WebElement> menuBtns = row.findElements(By.cssSelector(
                "button[class*='MuiIconButton'], [class*='MuiIconButton-root']"));
        if (menuBtns.isEmpty()) {
            menuBtns = row.findElements(By.xpath(
                    ".//button[contains(@aria-label,'more') or contains(@aria-label,'actions') or contains(@aria-label,'menu')]"));
        }

        if (!menuBtns.isEmpty()) {
            WebElement menuBtn = menuBtns.get(menuBtns.size() - 1);
            HandlerUtils.scrollIntoViewAndClick(driver, menuBtn);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
            try {
                WebElement menuItem = wait.until(d -> {
                    List<WebElement> items = d.findElements(By.xpath(
                            "//ul[contains(@class,'MuiMenu-list') or @role='menu']" +
                            "//li[normalize-space()='" + actionName + "'] | " +
                            "//div[contains(@class,'MuiPopover')]//li[normalize-space()='" + actionName + "'] | " +
                            "//div[contains(@class,'MuiMenu')]//*[normalize-space()='" + actionName + "']"));
                    return items.isEmpty() ? null : items.get(0);
                });
                if (menuItem != null) {
                    menuItem.click();
                    return;
                }
            } catch (Exception e) {
                logger.debug("MUI menu item '{}' not found", actionName);
            }
        }

        throw new RuntimeException("Action '" + actionName + "' not found in MUI DataGrid row. "
                + "Searched: button text, link text, title, aria-label, data-action, MUI menu");
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
            return driver.findElement(By.cssSelector(
                    "[class*='MuiDataGrid-root'], .MuiDataGrid-root"));
        } catch (Exception ignored) {}
        return driver.findElement(By.cssSelector("table"));
    }

    @Override
    protected List<WebElement> getCellsFromRow(WebElement row) {
        List<WebElement> cells = row.findElements(By.cssSelector(
                "[role='gridcell'], [class*='MuiDataGrid-cell']"));
        if (cells.isEmpty()) {
            cells = row.findElements(By.cssSelector("td"));
        }
        return cells;
    }

    // --- TableSorter ---

    @Override
    public void sortByColumn(WebDriver driver, String columnName, SortOrder order) {
        WebElement header = findMUIColumnHeader(driver, columnName);
        SortOrder current = readMUISortState(header);
        int maxClicks = 3;
        while (current != order && maxClicks-- > 0) {
            HandlerUtils.clickSafe(driver, header);
            waitForTableRefresh(driver);
            header = findMUIColumnHeader(driver, columnName);
            current = readMUISortState(header);
        }
    }

    @Override
    public SortOrder getCurrentSort(WebDriver driver, String columnName) {
        return readMUISortState(findMUIColumnHeader(driver, columnName));
    }

    @Override
    public boolean verifySortOrder(WebDriver driver, String columnName, SortOrder expected) {
        List<String> values = getVisibleColumnValues(driver, columnName);
        return verifyDataSortOrder(values, expected);
    }

    private WebElement findMUIColumnHeader(WebDriver driver, String columnName) {
        ensureTableRoot(driver);
        List<WebElement> headers = tableRoot.findElements(By.cssSelector(
                ".MuiDataGrid-columnHeader[role='columnheader'], [class*='MuiDataGrid-columnHeader']"));
        for (WebElement h : headers) {
            try {
                WebElement titleEl = h.findElement(By.cssSelector(
                        ".MuiDataGrid-columnHeaderTitle, [class*='MuiDataGrid-columnHeaderTitle']"));
                if (columnName.equalsIgnoreCase(titleEl.getText().trim())) return h;
            } catch (Exception e) {
                if (columnName.equalsIgnoreCase(h.getText().trim())) return h;
            }
        }
        throw new RuntimeException("MUI DataGrid column header '" + columnName + "' not found");
    }

    private SortOrder readMUISortState(WebElement header) {
        String ariaSort = header.getAttribute("aria-sort");
        if (ariaSort != null) {
            if (ariaSort.equalsIgnoreCase("ascending")) return SortOrder.ASC;
            if (ariaSort.equalsIgnoreCase("descending")) return SortOrder.DESC;
        }
        // Check for sort icon presence
        List<WebElement> ascIcons = header.findElements(By.cssSelector(
                "[data-testid='ArrowUpwardIcon'], svg[class*='iconDirectionAsc']"));
        if (!ascIcons.isEmpty()) return SortOrder.ASC;
        List<WebElement> descIcons = header.findElements(By.cssSelector(
                "[data-testid='ArrowDownwardIcon'], svg[class*='iconDirectionDesc']"));
        if (!descIcons.isEmpty()) return SortOrder.DESC;
        return SortOrder.NONE;
    }

    // --- TableFilter ---

    @Override
    public void filterColumnContains(WebDriver driver, String columnName, String value) {
        // MUI DataGrid uses toolbar filter panel
        openMUIFilterPanel(driver);
        WebDriverWait wait = new WebDriverWait(driver, getSortTimeoutDuration());
        WebElement filterPanel = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".MuiDataGrid-filterForm, [class*='MuiDataGrid-filterForm'], .MuiDataGrid-panelContent")));
        // Select column in filter form
        List<WebElement> columnSelects = filterPanel.findElements(By.cssSelector(
                "select, [class*='MuiSelect'], input[placeholder*='Column' i]"));
        // Type filter value
        List<WebElement> valueInputs = filterPanel.findElements(By.cssSelector(
                "input[placeholder*='value' i], input[placeholder*='Value'], input[type='text']"));
        if (!valueInputs.isEmpty()) {
            HandlerUtils.clearAndType(driver, valueInputs.get(valueInputs.size() - 1), value);
        }
        waitForTableRefresh(driver);
    }

    @Override
    public void clearColumnFilter(WebDriver driver, String columnName) {
        openMUIFilterPanel(driver);
        WebDriverWait wait = new WebDriverWait(driver, getSortTimeoutDuration());
        try {
            WebElement filterPanel = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector(".MuiDataGrid-filterForm, [class*='MuiDataGrid-filterForm'], .MuiDataGrid-panelContent")));
            List<WebElement> deleteButtons = filterPanel.findElements(By.cssSelector(
                    "button[aria-label*='Delete' i], button[aria-label*='Remove' i], [data-testid*='delete' i]"));
            if (!deleteButtons.isEmpty()) deleteButtons.get(0).click();
        } catch (Exception ignored) {}
        waitForTableRefresh(driver);
    }

    @Override
    public void clearAllFilters(WebDriver driver) {
        openMUIFilterPanel(driver);
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement filterPanel = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector(".MuiDataGrid-panelContent, [class*='MuiDataGrid-panel']")));
            List<WebElement> deleteButtons = filterPanel.findElements(By.cssSelector(
                    "button[aria-label*='Delete' i], button[aria-label*='Remove' i]"));
            for (WebElement btn : deleteButtons) {
                try { btn.click(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        // Close panel
        try {
            driver.findElement(By.cssSelector("body")).click();
        } catch (Exception ignored) {}
        waitForTableRefresh(driver);
    }

    @Override
    public void globalSearch(WebDriver driver, String searchText) {
        WebElement quickFilter = findMUIQuickFilter(driver);
        HandlerUtils.clearAndType(driver, quickFilter, searchText);
        waitForTableRefresh(driver);
    }

    @Override
    public void clearGlobalSearch(WebDriver driver) {
        WebElement quickFilter = findMUIQuickFilter(driver);
        HandlerUtils.clearAndType(driver, quickFilter, "");
        quickFilter.sendKeys(Keys.BACK_SPACE);
        waitForTableRefresh(driver);
    }

    private void openMUIFilterPanel(WebDriver driver) {
        // Try toolbar filter button
        String[] selectors = {
                "button[aria-label*='filter' i]",
                "button[aria-label*='Filter']",
                ".MuiDataGrid-toolbarContainer button:has(svg)",
                "[data-testid*='filter' i]"
        };
        for (String sel : selectors) {
            List<WebElement> btns = driver.findElements(By.cssSelector(sel));
            for (WebElement btn : btns) {
                try {
                    if (btn.isDisplayed()) { btn.click(); return; }
                } catch (Exception ignored) {}
            }
        }
        throw new RuntimeException("MUI DataGrid filter button not found in toolbar. "
                + "Ensure the DataGrid toolbar with filter is configured.");
    }

    private WebElement findMUIQuickFilter(WebDriver driver) {
        String[] selectors = {
                ".MuiDataGrid-toolbarQuickFilter input",
                "[class*='MuiDataGrid-toolbarQuickFilter'] input",
                "input[placeholder*='search' i]",
                "input[type='search']"
        };
        for (String sel : selectors) {
            List<WebElement> inputs = driver.findElements(By.cssSelector(sel));
            if (!inputs.isEmpty() && inputs.get(0).isDisplayed()) return inputs.get(0);
        }
        throw new RuntimeException("MUI DataGrid quick filter input not found. "
                + "Ensure GridToolbarQuickFilter is configured in the toolbar.");
    }

    // --- TablePaginator ---

    @Override
    public void nextPage(WebDriver driver) {
        WebElement nextBtn = findPaginatorButton("next");
        if (nextBtn == null || !nextBtn.isEnabled() || isDisabled(nextBtn)) {
            throw new RuntimeException("MUI DataGrid: no next page available");
        }
        nextBtn.click();
        waitForTableRefresh(driver);
    }

    @Override
    public void previousPage(WebDriver driver) {
        WebElement prevBtn = findPaginatorButton("prev");
        if (prevBtn == null || !prevBtn.isEnabled() || isDisabled(prevBtn)) {
            throw new RuntimeException("MUI DataGrid: no previous page available");
        }
        prevBtn.click();
        waitForTableRefresh(driver);
    }

    @Override
    public void goToPage(WebDriver driver, int pageNumber) {
        // MUI pagination: navigate sequentially
        while (hasPrevPage(driver)) {
            previousPage(driver);
        }
        for (int i = 1; i < pageNumber; i++) {
            nextPage(driver);
        }
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
        String[] selectors = "next".equals(direction)
                ? new String[]{
                    "button[aria-label*='next' i]",
                    "button[aria-label*='Next']",
                    "button[class*='next']",
                    "[data-testid*='next']"}
                : new String[]{
                    "button[aria-label*='previous' i]",
                    "button[aria-label*='Previous']",
                    "button[class*='previous']",
                    "[data-testid*='previous']"};
        for (String sel : selectors) {
            List<WebElement> btns = container.findElements(By.cssSelector(sel));
            if (!btns.isEmpty()) return btns.get(0);
        }
        return null;
    }

    private WebElement findPaginatorContainer() {
        List<WebElement> paginator = tableRoot.findElements(By.cssSelector(
                "[class*='MuiTablePagination'], [class*='MuiDataGrid-footerContainer']"));
        if (!paginator.isEmpty()) return paginator.get(0);
        try {
            return driver.findElement(By.cssSelector(
                    "[class*='MuiTablePagination'], [class*='MuiDataGrid-footerContainer']"));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasPrevPage(WebDriver driver) {
        WebElement prevBtn = findPaginatorButton("prev");
        if (prevBtn == null) return false;
        return !isDisabled(prevBtn);
    }

    private boolean isDisabled(WebElement element) {
        String disabled = element.getAttribute("disabled");
        String ariaDisabled = element.getAttribute("aria-disabled");
        String className = element.getAttribute("class");
        return "true".equals(disabled) || "true".equals(ariaDisabled)
                || (className != null && className.contains("Mui-disabled"));
    }

    private void waitForTableRefresh(WebDriver driver) {
        try {
            new WebDriverWait(driver, TIMEOUT).until(d -> !getDataRows(d).isEmpty());
        } catch (Exception e) {
            logger.debug("MUI DataGrid refresh wait completed");
        }
        invalidateColumnMap();
    }

    // --- TableSelector ---

    @Override
    public void selectRow(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findMUIRowCheckbox(row);
        if (checkbox == null) throw new RuntimeException("No checkbox found in MUI DataGrid row where " + column + "='" + value + "'");
        toggleMUICheckbox(driver, checkbox, true);
    }

    @Override
    public void deselectRow(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findMUIRowCheckbox(row);
        if (checkbox == null) throw new RuntimeException("No checkbox found in MUI DataGrid row where " + column + "='" + value + "'");
        toggleMUICheckbox(driver, checkbox, false);
    }

    @Override
    public void selectAll(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> headerCheckboxes = tableRoot.findElements(By.cssSelector(
                ".MuiDataGrid-columnHeaderCheckbox input[type='checkbox'], " +
                "[class*='MuiDataGrid-columnHeaderCheckbox'] input"));
        if (headerCheckboxes.isEmpty()) throw new RuntimeException("No header checkbox found in MUI DataGrid");
        toggleMUICheckbox(driver, headerCheckboxes.get(0), true);
    }

    @Override
    public void deselectAll(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> headerCheckboxes = tableRoot.findElements(By.cssSelector(
                ".MuiDataGrid-columnHeaderCheckbox input[type='checkbox'], " +
                "[class*='MuiDataGrid-columnHeaderCheckbox'] input"));
        if (headerCheckboxes.isEmpty()) throw new RuntimeException("No header checkbox found in MUI DataGrid");
        toggleMUICheckbox(driver, headerCheckboxes.get(0), false);
    }

    @Override
    public int getSelectedCount(WebDriver driver) {
        ensureTableRoot(driver);
        // MUI adds Mui-selected class to selected rows
        List<WebElement> selected = tableRoot.findElements(By.cssSelector(
                ".MuiDataGrid-row.Mui-selected, [class*='MuiDataGrid-row'][class*='Mui-selected']"));
        if (!selected.isEmpty()) return selected.size();
        // Fallback: count checked checkboxes in body
        List<WebElement> rows = getDataRows(driver);
        int count = 0;
        for (WebElement row : rows) {
            WebElement cb = findMUIRowCheckbox(row);
            if (cb != null && isMUICheckboxChecked(cb)) count++;
        }
        return count;
    }

    @Override
    public boolean isRowSelected(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        String rowClass = row.getAttribute("class");
        if (rowClass != null && rowClass.contains("Mui-selected")) return true;
        WebElement checkbox = findMUIRowCheckbox(row);
        return checkbox != null && isMUICheckboxChecked(checkbox);
    }

    private WebElement findMUIRowCheckbox(WebElement row) {
        List<WebElement> checkboxes = row.findElements(By.cssSelector(
                ".MuiDataGrid-cellCheckbox input[type='checkbox'], " +
                "[class*='MuiDataGrid-cellCheckbox'] input, " +
                "[class*='MuiCheckbox'] input[type='checkbox']"));
        if (!checkboxes.isEmpty()) return checkboxes.get(0);
        checkboxes = row.findElements(By.cssSelector("input[type='checkbox']"));
        return checkboxes.isEmpty() ? null : checkboxes.get(0);
    }

    private void toggleMUICheckbox(WebDriver driver, WebElement checkbox, boolean targetState) {
        boolean current = isMUICheckboxChecked(checkbox);
        if (current != targetState) {
            HandlerUtils.clickSafe(driver, checkbox);
        }
    }

    private boolean isMUICheckboxChecked(WebElement checkbox) {
        try {
            WebElement wrapper = checkbox.findElement(By.xpath(
                    "ancestor::*[contains(@class,'MuiCheckbox')]"));
            String cls = wrapper.getAttribute("class");
            if (cls != null && cls.contains("Mui-checked")) return true;
        } catch (Exception ignored) {}
        return isCheckboxChecked(checkbox);
    }

    // --- TableStateAssert ---

    @Override
    public boolean isLoading(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> overlays = tableRoot.findElements(By.cssSelector(
                ".MuiDataGrid-overlay"));
        for (WebElement overlay : overlays) {
            try {
                if (overlay.isDisplayed()) {
                    // Check if it contains a loading indicator
                    List<WebElement> loaders = overlay.findElements(By.cssSelector(
                            "[class*='MuiCircularProgress'], [class*='MuiLinearProgress'], [role='progressbar']"));
                    if (!loaders.isEmpty()) return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    @Override
    public boolean isEmpty(WebDriver driver) {
        if (isLoading(driver)) return false;
        ensureTableRoot(driver);
        List<WebElement> overlays = tableRoot.findElements(By.cssSelector(
                ".MuiDataGrid-overlay"));
        for (WebElement overlay : overlays) {
            try {
                if (overlay.isDisplayed()) {
                    String text = overlay.getText().trim().toLowerCase();
                    if (text.contains("no rows") || text.contains("no data") || text.contains("no results")) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }
        return getDataRows(driver).isEmpty();
    }

    @Override
    public String getEmptyStateMessage(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> overlays = tableRoot.findElements(By.cssSelector(
                ".MuiDataGrid-overlay"));
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
        WebElement toggle = findExpandToggle(row);
        if (toggle == null) throw new RuntimeException("No expand toggle in MUI DataGrid row where " + column + "='" + value + "'");
        HandlerUtils.clickSafe(driver, toggle);
        waitForExpandedContent(driver, row);
    }

    @Override
    public void collapseRow(WebDriver driver, String column, String value) {
        if (!isRowExpanded(driver, column, value)) return;
        WebElement row = findRow(driver, column, value);
        WebElement toggle = findExpandToggle(row);
        if (toggle == null) throw new RuntimeException("No expand toggle in MUI DataGrid row where " + column + "='" + value + "'");
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
        return cls != null && (cls.contains("editable") || cls.contains("MuiDataGrid-cell--editable"));
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
