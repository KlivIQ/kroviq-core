package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.List;

public interface AutoCompleteHandler {

    void searchAndSelect(WebDriver driver, WebElement input, String searchText, String optionToSelect);

    List<String> getSuggestions(WebDriver driver, WebElement input, String searchText);
}
