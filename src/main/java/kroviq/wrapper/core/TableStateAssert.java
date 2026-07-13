package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;

public interface TableStateAssert {

    boolean isLoading(WebDriver driver);

    boolean isEmpty(WebDriver driver);

    String getEmptyStateMessage(WebDriver driver);
}
