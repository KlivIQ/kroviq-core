package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;

public interface TableFilter {

    void filterColumnContains(WebDriver driver, String columnName, String value);

    void clearColumnFilter(WebDriver driver, String columnName);

    void clearAllFilters(WebDriver driver);

    void globalSearch(WebDriver driver, String searchText);

    void clearGlobalSearch(WebDriver driver);
}
