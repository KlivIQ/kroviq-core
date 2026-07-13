package kroviq.wrapper.core;

import org.openqa.selenium.WebElement;
import java.util.Collections;
import java.util.Map;

public class TableRowContext {

    private final WebElement tableRoot;
    private final String findColumn;
    private final String findValue;
    private final Map<String, String> criteria;
    private WebElement currentRow;

    private TableRowContext(WebElement tableRoot, String findColumn, String findValue,
                            Map<String, String> criteria, WebElement currentRow) {
        this.tableRoot = tableRoot;
        this.findColumn = findColumn;
        this.findValue = findValue;
        this.criteria = criteria != null ? Collections.unmodifiableMap(criteria) : null;
        this.currentRow = currentRow;
    }

    public static TableRowContext of(WebElement tableRoot, String findColumn, String findValue, WebElement row) {
        return new TableRowContext(tableRoot, findColumn, findValue, null, row);
    }

    public static TableRowContext ofCriteria(WebElement tableRoot, Map<String, String> criteria, WebElement row) {
        return new TableRowContext(tableRoot, null, null, criteria, row);
    }

    public WebElement getTableRoot() { return tableRoot; }
    public String getFindColumn() { return findColumn; }
    public String getFindValue() { return findValue; }
    public Map<String, String> getCriteria() { return criteria; }
    public WebElement getCurrentRow() { return currentRow; }
    public void setCurrentRow(WebElement row) { this.currentRow = row; }

    public boolean hasSingleColumnCriteria() { return findColumn != null && findValue != null; }
    public boolean hasMultiCriteria() { return criteria != null && !criteria.isEmpty(); }
}
