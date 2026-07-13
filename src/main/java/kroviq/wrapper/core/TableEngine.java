package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.Map;

public interface TableEngine extends GridHandler {

    WebElement findRow(WebDriver driver, String columnHeader, String value);

    WebElement findRowByCriteria(WebDriver driver, Map<String, String> criteria);

    boolean rowExists(WebDriver driver, String columnHeader, String value);

    String getCellValue(WebDriver driver, WebElement row, String columnHeader);

    int getRowCount(WebDriver driver);

    void performRowAction(WebDriver driver, WebElement row, String actionName);
}
