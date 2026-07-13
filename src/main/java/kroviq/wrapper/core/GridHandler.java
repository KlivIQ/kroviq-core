package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public interface GridHandler {

    String getCellValue(WebDriver driver, String columnHeader, int rowIndex);

    void clickCell(WebDriver driver, String columnHeader, int rowIndex);

    void selectRow(WebDriver driver, int rowIndex);

    WebElement getRow(WebDriver driver, int rowIndex);

    int getColumnIndex(WebDriver driver, String columnHeader);
}
