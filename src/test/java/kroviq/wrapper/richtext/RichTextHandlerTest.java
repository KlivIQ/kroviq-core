package kroviq.wrapper.richtext;

import kroviq.wrapper.core.RichTextHandler;
import kroviq.wrapper.factory.ComponentHandlerFactory;
import kroviq.wrapper.factory.GenericRichTextHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RichTextHandlerTest {

    private WebDriver driver;
    private WebElement element;
    private GenericRichTextHandler handler;

    @BeforeEach
    void setUp() {
        driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        element = mock(WebElement.class);
        handler = new GenericRichTextHandler();
    }

    @Test
    void implementsInterface() {
        assertInstanceOf(RichTextHandler.class, handler);
    }

    @Test
    void factory_returnsGenericHandler() {
        when(element.getAttribute("class")).thenReturn("editor-wrapper");
        when(element.getTagName()).thenReturn("div");
        when(element.getAttribute("data-testid")).thenReturn(null);
        assertInstanceOf(GenericRichTextHandler.class, ComponentHandlerFactory.getRichTextHandler(element, driver));
    }

    @Test
    void getContent_contenteditableElement_returnsInnerHTML() {
        when(element.getAttribute("contenteditable")).thenReturn("true");
        when(((JavascriptExecutor) driver).executeScript(contains("innerHTML"), eq(element)))
                .thenReturn("<p>Hello World</p>");
        assertEquals("<p>Hello World</p>", handler.getContent(driver, element));
    }

    @Test
    void getPlainText_contenteditableElement_returnsText() {
        when(element.getAttribute("contenteditable")).thenReturn("true");
        when(((JavascriptExecutor) driver).executeScript(contains("innerText"), eq(element)))
                .thenReturn("Hello World");
        assertEquals("Hello World", handler.getPlainText(driver, element));
    }

    @Test
    void setContent_contenteditableElement_setsInnerHTML() {
        when(element.getAttribute("contenteditable")).thenReturn("true");
        when(element.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(((JavascriptExecutor) driver).executeScript(anyString(), any(), any())).thenReturn(false);

        handler.setContent(driver, element, "<p>New Content</p>");

        verify((JavascriptExecutor) driver, atLeastOnce()).executeScript(anyString(), any(), any());
    }

    @Test
    void clearContent_setsEmptyInnerHTML() {
        when(element.getAttribute("contenteditable")).thenReturn("true");
        when(element.findElements(any(By.class))).thenReturn(Collections.emptyList());

        handler.clearContent(driver, element);

        verify((JavascriptExecutor) driver).executeScript(contains("innerHTML = ''"), eq(element));
    }

    @Test
    void resolveEditableArea_quillEditor_findsQlEditor() {
        WebElement qlEditor = mock(WebElement.class);
        when(element.getAttribute("contenteditable")).thenReturn(null);
        when(element.findElements(By.cssSelector(".ql-editor[contenteditable='true']")))
                .thenReturn(List.of(qlEditor));
        when(qlEditor.getAttribute("contenteditable")).thenReturn("true");
        when(((JavascriptExecutor) driver).executeScript(contains("innerHTML"), eq(qlEditor)))
                .thenReturn("<p>Quill content</p>");

        assertEquals("<p>Quill content</p>", handler.getContent(driver, element));
    }
}
