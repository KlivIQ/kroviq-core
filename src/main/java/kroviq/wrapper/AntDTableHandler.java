package kroviq.wrapper;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import kroviq.utils.ConstantsResolver;
import kroviq.utils.TestContext;

public class AntDTableHandler {
    private static final Logger logger = LogManager.getLogger(AntDTableHandler.class);
    private static final int MAX_SYNC_RETRIES = 2;
    private final WebDriver driver;
    private final WebDriverWait wait;

    public AntDTableHandler(WebDriver driver, int timeoutSec) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSec));
    }

    public WebElement findTable(String tableIdentifier) {
        WebElement table = null;
        // Try constant resolution first (e.g., Listing_Table -> XPath)
        try {
            String module = TestContext.getCurrentModule();
            String pageName = module + "Page";
            ConstantsResolver.ElementInfo info = ConstantsResolver.resolve(pageName, tableIdentifier);
            String xpath = info.getXpath();
            logger.debug("Resolved table '{}' to XPath: {}", tableIdentifier, xpath);
            table = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
        } catch (Exception resolveEx) {
            logger.debug("Constant resolution failed for '{}', falling back to id lookup", tableIdentifier);
        }
        // Fallback: search by HTML id attribute
        if (table == null) {
            try {
                table = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[contains(@class,'ant-table') and (contains(@id,'" + tableIdentifier + "') or @id='" + tableIdentifier + "')]")));
            } catch (Exception e) {
                throw new RuntimeException("Table not found: " + tableIdentifier);
            }
        }
        // Wait for at least 1 data row to be present before returning
        waitForRowsPresent(table);
        return table;
    }

    public Map<String, Integer> buildColumnMap(WebElement table) {
        Map<String, Integer> map = new LinkedHashMap<>();
        // Try thead within the table element first
        List<WebElement> headers = table.findElements(By.xpath(".//thead//th[not(contains(@class,'ant-table-cell-scrollbar'))]"));
        // AntD split-header: thead is in a sibling table under ant-table-header
        if (headers.isEmpty()) {
            headers = table.findElements(By.xpath(".//div[contains(@class,'ant-table-header')]//thead//th[not(contains(@class,'ant-table-cell-scrollbar'))]"));
        }
        for (int i = 0; i < headers.size(); i++) {
            String colName = headers.get(i).getText().trim();
            if (!colName.isEmpty()) {
                map.put(colName, i);
            }
        }
        logger.debug("Column map: {}", map);
        return map;
    }

    public WebElement findRowByColumnValue(WebElement table, String columnName, String value, Map<String, Integer> columnMap) {
        Integer colIndex = columnMap.get(columnName);
        if (colIndex == null) {
            throw new RuntimeException("Column '" + columnName + "' not found. Available: " + columnMap.keySet());
        }

        for (int attempt = 1; attempt <= MAX_SYNC_RETRIES; attempt++) {
            try {
                List<WebElement> rows = getDataRows(table);
                for (WebElement row : rows) {
                    List<WebElement> cells = row.findElements(By.xpath(".//td[contains(@class,'ant-table-cell')]"));
                    if (colIndex < cells.size()) {
                        String cellText = extractCellText(cells.get(colIndex));
                        if (cellText.equalsIgnoreCase(value.trim())) {
                            logger.debug("Found row with {}='{}', data-row-key={}", columnName, value, row.getAttribute("data-row-key"));
                            return row;
                        }
                    }
                }
                break; // rows iterated without stale — value simply not found
            } catch (StaleElementReferenceException e) {
                logger.debug("[Sync] Retrying action (attempt {}) for element findRowByColumnValue: StaleElementReferenceException", attempt);
                if (attempt == MAX_SYNC_RETRIES) throw new RuntimeException("Row search failed due to stale elements", e);
            }
        }
        throw new RuntimeException("Row not found with " + columnName + "='" + value + "'");
    }

    public String getCellValue(WebElement row, String columnName, Map<String, Integer> columnMap) {
        Integer colIndex = columnMap.get(columnName);
        if (colIndex == null) {
            throw new RuntimeException("Column '" + columnName + "' not found");
        }

        List<WebElement> cells = row.findElements(By.xpath(".//td[contains(@class,'ant-table-cell')]"));
        if (colIndex >= cells.size()) {
            throw new RuntimeException("Column index " + colIndex + " out of bounds");
        }

        return extractCellText(cells.get(colIndex));
    }

    public String getRowStatus(WebElement row) {
        String rowClass = row.getAttribute("class");
        if (rowClass.contains("row-status-")) {
            String[] parts = rowClass.split("row-status-");
            if (parts.length > 1) {
                String status = parts[1].split("\\s")[0];
                return status.substring(0, 1).toUpperCase() + status.substring(1);
            }
        }

        try {
            WebElement statusTag = row.findElement(By.xpath(".//span[contains(@class,'ant-tag')]"));
            return statusTag.getText().trim();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public void performRowAction(WebElement row, String action) {
        for (int attempt = 1; attempt <= MAX_SYNC_RETRIES; attempt++) {
            try {
                WebElement meatballBtn = row.findElement(By.xpath(".//button[contains(@id,'MeatBallMenu')]"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", meatballBtn);
                wait.until(ExpectedConditions.elementToBeClickable(meatballBtn)).click();
                logger.debug("Clicked meatball menu");

                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
                WebElement menuItem = shortWait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class,'ant-dropdown')]//span[normalize-space()='" + action + "']")));
                menuItem.click();
                logger.info("Performed action: {}", action);
                return;
            } catch (StaleElementReferenceException e) {
                logger.debug("[Sync] Retrying action (attempt {}) for element performRowAction: StaleElementReferenceException", attempt);
                if (attempt == MAX_SYNC_RETRIES) throw new RuntimeException("Failed to perform action '" + action + "': " + e.getMessage(), e);
            } catch (ElementClickInterceptedException e) {
                logger.debug("[Sync] Retrying action (attempt {}) for element performRowAction: ElementClickInterceptedException", attempt);
                if (attempt == MAX_SYNC_RETRIES) throw new RuntimeException("Failed to perform action '" + action + "': " + e.getMessage(), e);
            } catch (Exception e) {
                throw new RuntimeException("Failed to perform action '" + action + "': " + e.getMessage(), e);
            }
        }
    }

    public void validateRowData(WebElement row, Map<String, Integer> columnMap, Map<String, String> expectedData) {
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, String> entry : expectedData.entrySet()) {
            String colName = entry.getKey();
            String expected = entry.getValue();
            try {
                String actual = getCellValue(row, colName, columnMap);
                if (!expected.trim().equalsIgnoreCase(actual.trim())) {
                    errors.add(colName + ": expected '" + expected + "', found '" + actual + "'");
                }
            } catch (Exception e) {
                errors.add(colName + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new AssertionError("Row validation failed:\n  " + String.join("\n  ", errors));
        }
    }

    public WebElement getFirstRow(WebElement table) {
        List<WebElement> rows = getDataRows(table);
        if (rows.isEmpty()) {
            throw new RuntimeException("No rows found in table");
        }
        return rows.get(0);
    }

    private void waitForRowsPresent(WebElement table) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> !getDataRows(table).isEmpty());
        } catch (TimeoutException e) {
            logger.debug("No data rows found in table after 5s, proceeding with empty table");
        }
    }

    private List<WebElement> getDataRows(WebElement table) {
        List<WebElement> rows = table.findElements(By.xpath(".//tbody[@class='ant-table-tbody']/tr[not(contains(@class,'ant-table-measure-row')) and not(@aria-hidden='true')]"));
        if (rows.isEmpty()) {
            rows = table.findElements(By.xpath(".//div[contains(@class,'ant-table-body')]//tbody/tr[not(contains(@class,'ant-table-measure-row')) and not(@aria-hidden='true')]"));
        }
        return rows;
    }

    private String extractCellText(WebElement cell) {
        try {
            WebElement span = cell.findElement(By.xpath(".//span"));
            String text = span.getText().trim();
            if (!text.isEmpty()) return text;
        } catch (org.openqa.selenium.NoSuchElementException ignored) {}
        return cell.getText().trim();
    }
}
