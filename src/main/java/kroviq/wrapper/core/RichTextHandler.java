package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public interface RichTextHandler {

    void setContent(WebDriver driver, WebElement editorElement, String htmlContent);

    void setPlainText(WebDriver driver, WebElement editorElement, String text);

    void appendContent(WebDriver driver, WebElement editorElement, String htmlContent);

    void clearContent(WebDriver driver, WebElement editorElement);

    String getContent(WebDriver driver, WebElement editorElement);

    String getPlainText(WebDriver driver, WebElement editorElement);
}
