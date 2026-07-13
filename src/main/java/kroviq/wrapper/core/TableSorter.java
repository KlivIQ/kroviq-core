package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;

public interface TableSorter {

    enum SortOrder { ASC, DESC, NONE }

    void sortByColumn(WebDriver driver, String columnName, SortOrder order);

    SortOrder getCurrentSort(WebDriver driver, String columnName);

    boolean verifySortOrder(WebDriver driver, String columnName, SortOrder expected);
}
