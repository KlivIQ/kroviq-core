package kroviq.wrapper.angularmaterial;

import kroviq.wrapper.core.GridHandler;
import kroviq.wrapper.core.HandlerUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;

public class AngularMaterialTableHandler implements GridHandler {

    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("angularmaterial");
    private static final int MAX_SCROLL_ATTEMPTS = HandlerUtils.getMaxScrollAttempts("angularmaterial");

    private static final String HEADER_ROW = "mat-header-row, tr.mat-mdc-header-row, tr[mat-header-row]";
    private static final String HEADER_CELL = "mat-header-cell, th.mat-mdc-header-cell, th[mat-header-cell]";
    private static final String DATA_ROW = "mat-row, tr.mat-mdc-row, tr[mat-row]";
    private static final String DATA_CELL = "mat-cell, td.mat-mdc-cell, td[mat-cell]";
    private static final String CDK_VIRTUAL_SCROLL = "cdk-virtual-scroll-viewport";

    private final WebElement tableRoot;

    public AngularMaterialTableHandler(WebElement tableRoot) {
        this.tableRoot = tableRoot;
    }

    @Override
    public int getColumnIndex(WebDriver driver, String columnHeader) {
        List<WebElement> headers = tableRoot.findElements(By.cssSelector(HEADER_CELL));
        for (int i = 0; i < headers.size(); i++) {
            if (columnHeader.equals(headers.get(i).getText().trim())) {
                return i;
            }
        }
        throw new org.openqa.selenium.NoSuchElementException(
                "Angular Material table column '" + columnHeader + "' not found");
    }

    @Override
    public String getCellValue(WebDriver driver, String columnHeader, int rowIndex) {
        WebElement cell = locateCell(driver, columnHeader, rowIndex);
        return cell.getText().trim();
    }

    @Override
    public void clickCell(WebDriver driver, String columnHeader, int rowIndex) {
        WebElement cell = locateCell(driver, columnHeader, rowIndex);
        cell.click();
    }

    @Override
    public void selectRow(WebDriver driver, int rowIndex) {
        WebElement row = getRow(driver, rowIndex);
        row.click();
    }

    @Override
    public WebElement getRow(WebDriver driver, int rowIndex) {
        List<WebElement> rows = tableRoot.findElements(By.cssSelector(DATA_ROW));

        // Standard mat-table: rows are all rendered
        if (rowIndex < rows.size()) return rows.get(rowIndex);

        // CDK virtual scroll enhancement: scroll to find row
        if (hasCdkVirtualScroll()) {
            return scrollToRow(driver, rowIndex);
        }

        throw new org.openqa.selenium.NoSuchElementException(
                "Angular Material table row at index " + rowIndex + " not found");
    }

    private WebElement locateCell(WebDriver driver, String columnHeader, int rowIndex) {
        int colIndex = getColumnIndex(driver, columnHeader);
        WebElement row = getRow(driver, rowIndex);
        List<WebElement> cells = row.findElements(By.cssSelector(DATA_CELL));
        if (colIndex < cells.size()) return cells.get(colIndex);

        throw new org.openqa.selenium.NoSuchElementException(
                "Angular Material table cell not found at column '" + columnHeader + "', row " + rowIndex);
    }

    private boolean hasCdkVirtualScroll() {
        try {
            return !tableRoot.findElements(By.tagName(CDK_VIRTUAL_SCROLL)).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private WebElement scrollToRow(WebDriver driver, int rowIndex) {
        WebElement viewport = tableRoot.findElement(By.tagName(CDK_VIRTUAL_SCROLL));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int scrollStep = viewport.getSize().getHeight();

        for (int attempt = 0; attempt < MAX_SCROLL_ATTEMPTS; attempt++) {
            js.executeScript("arguments[0].scrollTop += arguments[1];", viewport, scrollStep);

            WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
            try {
                wait.until(d -> {
                    List<WebElement> rows = tableRoot.findElements(By.cssSelector(DATA_ROW));
                    return rows.size() > rowIndex;
                });
                List<WebElement> rows = tableRoot.findElements(By.cssSelector(DATA_ROW));
                if (rowIndex < rows.size()) return rows.get(rowIndex);
            } catch (Exception e) {
                // Row not yet in viewport, continue scrolling
            }
        }

        throw new org.openqa.selenium.NoSuchElementException(
                "Angular Material table row at index " + rowIndex + " not found after " + MAX_SCROLL_ATTEMPTS + " scroll attempts");
    }
}
