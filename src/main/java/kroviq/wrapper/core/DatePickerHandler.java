package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.time.LocalDate;

public interface DatePickerHandler {

    void selectDate(WebDriver driver, WebElement pickerElement, LocalDate date);

    void setDateValue(WebDriver driver, WebElement pickerElement, String dateString);

    String getCurrentValue(WebDriver driver, WebElement pickerElement);
}
