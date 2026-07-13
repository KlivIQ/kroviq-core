package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.List;

public interface MultiSelectHandler {

    void selectMultiple(WebDriver driver, WebElement trigger, List<String> values);

    void deselectValue(WebDriver driver, WebElement trigger, String value);

    void deselectAll(WebDriver driver, WebElement trigger);

    List<String> getSelectedValues(WebDriver driver, WebElement trigger);
}
