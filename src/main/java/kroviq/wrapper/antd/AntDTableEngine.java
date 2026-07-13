package kroviq.wrapper.antd;

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

public class AntDTableEngine extends AbstractTableEngine implements TablePaginator, TableSorter, TableFilter, TableSelector, TableStateAssert, TableExpander, TableEditor {

    private static final Logger logger = LogManager.getLogger(AntDTableEngine.class);
    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("antd");

    private final WebDriver driver;

    public AntDTableEngine(WebElement tableRoot, WebDriver driver) {
        super(tableRoot);
        this.driver = driver;
    }

    @Override
    protected List<WebElement> getDataRows(WebDriver driver) {
        ensureTableRoot(driver);
        // AntD table body rows (exclude measure rows and hidden rows)
        List<WebElement> rows = tableRoot.findElements(By.xpath(
                ".//tbody[contains(@class,'ant-table-tbody')]/tr[not(contains(@class,'ant-table-measure-row')) and not(@aria-hidden='true')]"));
        if (rows.isEmpty()) {
            // Fallback: split-table layout (body in separate div)
            rows = tableRoot.findElements(By.xpath(
                    ".//div[contains(@class,'ant-table-body')]//tbody/tr[not(contains(@class,'ant-table-measure-row')) and not(@aria-hidden='true')]"));
        }
        return rows;
    }

    @Override
    protected Map<String, Integer> buildColumnMap(WebDriver driver) {
        ensureTableRoot(driver);
        Map<String, Integer> map = new LinkedHashMap<>();
        // Try thead within the table element
        List<WebElement> headers = tableRoot.findElements(By.xpath(
                ".//thead//th[not(contains(@class,'ant-table-cell-scrollbar'))]"));
        // AntD split-header: thead in sibling table under ant-table-header
        if (headers.isEmpty()) {
            headers = tableRoot.findElements(By.xpath(
                    ".//div[contains(@class,'ant-table-header')]//thead//th[not(contains(@class,'ant-table-cell-scrollbar'))]"));
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
        // AntD cells often wrap text in spans or tags
        try {
            WebElement tag = cell.findElement(By.xpath(".//span[contains(@class,'ant-tag')]"));
            String tagText = tag.getText().trim();
            if (!tagText.isEmpty()) return tagText;
        } catch (Exception ignored) {}
        try {
            WebElement span = cell.findElement(By.xpath(".//span"));
            String spanText = span.getText().trim();
            if (!spanText.isEmpty()) return spanText;
        } catch (Exception ignored) {}
        return cell.getText().trim();
    }

    @Override
    protected void doPerformRowAction(WebDriver driver, WebElement row, String actionName) {
        // Priority 1-5: standard action resolution
        WebElement actionElement = findActionInRow(driver, row, actionName);
        if (actionElement != null) {
            HandlerUtils.scrollIntoViewAndClick(driver, actionElement);
            return;
        }

        // Priority 6: AntD meatball/ellipsis menu
        List<WebElement> meatballBtns = row.findElements(By.xpath(
                ".//button[contains(@id,'MeatBallMenu') or contains(@class,'ant-dropdown-trigger') "
                        + "or contains(@class,'more') or contains(@class,'action')]"));
        if (meatballBtns.isEmpty()) {
            // Try generic ellipsis patterns
            meatballBtns = row.findElements(By.xpath(
                    ".//button[.//span[contains(@class,'anticon-more') or contains(@class,'anticon-ellipsis')]]"));
        }

        if (!meatballBtns.isEmpty()) {
            WebElement menuBtn = meatballBtns.get(0);
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center'});", menuBtn);
            new WebDriverWait(driver, TIMEOUT).until(ExpectedConditions.elementToBeClickable(menuBtn)).click();

            WebDriverWait menuWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            try {
                WebElement menuItem = menuWait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//div[contains(@class,'ant-dropdown')]//span[normalize-space()='" + actionName + "'] | "
                                + "//ul[contains(@class,'ant-dropdown-menu')]//span[normalize-space()='" + actionName + "'] | "
                                + "//div[contains(@class,'ant-dropdown')]//*[normalize-space()='" + actionName + "']")));
                menuItem.click();
                return;
            } catch (Exception e) {
                logger.debug("AntD dropdown menu item '{}' not found", actionName);
            }
        }

        throw new RuntimeException("Action '" + actionName + "' not found in AntD table row. "
                + "Searched: button text, link text, title, aria-label, data-action, meatball menu");
    }

    @Override
    protected WebElement resolveTableRoot(WebDriver driver) {
        try {
            String id = tableRoot.getAttribute("id");
            if (id != null && !id.isEmpty()) {
                return driver.findElement(By.id(id));
            }
        } catch (Exception ignored) {}
        // Re-find by AntD table class
        try {
            String className = tableRoot.getAttribute("class");
            if (className != null && className.contains("ant-table")) {
                return driver.findElement(By.cssSelector(".ant-table-wrapper, .ant-table"));
            }
        } catch (Exception ignored) {}
        return driver.findElement(By.cssSelector("[class*='ant-table']"));
    }

    @Override
    protected List<WebElement> getCellsFromRow(WebElement row) {
        return row.findElements(By.xpath(".//td[contains(@class,'ant-table-cell')]"));
    }

    // --- TableSorter ---

    @Override
    public void sortByColumn(WebDriver driver, String columnName, SortOrder order) {
        WebElement header = findAntDSortableHeader(driver, columnName);
        SortOrder current = readAntDSortState(header);
        int maxClicks = 3;
        while (current != order && maxClicks-- > 0) {
            WebElement sorter = header.findElement(By.cssSelector(
                    ".ant-table-column-sorters, .ant-table-column-sorter, *"));
            HandlerUtils.clickSafe(driver, sorter);
            waitForTableRefresh(driver);
            header = findAntDSortableHeader(driver, columnName);
            current = readAntDSortState(header);
        }
    }

    @Override
    public SortOrder getCurrentSort(WebDriver driver, String columnName) {
        return readAntDSortState(findAntDSortableHeader(driver, columnName));
    }

    @Override
    public boolean verifySortOrder(WebDriver driver, String columnName, SortOrder expected) {
        List<String> values = getVisibleColumnValues(driver, columnName);
        return verifyDataSortOrder(values, expected);
    }

    private WebElement findAntDSortableHeader(WebDriver driver, String columnName) {
        ensureTableRoot(driver);
        List<WebElement> headers = tableRoot.findElements(By.xpath(
                ".//thead//th[not(contains(@class,'ant-table-cell-scrollbar'))]"));
        for (WebElement h : headers) {
            if (columnName.equalsIgnoreCase(h.getText().trim())) return h;
        }
        throw new RuntimeException("AntD sortable header '" + columnName + "' not found");
    }

    private SortOrder readAntDSortState(WebElement header) {
        String ariaSort = header.getAttribute("aria-sort");
        if (ariaSort != null) {
            if (ariaSort.equalsIgnoreCase("ascending")) return SortOrder.ASC;
            if (ariaSort.equalsIgnoreCase("descending")) return SortOrder.DESC;
        }
        String cls = header.getAttribute("class");
        if (cls != null && cls.contains("ant-table-column-sort")) {
            // Check sorter icon direction
            List<WebElement> ascActive = header.findElements(By.cssSelector(
                    ".ant-table-column-sorter-up.active"));
            if (!ascActive.isEmpty()) return SortOrder.ASC;
            List<WebElement> descActive = header.findElements(By.cssSelector(
                    ".ant-table-column-sorter-down.active"));
            if (!descActive.isEmpty()) return SortOrder.DESC;
        }
        return SortOrder.NONE;
    }

    // --- TableFilter ---

    @Override
    public void filterColumnContains(WebDriver driver, String columnName, String value) {
        WebElement header = findAntDSortableHeader(driver, columnName);
        // Click filter trigger icon
        List<WebElement> filterTriggers = header.findElements(By.cssSelector(
                ".ant-table-filter-trigger, [class*='ant-table-filter-trigger']"));
        if (filterTriggers.isEmpty()) {
            throw new RuntimeException("AntD column filter trigger for '" + columnName + "' not found. "
                    + "Ensure column filters are configured in the table.");
        }
        HandlerUtils.clickSafe(driver, filterTriggers.get(0));
        // Wait for filter dropdown
        WebDriverWait wait = new WebDriverWait(driver, getSortTimeoutDuration());
        WebElement filterDropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".ant-table-filter-dropdown, .ant-dropdown")));
        // Find input in dropdown and type
        List<WebElement> inputs = filterDropdown.findElements(By.cssSelector("input"));
        if (!inputs.isEmpty()) {
            HandlerUtils.clearAndType(driver, inputs.get(0), value);
        }
        // Click confirm/OK button
        List<WebElement> confirmBtns = filterDropdown.findElements(By.cssSelector(
                "button.ant-btn-primary, button[class*='confirm'], .ant-table-filter-dropdown-btns .ant-btn-primary"));
        if (!confirmBtns.isEmpty()) {
            confirmBtns.get(0).click();
        }
        waitForTableRefresh(driver);
    }

    @Override
    public void clearColumnFilter(WebDriver driver, String columnName) {
        WebElement header = findAntDSortableHeader(driver, columnName);
        List<WebElement> filterTriggers = header.findElements(By.cssSelector(
                ".ant-table-filter-trigger, [class*='ant-table-filter-trigger']"));
        if (filterTriggers.isEmpty()) {
            throw new RuntimeException("AntD column filter trigger for '" + columnName + "' not found.");
        }
        HandlerUtils.clickSafe(driver, filterTriggers.get(0));
        WebDriverWait wait = new WebDriverWait(driver, getSortTimeoutDuration());
        WebElement filterDropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".ant-table-filter-dropdown, .ant-dropdown")));
        // Click reset button
        List<WebElement> resetBtns = filterDropdown.findElements(By.cssSelector(
                "button.ant-btn-link, button[class*='reset'], .ant-table-filter-dropdown-btns .ant-btn-link"));
        if (!resetBtns.isEmpty()) {
            resetBtns.get(0).click();
        }
        waitForTableRefresh(driver);
    }

    @Override
    public void clearAllFilters(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> activeFilters = tableRoot.findElements(By.cssSelector(
                ".ant-table-filter-trigger.active, .ant-table-filter-trigger-container-open"));
        for (WebElement trigger : activeFilters) {
            try {
                HandlerUtils.clickSafe(driver, trigger);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
                WebElement dropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".ant-table-filter-dropdown, .ant-dropdown")));
                List<WebElement> resetBtns = dropdown.findElements(By.cssSelector(
                        "button.ant-btn-link, .ant-table-filter-dropdown-btns .ant-btn-link"));
                if (!resetBtns.isEmpty()) resetBtns.get(0).click();
            } catch (Exception ignored) {}
        }
        waitForTableRefresh(driver);
    }

    @Override
    public void globalSearch(WebDriver driver, String searchText) {
        WebElement searchInput = findAntDGlobalSearch(driver);
        HandlerUtils.clearAndType(driver, searchInput, searchText);
        waitForTableRefresh(driver);
    }

    @Override
    public void clearGlobalSearch(WebDriver driver) {
        WebElement searchInput = findAntDGlobalSearch(driver);
        HandlerUtils.clearAndType(driver, searchInput, "");
        searchInput.sendKeys(Keys.BACK_SPACE);
        waitForTableRefresh(driver);
    }

    private WebElement findAntDGlobalSearch(WebDriver driver) {
        String[] selectors = {
                "input.ant-input[type='search']",
                ".ant-input-search input",
                "input[placeholder*='search' i]",
                "input[placeholder*='Search']",
                ".ant-input-affix-wrapper input"
        };
        for (String sel : selectors) {
            List<WebElement> inputs = driver.findElements(By.cssSelector(sel));
            if (!inputs.isEmpty() && inputs.get(0).isDisplayed()) return inputs.get(0);
        }
        throw new RuntimeException("AntD global search input not found");
    }

    // --- TablePaginator ---

    @Override
    public void nextPage(WebDriver driver) {
        WebElement nextBtn = findAntDPaginationButton("next");
        if (nextBtn == null || !nextBtn.isEnabled()
                || nextBtn.getAttribute("class").contains("ant-pagination-disabled")) {
            throw new RuntimeException("AntD table: no next page available");
        }
        nextBtn.click();
        waitForTableRefresh(driver);
    }

    @Override
    public void previousPage(WebDriver driver) {
        WebElement prevBtn = findAntDPaginationButton("prev");
        if (prevBtn == null || !prevBtn.isEnabled()
                || prevBtn.getAttribute("class").contains("ant-pagination-disabled")) {
            throw new RuntimeException("AntD table: no previous page available");
        }
        prevBtn.click();
        waitForTableRefresh(driver);
    }

    @Override
    public void goToPage(WebDriver driver, int pageNumber) {
        List<WebElement> pageItems = findPaginationContainer()
                .findElements(By.cssSelector(".ant-pagination-item"));
        for (WebElement item : pageItems) {
            String title = item.getAttribute("title");
            if (String.valueOf(pageNumber).equals(title)) {
                item.click();
                waitForTableRefresh(driver);
                return;
            }
        }
        throw new RuntimeException("AntD pagination: page " + pageNumber + " not found");
    }

    @Override
    public boolean hasNextPage(WebDriver driver) {
        WebElement nextBtn = findAntDPaginationButton("next");
        if (nextBtn == null) return false;
        String className = nextBtn.getAttribute("class");
        return className != null && !className.contains("ant-pagination-disabled");
    }

    // --- Private helpers ---

    private WebElement findAntDPaginationButton(String direction) {
        WebElement paginationContainer = findPaginationContainer();
        if (paginationContainer == null) return null;

        String selector = "next".equals(direction)
                ? ".ant-pagination-next"
                : ".ant-pagination-prev";
        List<WebElement> btns = paginationContainer.findElements(By.cssSelector(selector));
        return btns.isEmpty() ? null : btns.get(0);
    }

    private WebElement findPaginationContainer() {
        // Pagination may be inside or outside the table wrapper
        List<WebElement> pagination = tableRoot.findElements(By.cssSelector(".ant-pagination"));
        if (!pagination.isEmpty()) return pagination.get(0);
        // Check sibling/parent level
        try {
            return driver.findElement(By.cssSelector(".ant-pagination"));
        } catch (Exception e) {
            return null;
        }
    }

    private void waitForTableRefresh(WebDriver driver) {
        try {
            // Wait for spinner to disappear
            new WebDriverWait(driver, Duration.ofSeconds(1)).until(d -> {
                List<WebElement> spinners = tableRoot.findElements(By.cssSelector(".ant-spin-spinning"));
                return spinners.isEmpty() || spinners.stream().noneMatch(s -> {
                    try { return s.isDisplayed(); } catch (Exception e) { return false; }
                });
            });
        } catch (Exception ignored) {}
        try {
            new WebDriverWait(driver, TIMEOUT).until(d -> !getDataRows(d).isEmpty());
        } catch (Exception e) {
            logger.debug("AntD table refresh wait completed");
        }
        invalidateColumnMap();
    }

    // --- TableSelector ---

    @Override
    public void selectRow(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findAntDRowCheckbox(row);
        if (checkbox == null) throw new RuntimeException("No checkbox found in AntD row where " + column + "='" + value + "'");
        toggleAntDCheckbox(driver, checkbox, true);
    }

    @Override
    public void deselectRow(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        WebElement checkbox = findAntDRowCheckbox(row);
        if (checkbox == null) throw new RuntimeException("No checkbox found in AntD row where " + column + "='" + value + "'");
        toggleAntDCheckbox(driver, checkbox, false);
    }

    @Override
    public void selectAll(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> headerCheckboxes = tableRoot.findElements(By.cssSelector(
                ".ant-table-selection .ant-checkbox-input, thead .ant-checkbox-input"));
        if (headerCheckboxes.isEmpty()) throw new RuntimeException("No header checkbox found in AntD table");
        toggleAntDCheckbox(driver, headerCheckboxes.get(0), true);
    }

    @Override
    public void deselectAll(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> headerCheckboxes = tableRoot.findElements(By.cssSelector(
                ".ant-table-selection .ant-checkbox-input, thead .ant-checkbox-input"));
        if (headerCheckboxes.isEmpty()) throw new RuntimeException("No header checkbox found in AntD table");
        toggleAntDCheckbox(driver, headerCheckboxes.get(0), false);
    }

    @Override
    public int getSelectedCount(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> checkedWrappers = tableRoot.findElements(By.cssSelector(
                "tbody .ant-checkbox-checked"));
        return checkedWrappers.size();
    }

    @Override
    public boolean isRowSelected(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        // AntD adds ant-table-row-selected class
        String rowClass = row.getAttribute("class");
        if (rowClass != null && rowClass.contains("ant-table-row-selected")) return true;
        WebElement checkbox = findAntDRowCheckbox(row);
        if (checkbox == null) return false;
        // Check wrapper for ant-checkbox-checked
        try {
            WebElement wrapper = checkbox.findElement(By.xpath("ancestor::span[contains(@class,'ant-checkbox')]"));
            String cls = wrapper.getAttribute("class");
            return cls != null && cls.contains("ant-checkbox-checked");
        } catch (Exception e) {
            return isCheckboxChecked(checkbox);
        }
    }

    private WebElement findAntDRowCheckbox(WebElement row) {
        List<WebElement> checkboxes = row.findElements(By.cssSelector(
                "td.ant-table-selection-column .ant-checkbox-input, .ant-checkbox-input"));
        return checkboxes.isEmpty() ? null : checkboxes.get(0);
    }

    private void toggleAntDCheckbox(WebDriver driver, WebElement checkbox, boolean targetState) {
        boolean current = isAntDCheckboxChecked(checkbox);
        if (current != targetState) {
            HandlerUtils.clickSafe(driver, checkbox);
        }
    }

    private boolean isAntDCheckboxChecked(WebElement checkbox) {
        try {
            WebElement wrapper = checkbox.findElement(By.xpath("ancestor::span[contains(@class,'ant-checkbox')]"));
            String cls = wrapper.getAttribute("class");
            return cls != null && cls.contains("ant-checkbox-checked");
        } catch (Exception e) {
            return isCheckboxChecked(checkbox);
        }
    }

    // --- TableStateAssert ---

    @Override
    public boolean isLoading(WebDriver driver) {
        ensureTableRoot(driver);
        List<WebElement> spinners = tableRoot.findElements(By.cssSelector(
                ".ant-spin-spinning, .ant-table-loading"));
        for (WebElement spinner : spinners) {
            try { if (spinner.isDisplayed()) return true; } catch (Exception ignored) {}
        }
        return false;
    }

    @Override
    public boolean isEmpty(WebDriver driver) {
        if (isLoading(driver)) return false;
        ensureTableRoot(driver);
        List<WebElement> placeholders = tableRoot.findElements(By.cssSelector(
                ".ant-table-placeholder, .ant-empty"));
        for (WebElement ph : placeholders) {
            try { if (ph.isDisplayed()) return true; } catch (Exception ignored) {}
        }
        return getDataRows(driver).isEmpty();
    }

    @Override
    public String getEmptyStateMessage(WebDriver driver) {
        ensureTableRoot(driver);
        String[] selectors = {
                ".ant-empty-description",
                ".ant-table-placeholder .ant-empty",
                ".ant-table-placeholder"
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

    // --- TableExpander ---

    @Override
    public void expandRow(WebDriver driver, String column, String value) {
        if (isRowExpanded(driver, column, value)) return;
        WebElement row = findRow(driver, column, value);
        List<WebElement> icons = row.findElements(By.cssSelector(".ant-table-row-expand-icon"));
        if (icons.isEmpty()) icons = row.findElements(By.cssSelector("button[class*='expand']"));
        if (icons.isEmpty()) throw new RuntimeException("No expand icon in AntD row where " + column + "='" + value + "'");
        HandlerUtils.clickSafe(driver, icons.get(0));
        waitForExpandedContent(driver, row);
    }

    @Override
    public void collapseRow(WebDriver driver, String column, String value) {
        if (!isRowExpanded(driver, column, value)) return;
        WebElement row = findRow(driver, column, value);
        List<WebElement> icons = row.findElements(By.cssSelector(".ant-table-row-expand-icon"));
        if (icons.isEmpty()) icons = row.findElements(By.cssSelector("button[class*='expand']"));
        if (icons.isEmpty()) throw new RuntimeException("No expand icon in AntD row where " + column + "='" + value + "'");
        HandlerUtils.clickSafe(driver, icons.get(0));
        waitForCollapsed(driver, row);
    }

    @Override
    public boolean isRowExpanded(WebDriver driver, String column, String value) {
        WebElement row = findRow(driver, column, value);
        List<WebElement> expandedIcons = row.findElements(By.cssSelector(
                ".ant-table-row-expand-icon-expanded"));
        if (!expandedIcons.isEmpty()) return true;
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
        if (cls != null && cls.contains("editable")) return true;
        String dataEditable = cell.getAttribute("data-editable");
        return "true".equals(dataEditable);
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
