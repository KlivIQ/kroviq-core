package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public interface TableExpander {

    void expandRow(WebDriver driver, String column, String value);

    void collapseRow(WebDriver driver, String column, String value);

    boolean isRowExpanded(WebDriver driver, String column, String value);

    WebElement getExpandedContent(WebDriver driver, String column, String value);
}
