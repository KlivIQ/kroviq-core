package kroviq.wrapper.antd;

import kroviq.utils.DatePickerUtils;
import kroviq.wrapper.AntDDatePicker;
import kroviq.wrapper.core.DatePickerHandler;
import kroviq.wrapper.core.HandlerUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;

public class AntDDatePickerHandler implements DatePickerHandler {

    private final WebDriver driver;

    public AntDDatePickerHandler(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void selectDate(WebDriver driver, WebElement pickerElement, LocalDate date) {
        int timeout = HandlerUtils.getTimeout("antd");
        AntDDatePicker picker = new AntDDatePicker(driver, timeout);
        By locator = toByLocator(pickerElement);
        picker.selectDate(locator, date);
    }

    @Override
    public void setDateValue(WebDriver driver, WebElement pickerElement, String dateString) {
        LocalDate date = DatePickerUtils.parseDate(dateString);
        selectDate(driver, pickerElement, date);
    }

    @Override
    public String getCurrentValue(WebDriver driver, WebElement pickerElement) {
        WebElement input = resolveInput(pickerElement);
        String value = input.getAttribute("value");
        return value != null ? value.trim() : "";
    }

    private WebElement resolveInput(WebElement element) {
        try {
            return element.findElement(By.cssSelector(".ant-picker-input > input, input"));
        } catch (Exception e) {
            return element;
        }
    }

    private By toByLocator(WebElement element) {
        String id = element.getAttribute("id");
        if (id != null && !id.isEmpty()) {
            return By.id(id);
        }
        String className = element.getAttribute("class");
        if (className != null && className.contains("ant-picker")) {
            return By.xpath("//*[@class='" + className + "']");
        }
        return By.xpath(".//*[contains(@class,'ant-picker')]");
    }
}
