package kroviq.wrapper.desktop.table;

import kroviq.utils.LoadProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GenericDesktopTable implements DesktopTable {

    private static final Logger logger = LogManager.getLogger(GenericDesktopTable.class);

    private final WebElement gridRoot;
    private final RemoteWebDriver driver;
    private Map<String, Integer> columnMap;

    public GenericDesktopTable(WebElement gridRoot, RemoteWebDriver driver) {
        this.gridRoot = gridRoot;
        this.driver = driver;
    }

    @Override
    public DesktopRow findRow(String columnName, String value) {
        logger.info("[DesktopTable] Searching row where {}='{}'", columnName, value);
        WebElement row = scrollAndFind(columnName, value);
        if (row == null) {
            throw new RuntimeException("Desktop table row not found where " + columnName + "='" + value + "'");
        }
        logger.info("[DesktopTable] Row found where {}='{}'", columnName, value);
        return new WinAppDriverRow(row);
    }

    @Override
    public DesktopRow findRowByCriteria(Map<String, String> criteria) {
        logger.info("[DesktopTable] Searching row by criteria: {}", criteria);
        WebElement row = scrollAndFindByCriteria(criteria);
        if (row == null) {
            throw new RuntimeException("Desktop table row not found matching criteria: " + criteria);
        }
        logger.info("[DesktopTable] Row found matching criteria");
        return new WinAppDriverRow(row);
    }

    @Override
    public boolean rowExists(String columnName, String value) {
        try {
            WebElement row = scrollAndFind(columnName, value);
            return row != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getCellValue(DesktopRow row, String columnName) {
        WebElement rowElement = unwrap(row);
        Map<String, Integer> columns = getColumnMap();
        Integer colIndex = columns.get(columnName);
        if (colIndex == null) {
            throw new RuntimeException("Column '" + columnName + "' not found. Available: " + columns.keySet());
        }
        List<WebElement> cells = getCells(rowElement);
        if (colIndex >= cells.size()) {
            throw new RuntimeException("Column index " + colIndex + " out of bounds (row has " + cells.size() + " cells)");
        }
        return extractCellText(cells.get(colIndex));
    }

    @Override
    public int getRowCount() {
        List<WebElement> rows = getVisibleRows();
        return rows.size();
    }

    // --- Internal scroll-to-find ---

    private WebElement scrollAndFind(String columnName, String value) {
        int maxAttempts = getMaxScrollAttempts();
        int previousRowHash = 0;

        for (int attempt = 0; attempt <= maxAttempts; attempt++) {
            List<WebElement> rows = getVisibleRows();
            for (WebElement row : rows) {
                String cellText = getCellTextByColumn(row, columnName);
                if (value.equalsIgnoreCase(cellText)) {
                    return row;
                }
            }

            int currentHash = computeRowHash(rows);
            if (attempt > 0 && currentHash == previousRowHash) {
                logger.debug("[DesktopTable] No new rows after scroll, stopping search");
                break;
            }
            previousRowHash = currentHash;

            if (attempt < maxAttempts) {
                scrollDown();
            }
        }
        return null;
    }

    private WebElement scrollAndFindByCriteria(Map<String, String> criteria) {
        int maxAttempts = getMaxScrollAttempts();
        int previousRowHash = 0;

        for (int attempt = 0; attempt <= maxAttempts; attempt++) {
            List<WebElement> rows = getVisibleRows();
            for (WebElement row : rows) {
                if (matchesCriteria(row, criteria)) {
                    return row;
                }
            }

            int currentHash = computeRowHash(rows);
            if (attempt > 0 && currentHash == previousRowHash) {
                break;
            }
            previousRowHash = currentHash;

            if (attempt < maxAttempts) {
                scrollDown();
            }
        }
        return null;
    }

    private boolean matchesCriteria(WebElement row, Map<String, String> criteria) {
        for (Map.Entry<String, String> entry : criteria.entrySet()) {
            String cellText = getCellTextByColumn(row, entry.getKey());
            if (!entry.getValue().equalsIgnoreCase(cellText)) {
                return false;
            }
        }
        return true;
    }

    // --- Column mapping ---

    private Map<String, Integer> getColumnMap() {
        if (columnMap == null) {
            columnMap = buildColumnMap();
        }
        return columnMap;
    }

    private Map<String, Integer> buildColumnMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        List<WebElement> headers = getHeaderCells();
        for (int i = 0; i < headers.size(); i++) {
            String text = extractCellText(headers.get(i));
            if (text != null && !text.isEmpty()) {
                map.put(text, i);
            }
        }
        logger.debug("[DesktopTable] Column map built: {}", map);
        return map;
    }

    // --- Element discovery ---

    private List<WebElement> getVisibleRows() {
        // UIA DataGrid: DataItem children represent rows
        // UIA List: ListItem children represent rows
        List<WebElement> rows = gridRoot.findElements(By.xpath(".//*[@LocalizedControlType='data item']"));
        if (rows.isEmpty()) {
            rows = gridRoot.findElements(By.xpath(".//*[@LocalizedControlType='list item']"));
        }
        if (rows.isEmpty()) {
            // Fallback: direct children with row-like control type
            rows = gridRoot.findElements(By.xpath(".//child::*[@ControlType='DataItem' or @ControlType='ListItem']"));
        }
        if (rows.isEmpty()) {
            // WinAppDriver fallback: find by class name patterns
            rows = gridRoot.findElements(By.className("DataGridRow"));
            if (rows.isEmpty()) {
                rows = gridRoot.findElements(By.className("ListViewItem"));
            }
        }
        return rows;
    }

    private List<WebElement> getHeaderCells() {
        // Try header row first
        List<WebElement> headers = gridRoot.findElements(By.xpath(".//*[@LocalizedControlType='column header']"));
        if (headers.isEmpty()) {
            headers = gridRoot.findElements(By.xpath(".//*[@LocalizedControlType='header item']"));
        }
        if (headers.isEmpty()) {
            headers = gridRoot.findElements(By.className("DataGridColumnHeader"));
        }
        return headers;
    }

    private List<WebElement> getCells(WebElement row) {
        List<WebElement> cells = row.findElements(By.xpath(".//*[@LocalizedControlType='text']"));
        if (cells.isEmpty()) {
            cells = row.findElements(By.xpath(".//child::*"));
        }
        return cells;
    }

    private String getCellTextByColumn(WebElement row, String columnName) {
        Map<String, Integer> columns = getColumnMap();
        Integer colIndex = columns.get(columnName);
        if (colIndex == null) return "";
        List<WebElement> cells = getCells(row);
        if (colIndex >= cells.size()) return "";
        return extractCellText(cells.get(colIndex));
    }

    // --- Text extraction ---

    private String extractCellText(WebElement cell) {
        // Priority 1: Value.Value pattern property
        String value = cell.getAttribute("Value.Value");
        if (value != null && !value.isEmpty()) return value.trim();

        // Priority 2: Name property
        String name = cell.getAttribute("Name");
        if (name != null && !name.isEmpty()) return name.trim();

        // Priority 3: getText()
        String text = cell.getText();
        return text != null ? text.trim() : "";
    }

    // --- Scrolling ---

    private void scrollDown() {
        gridRoot.sendKeys(Keys.PAGE_DOWN);
        sleep(getScrollDelayMs());
    }

    private int computeRowHash(List<WebElement> rows) {
        if (rows.isEmpty()) return 0;
        // Use first and last row text as identity
        StringBuilder sb = new StringBuilder();
        sb.append(rows.get(0).getAttribute("Name"));
        if (rows.size() > 1) {
            sb.append(rows.get(rows.size() - 1).getAttribute("Name"));
        }
        return sb.toString().hashCode();
    }

    // --- Helpers ---

    private WebElement unwrap(DesktopRow row) {
        if (!(row instanceof WinAppDriverRow wrapper)) {
            throw new IllegalArgumentException("DesktopRow is not from this engine");
        }
        return wrapper.element();
    }

    private int getMaxScrollAttempts() {
        return LoadProperties.getInt("desktop.table.maxScrollAttempts", 20);
    }

    private int getScrollDelayMs() {
        return LoadProperties.getInt("desktop.table.scrollDelayMs", 300);
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // --- Internal DesktopRow implementation ---

    record WinAppDriverRow(WebElement element) implements DesktopRow {
    }
}
