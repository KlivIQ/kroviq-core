package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.time.LocalDateTime;

public interface DateTimeHandler {

    void selectDateTime(WebDriver driver, WebElement pickerElement, LocalDateTime dateTime);

    void setDateTimeValue(WebDriver driver, WebElement pickerElement, String dateTimeString);

    String getCurrentValue(WebDriver driver, WebElement pickerElement);
}
