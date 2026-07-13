package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;

public interface TableSelector {

    void selectRow(WebDriver driver, String column, String value);

    void deselectRow(WebDriver driver, String column, String value);

    void selectAll(WebDriver driver);

    void deselectAll(WebDriver driver);

    int getSelectedCount(WebDriver driver);

    boolean isRowSelected(WebDriver driver, String column, String value);
}
