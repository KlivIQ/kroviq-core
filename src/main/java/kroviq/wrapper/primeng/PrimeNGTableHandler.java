package kroviq.wrapper.primeng;

import kroviq.wrapper.core.GridHandler;
import kroviq.wrapper.core.HandlerUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;

public class PrimeNGTableHandler implements GridHandler {

    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("primeng");
    private static final int MAX_SCROLL_ATTEMPTS = HandlerUtils.getMaxScrollAttempts("primeng");

    private static final String HEADER_ROW = "thead tr, .p-datatable-thead tr";
    private static final String HEADER_CELL = "th";
    private static final String DATA_ROW = "tbody tr, .p-datatable-tbody tr";
    private static final String DATA_CELL = "td";
    private static final String VIRTUAL_SCROLLER = ".p-datatable-virtual-scroller, cdk-virtual-scroll-viewport, .p-scroller";
    private static final String PAGINATOR_NEXT = ".p-paginator-next";
    private static final String PAGINATOR_PAGE = ".p-paginator-page";

    private final WebElement tableRoot;

    public PrimeNGTableHandler(WebElement tableRoot) {
        this.tableRoot = tableRoot;
    }

    @Override
    public int getColumnIndex(WebDriver driver, String columnHeader) {
        List<WebElement> headerRows = tableRoot.findElements(By.cssSelector(HEADER_ROW));
        for (WebElement row : headerRows) {
            List<WebElement> headers = row.findElements(By.cssSelector(HEADER_CELL));
            for (int i = 0; i < headers.size(); i++) {
                if (columnHeader.equals(headers.get(i).getText().trim())) {
                    return i;
                }
            }
        }
        throw new org.openqa.selenium.NoSuchElementException(
                "PrimeNG table column '" + columnHeader + "' not found");
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
        List<WebElement> rows = getDataRows();

        if (rowIndex < rows.size()) return rows.get(rowIndex);

        // Virtual scroll support if naturally present
        if (hasVirtualScroll()) {
            return scrollToRow(driver, rowIndex);
        }

        throw new org.openqa.selenium.NoSuchElementException(
                "PrimeNG table row at index " + rowIndex + " not found");
    }

    public void goToPage(WebDriver driver, int pageNumber) {
        List<WebElement> pageButtons = tableRoot.findElements(By.cssSelector(PAGINATOR_PAGE));
        for (WebElement btn : pageButtons) {
            if (String.valueOf(pageNumber).equals(btn.getText().trim())) {
                btn.click();
                new WebDriverWait(driver, TIMEOUT).until(d -> {
                    List<WebElement> rows = getDataRows();
                    return !rows.isEmpty();
                });
                return;
            }
        }
        throw new org.openqa.selenium.NoSuchElementException(
                "PrimeNG paginator page " + pageNumber + " not found");
    }

    public void nextPage(WebDriver driver) {
        List<WebElement> nextButtons = tableRoot.findElements(By.cssSelector(PAGINATOR_NEXT));
        if (nextButtons.isEmpty()) {
            // Paginator may be outside table root — check document level
            nextButtons = driver.findElements(By.cssSelector(PAGINATOR_NEXT));
        }
        if (nextButtons.isEmpty()) {
            throw new org.openqa.selenium.NoSuchElementException("PrimeNG paginator next button not found");
        }
        WebElement nextBtn = nextButtons.get(0);
        if (!nextBtn.isEnabled()) {
            throw new IllegalStateException("PrimeNG paginator: already on last page");
        }
        nextBtn.click();
        new WebDriverWait(driver, TIMEOUT).until(d -> {
            List<WebElement> rows = getDataRows();
            return !rows.isEmpty();
        });
    }

    private WebElement locateCell(WebDriver driver, String columnHeader, int rowIndex) {
        int colIndex = getColumnIndex(driver, columnHeader);
        WebElement row = getRow(driver, rowIndex);
        List<WebElement> cells = row.findElements(By.cssSelector(DATA_CELL));
        if (colIndex < cells.size()) return cells.get(colIndex);

        throw new org.openqa.selenium.NoSuchElementException(
                "PrimeNG table cell not found at column '" + columnHeader + "', row " + rowIndex);
    }

    private List<WebElement> getDataRows() {
        return tableRoot.findElements(By.cssSelector(DATA_ROW));
    }

    private boolean hasVirtualScroll() {
        try {
            return !tableRoot.findElements(By.cssSelector(VIRTUAL_SCROLLER)).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private WebElement scrollToRow(WebDriver driver, int rowIndex) {
        WebElement viewport = tableRoot.findElement(By.cssSelector(VIRTUAL_SCROLLER));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int scrollStep = viewport.getSize().getHeight();

        for (int attempt = 0; attempt < MAX_SCROLL_ATTEMPTS; attempt++) {
            js.executeScript("arguments[0].scrollTop += arguments[1];", viewport, scrollStep);

            try {
                new WebDriverWait(driver, Duration.ofSeconds(1)).until(d -> {
                    List<WebElement> rows = getDataRows();
                    return rows.size() > rowIndex;
                });
                List<WebElement> rows = getDataRows();
                if (rowIndex < rows.size()) return rows.get(rowIndex);
            } catch (Exception e) {
                // Row not yet in viewport, continue scrolling
            }
        }

        throw new org.openqa.selenium.NoSuchElementException(
                "PrimeNG table row at index " + rowIndex + " not found after " + MAX_SCROLL_ATTEMPTS + " scroll attempts");
    }
}
