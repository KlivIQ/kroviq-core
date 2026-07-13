package kroviq.wrapper.aggrid;

import kroviq.wrapper.core.GridHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class AgGridHandler implements GridHandler {

    private final WebElement gridRoot;

    public AgGridHandler(WebElement gridRoot) {
        this.gridRoot = gridRoot;
    }

    @Override
    public int getColumnIndex(WebDriver driver, String columnHeader) {
        List<WebElement> headers = gridRoot.findElements(By.cssSelector(AgGridLocators.HEADER_CELL));
        for (int i = 0; i < headers.size(); i++) {
            WebElement textEl = headers.get(i).findElement(By.cssSelector(AgGridLocators.HEADER_CELL_TEXT));
            if (columnHeader.equals(textEl.getText().trim())) {
                return i;
            }
        }
        throw new org.openqa.selenium.NoSuchElementException(
                "AG Grid column '" + columnHeader + "' not found");
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
        WebElement firstCell = row.findElement(By.cssSelector(AgGridLocators.CELL));
        firstCell.click();
    }

    @Override
    public WebElement getRow(WebDriver driver, int rowIndex) {
        return AgGridScrollHelper.scrollToRow(driver, gridRoot, rowIndex);
    }

    private WebElement locateCell(WebDriver driver, String columnHeader, int rowIndex) {
        int colIndex = getColumnIndex(driver, columnHeader);
        WebElement row = getRow(driver, rowIndex);
        String cellSelector = AgGridLocators.cellByColIndex(colIndex);
        List<WebElement> cells = row.findElements(By.cssSelector(cellSelector));
        if (!cells.isEmpty()) return cells.get(0);

        // Fallback: get cell by positional index
        List<WebElement> allCells = row.findElements(By.cssSelector(AgGridLocators.CELL));
        if (colIndex < allCells.size()) return allCells.get(colIndex);

        throw new org.openqa.selenium.NoSuchElementException(
                "AG Grid cell not found at column '" + columnHeader + "', row " + rowIndex);
    }
}
