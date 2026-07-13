package kroviq.wrapper.angularmaterial;

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
import org.openqa.selenium.support.ui.WebDriverWait;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AngularMaterialTableEngine extends AbstractTableEngine implements TablePaginator, TableSorter, TableFilter, TableSelector, TableStateAssert, TableExpander, TableEditor {

    private static final Logger logger = LogManager.getLogger(AngularMaterialTableEngine.class);
    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("angularmaterial");

    private final WebDriver driver;

    public AngularMaterialTableEngine(WebElement tableRoot, WebDriver driver) {
        super(tableRoot);
        this.driver = driver;
    }

    @Override
    protected List<WebElement> getDataRows(WebDriver driver) {
        ensureTableRoot(driver);
        // mat-table uses mat-row or tr[mat-row]
        List<WebElement> rows = tableRoot.findElements(By.cssSelector(
                "mat-row, tr[mat-row], tr.mat-mdc-row, [class*='mat-row']"));
        if (rows.isEmpty()) {
            rows = tableRoot.findElements(By.cssSelector("tbody tr"));
            rows = rows.stream()
                    .filter(row -> !row.findElements(By.cssSelector("td, mat-cell")).isEmpty())
                    .toList();
        }
        return rows;
    }

    @Override
    protected Map<String, Integer> buildColumnMap(WebDriver driver) {
        ensureTableRoot(driver);
        Map<String, Integer> map = new LinkedHashMap<>();
        List<WebElement> headers = tableRoot.findElements(By.cssSelector(
                "mat-header-cell, th[mat-header-cell], th.mat-mdc-header-cell, [class*='mat-header-cell']"));
        if (headers.isEmpty()) {
            headers = tableRoot.findElements(By.cssSelector("thead th"));
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
        // Angular Material may use mat-chip or span
        try {
            WebElement chip = cell.findElement(By.cssSelector("[class*='mat-chip'], [class*='mat-mdc-chip']"));
            String chipText = chip.getText().trim();
            if (!chipText.isEmpty()) return chipText;
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

        // Angular Material mat-menu trigger fallback
        List<WebElement> menuBtns = row.findElements(By.cssSelector(
                "button[mat-icon-button], button[class*='mat-icon-button'], " +
                "button[class*='mat-mdc-icon-button'], [matMenuTriggerFor]"));
        if (menuBtns.isEmpty()) {
            menuBtns = row.findElements(By.xpath(
                    ".//button[.//mat-icon[text()='more_vert' or text()='more_horiz']]"));
        }

        if (!menuBtns.isEmpty()) {
            WebElement menuBtn = menuBtns.get(menuBtns.size() - 1);
            HandlerUtils.scrollIntoViewAndClick(driver, menuBtn);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
            try {
                WebElement menuItem = wait.until(d -> {
                    List<WebElement> items = d.findElements(By.xpath(
                            "//div[contains(@class,'mat-menu-panel') or contains(@class,'mat-mdc-menu-panel')]" +
                            "//button[normalize-space()='" + actionName + "'] | " +
                            "//div[contains(@class,'cdk-overlay')]//button[normalize-space()='" + actionName + "'] | " +
                            "//div[contains(@class,'mat-menu')]//*[normalize-space()='" + actionName + "']"));
                    return items.isEmpty() ? null : items.get(0);
                });
                if (menuItem != null) {
                    menuItem.click();
                    return;
                }
            } catch (Exception e) {
                logger.debug("Angular Material menu item '{}' not found", actionName);
            }
        }

        throw new RuntimeException("Action '" + actionName + "' not found in Angular Material table row. "
                + "Searched: button text, link text, title, aria-label, data-action, mat-menu");
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
                    "mat-table, table[mat-table], [class*='mat-table']"));
        } catch (Exception ignored) {}
        return driver.findElement(By.cssSelector("table"));
    }

    @Override
    protected List<WebElement> getCellsFromRow(WebElement row) {
        List<WebElement> cells = row.findElements(By.cssSelector(
                "mat-cell, td[mat-cell], td.mat-mdc-cell, [class*='mat-cell']"));
        if (cells.isEmpty()) {
            cells = row.findElements(By.cssSelector("td"));
        }
        return cells;
    }

    // --- TableSorter ---

    @Override
    public void sortByColumn(WebDriver driver, String columnName, SortOrder order) {
        WebElement header = findMatSortHeader(driver, columnName);
        SortOrder current = readMatSortState(header);
        int maxClicks = 3;
        while (current != order && maxClicks-- > 0) {
            HandlerUtils.clickSafe(driver, header);
            waitForTableRefresh(driver);
            header = findMatSortHeader(driver, columnName);
            current = readMatSortState(header);
        }
    }

    @Override
    public SortOrder getCurrentSort(WebDriver driver, String columnName) {
        return readMatSortState(findMatSortHeader(driver, columnName));
    }

    @Override
    public boolean verifySortOrder(WebDriver driver, String columnName, SortOrder expected) {
        List<String> values = getVisibleColumnValues(driver, columnName);
        return verifyDataSortOrder(values, expected);
    }

    private WebElement findMatSortHeader(WebDriver driver, String columnName) {
        ensureTableRoot(driver);
        List<WebElement> headers = tableRoot.findElements(By.cssSelector(
                "[mat-sort-header], [class*='mat-sort-header'], th"));
        for (WebElement h : headers) {
            if (columnName.equalsIgnoreCase(h.getText().trim())) return h;
        }
        throw new RuntimeException("Angular Material sort header '" + columnName + "' not found");
    }

    private SortOrder readMatSortState(WebElement header) {
        String ariaSort = header.getAttribute("aria-sort");
        if (ariaSort != null) {
            if (ariaSort.equalsIgnoreCase("ascending")) return SortOrder.ASC;
            if (ariaSort.equalsIgnoreCase("descending")) return SortOrder.DESC;
        }
        // Check mat-sort-header-sorted class
        String cls = header.getAttribute("class");
        if (cls != null && cls.contains("mat-sort-header-sorted")) {
            List<WebElement> indicators = header.findElements(By.cssSelector(
                    ".mat-sort-header-arrow"));
            // Angular Material uses transform to indicate direction
            // Fallback: check aria-sort on parent th
            try {
                WebElement th = header.findElement(By.xpath("ancestor-or-self::th"));
                String thSort = th.getAttribute("aria-sort");
                if ("ascending".equalsIgnoreCase(thSort)) return SortOrder.ASC;
                if ("descending".equalsIgnoreCase(thSort)) return SortOrder.DESC;
            } catch (Exception ignored) {}
        }
        return SortOrder.NONE;
    }

    // --- TableFilter ---

    @Override
    public void filterColumnContains(WebDriver driver, String columnName, String value) {
        throw new UnsupportedOperationException(
                "Column filtering is not supported by AngularMaterialTableEngine. "
                + "Angular Material tables do not provide native per-column filter UI. "
                + "Use globalSearch() instead.");
    }

    @Override
    public void clearColumnFilter(WebDriver driver, String columnName) {
        throw new UnsupportedOperationException(
                "Column filtering is not supported by AngularMaterialTableEngine. "
                + "Angular Material tables do not provide native per-column filter UI. "
                + "Use clearGlobalSearch() instead.");
    }

    @Override
    public void clearAllFilters(WebDriver driver) {
        clearGlobalSearch(driver);
    }

    @Override
    public void globalSearch(WebDriver driver, String searchText) {
        WebElement searchInput = findMatGlobalFilter(driver);
        HandlerUtils.clearAndType(driver, searchInput, searchText);
        waitForTableRefresh(driver);
    }

    @Override
    public void clearGlobalSearch(WebDriver driver) {
        WebElement searchInput = findMatGlobalFilter(driver);
        HandlerUtils.clearAndType(driver, searchInput, "");
        searchInput.sendKeys(Keys.BACK_SPACE);
        waitForTableRefresh(driver);
    }

    private WebElement findMatGlobalFilter(WebDriver driver) {
        String[] selectors = {
                "input[matInput][placeholder*='filter' i]",
                "input[matInput][placeholder*='search' i]",
                "mat-form-field input",
                "input[placeholder*='search' i]",
                "input[placeholder*='filter' i]",
                "input[type='search']"
        };
        for (String sel : selectors) {
            List<WebElement> inputs = driver.findElements(By.cssSelector(sel));
            if (!inputs.isEmpty() && inputs.get(0).isDisplayed()) return inputs.get(0);
        }
        throw new RuntimeException("Angular Material global filter input not found");
    }

    // --- TablePaginator ---

    @Override
    public void nextPage(WebDriver driver) {
        WebElement nextBtn = findPaginatorButton("next");
        if (nextBtn == null || !nextBtn.isEnabled() || isDisabled(nextBtn)) {
            throw new RuntimeException("Angular Material table: no next page available");
        }
        nextBtn.click();
        waitForTableRefresh(driver);
    }

    @Override
    public void previousPage(WebDriver driver) {
        WebElement prevBtn = findPaginatorButton("prev");
        if (prevBtn == null || !prevBtn.isEnabled() || isDisabled(prevBtn)) {
            throw new RuntimeException("Angular Material table: no previous page available");
        }
        prevBtn.click();
        waitForTableRefresh(driver);
    }

    @Override
    public void goToPage(WebDriver driver, int pageNumber) {
        // mat-paginator doesn't have direct page buttons; navigate sequentially
        // Go to first page
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
        String selector = "next".equals(direction)
                ? "button[class*='mat-paginator-navigation-next'], button[class*='next'], button[aria-label*='Next']"
                : "button[class*='mat-paginator-navigation-previous'], button[class*='prev'], button[aria-label*='Previous']";
        List<WebElement> btns = container.findElements(By.cssSelector(selector));
        return btns.isEmpty() ? null : btns.get(0);
    }

    private WebElement findPaginatorContainer() {
        List<WebElement> paginator = tableRoot.findElements(By.cssSelector(
                "mat-paginator, [class*='mat-paginator'], [class*='mat-mdc-paginator']"));
        if (!paginator.isEmpty()) return paginator.get(0);
        try {
            return driver.findElement(By.cssSelector(
                    "mat-paginator, [class*='mat-paginator'], [class*='mat-mdc-paginator']"));
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
        return "true".equals(disabled) || "true".equals(ariaDisabled);
    }

    private void waitForTableRefresh(WebDriver driver) {
        try {
            new WebDriverWait(driver, TIMEOUT).until(d -> !getDataRows(d).isEmpty());
        } catch (Exception e) {
            logger.debug("Angular Material table refresh wait completed");
        }
        invalidateColumnMap();
    }

    // --- TableSelector ---

    @Override
    public void selectRow(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findMatRowCheckbox(row);
        if (checkbox == null) throw new RuntimeException("No checkbox found in Angular Material row where " + column + "='" + value + "'");
        toggleMatCheckbox(driver, checkbox, true);
    }

    @Override
    public void deselectRow(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findMatRowCheckbox(row);
        if (checkbox == null) throw new RuntimeException("No checkbox found in Angular Material row where " + column + "='" + value + "'");
        toggleMatCheckbox(driver, checkbox, false);
    }

    @Override
    public void selectAll(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> headerCheckboxes = tableRoot.findElements(By.cssSelector(
                "th mat-checkbox input, th [class*='mat-checkbox'] input, th [class*='mat-mdc-checkbox'] input"));
        if (headerCheckboxes.isEmpty()) throw new RuntimeException("No header checkbox found in Angular Material table");
        toggleMatCheckbox(driver, headerCheckboxes.get(0), true);
    }

    @Override
    public void deselectAll(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> headerCheckboxes = tableRoot.findElements(By.cssSelector(
                "th mat-checkbox input, th [class*='mat-checkbox'] input, th [class*='mat-mdc-checkbox'] input"));
        if (headerCheckboxes.isEmpty()) throw new RuntimeException("No header checkbox found in Angular Material table");
        toggleMatCheckbox(driver, headerCheckboxes.get(0), false);
    }

    @Override
    public int getSelectedCount(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> checked = tableRoot.findElements(By.cssSelector(
                "tbody .mat-mdc-checkbox-checked, tbody .mat-checkbox-checked"));
        if (!checked.isEmpty()) return checked.size();
        List<WebElement> rows = getDataRows(driver);
        int count = 0;
        for (WebElement row : rows) {
            WebElement cb = findMatRowCheckbox(row);
            if (cb != null && isMatCheckboxChecked(cb)) count++;
        }
        return count;
    }

    @Override
    public boolean isRowSelected(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findMatRowCheckbox(row);
        return checkbox != null && isMatCheckboxChecked(checkbox);
    }

    private WebElement findMatRowCheckbox(WebElement row) {
        List<WebElement> checkboxes = row.findElements(By.cssSelector(
                "mat-checkbox input, [class*='mat-checkbox'] input, [class*='mat-mdc-checkbox'] input"));
        if (!checkboxes.isEmpty()) return checkboxes.get(0);
        checkboxes = row.findElements(By.cssSelector("input[type='checkbox']"));
        return checkboxes.isEmpty() ? null : checkboxes.get(0);
    }

    private void toggleMatCheckbox(WebDriver driver, WebElement checkbox, boolean targetState) {
        boolean current = isMatCheckboxChecked(checkbox);
        if (current != targetState) {
            // Angular Material: click the mat-checkbox wrapper, not the input directly
            try {
                WebElement wrapper = checkbox.findElement(By.xpath(
                        "ancestor::mat-checkbox | ancestor::*[contains(@class,'mat-checkbox') or contains(@class,'mat-mdc-checkbox')]"));
                HandlerUtils.clickSafe(driver, wrapper);
            } catch (Exception e) {
                HandlerUtils.clickSafe(driver, checkbox);
            }
        }
    }

    private boolean isMatCheckboxChecked(WebElement checkbox) {
        try {
            WebElement wrapper = checkbox.findElement(By.xpath(
                    "ancestor::mat-checkbox | ancestor::*[contains(@class,'mat-checkbox') or contains(@class,'mat-mdc-checkbox')]"));
            String cls = wrapper.getAttribute("class");
            if (cls != null && (cls.contains("mat-mdc-checkbox-checked") || cls.contains("mat-checkbox-checked"))) {
                return true;
            }
        } catch (Exception ignored) {}
        return isCheckboxChecked(checkbox);
    }

    // --- TableStateAssert ---

    @Override
    public boolean isLoading(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> loaders = tableRoot.findElements(By.cssSelector(
                "mat-progress-bar, mat-spinner, [class*='mat-progress-bar'], [class*='mat-spinner']"));
        for (WebElement loader : loaders) {
            try { if (loader.isDisplayed()) return true; } catch (Exception ignored) {}
        }
        return false;
    }

    @Override
    public boolean isEmpty(WebDriver driver) {
        if (isLoading(driver)) return false;
        return getDataRows(driver).isEmpty();
    }

    @Override
    public String getEmptyStateMessage(WebDriver driver) {
        ensureTableRoot(driver);
        // Angular Material uses custom empty row or template
        List<WebElement> emptyRows = tableRoot.findElements(By.cssSelector(
                "tr.no-data, tr.empty-row, [class*='no-data'], [class*='empty']"));
        for (WebElement el : emptyRows) {
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
        WebElement toggle = findExpandToggle(row);
        if (toggle == null) throw new RuntimeException("No expand toggle in Angular Material row where " + column + "='" + value + "'");
        HandlerUtils.clickSafe(driver, toggle);
        waitForExpandedContent(driver, row);
    }

    @Override
    public void collapseRow(WebDriver driver, String column, String value) {
        if (!isRowExpanded(driver, column, value)) return;
        WebElement row = findRow(driver, column, value);
        WebElement toggle = findExpandToggle(row);
        if (toggle == null) throw new RuntimeException("No expand toggle in Angular Material row where " + column + "='" + value + "'");
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
        return cls != null && cls.contains("editable");
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
