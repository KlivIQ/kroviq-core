package kroviq.wrapper.factory;

import kroviq.wrapper.core.GridHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class GenericGridHandler implements GridHandler {

    private final WebElement gridRoot;

    public GenericGridHandler(WebElement gridRoot) {
        this.gridRoot = gridRoot;
    }

    @Override
    public int getColumnIndex(WebDriver driver, String columnHeader) {
        List<WebElement> headers = gridRoot.findElements(By.cssSelector("th, [role='columnheader']"));
        for (int i = 0; i < headers.size(); i++) {
            if (columnHeader.equals(headers.get(i).getText().trim())) return i;
        }
        throw new org.openqa.selenium.NoSuchElementException(
                "Column '" + columnHeader + "' not found in grid");
    }

    @Override
    public String getCellValue(WebDriver driver, String columnHeader, int rowIndex) {
        WebElement cell = locateCell(driver, columnHeader, rowIndex);
        return cell.getText().trim();
    }

    @Override
    public void clickCell(WebDriver driver, String columnHeader, int rowIndex) {
        locateCell(driver, columnHeader, rowIndex).click();
    }

    @Override
    public void selectRow(WebDriver driver, int rowIndex) {
        getRow(driver, rowIndex).click();
    }

    @Override
    public WebElement getRow(WebDriver driver, int rowIndex) {
        // Find data rows: rows that contain <td> elements (skip header rows with only <th>)
        List<WebElement> allRows = gridRoot.findElements(By.cssSelector("tr"));
        List<WebElement> dataRows = allRows.stream()
                .filter(row -> !row.findElements(By.cssSelector("td")).isEmpty())
                .toList();
        if (dataRows.isEmpty()) {
            dataRows = gridRoot.findElements(By.cssSelector("[role='row']:not(:has([role='columnheader']))"));
        }
        if (rowIndex < dataRows.size()) return dataRows.get(rowIndex);
        throw new org.openqa.selenium.NoSuchElementException(
                "Row at index " + rowIndex + " not found in grid");
    }

    private WebElement locateCell(WebDriver driver, String columnHeader, int rowIndex) {
        int colIndex = getColumnIndex(driver, columnHeader);
        WebElement row = getRow(driver, rowIndex);
        List<WebElement> cells = row.findElements(By.cssSelector("td, [role='gridcell']"));
        if (colIndex < cells.size()) return cells.get(colIndex);
        throw new org.openqa.selenium.NoSuchElementException(
                "Cell not found at column '" + columnHeader + "', row " + rowIndex);
    }
}
