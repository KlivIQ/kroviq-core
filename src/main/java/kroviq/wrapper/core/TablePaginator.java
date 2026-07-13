package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;

public interface TablePaginator {

    void nextPage(WebDriver driver);

    void previousPage(WebDriver driver);

    void goToPage(WebDriver driver, int pageNumber);

    boolean hasNextPage(WebDriver driver);
}
