package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.List;

public interface FormValidationAssert {

    boolean hasError(WebDriver driver, WebElement fieldElement);

    String getErrorMessage(WebDriver driver, WebElement fieldElement);

    List<String> getAllErrors(WebDriver driver, WebElement formOrContainer);

    boolean isFieldInvalid(WebDriver driver, WebElement fieldElement);
}
