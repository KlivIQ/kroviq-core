package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.List;

public interface DropdownHandler {

    void select(WebDriver driver, WebElement trigger, String visibleText);

    List<String> getOptions(WebDriver driver, WebElement trigger);
}
