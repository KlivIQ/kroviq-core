package kroviq.wrapper.cascader;

import kroviq.wrapper.core.CascaderHandler;
import kroviq.wrapper.factory.ComponentHandlerFactory;
import kroviq.wrapper.factory.GenericCascaderHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CascaderHandlerTest {

    private WebDriver driver;
    private WebElement element;
    private GenericCascaderHandler handler;

    @BeforeEach
    void setUp() {
        driver = mock(WebDriver.class);
        element = mock(WebElement.class);
        handler = new GenericCascaderHandler(driver);
    }

    @Test
    void implementsInterface() {
        assertInstanceOf(CascaderHandler.class, handler);
    }

    @Test
    void factory_returnsGenericHandler() {
        when(element.getAttribute("class")).thenReturn("ant-cascader");
        when(element.getTagName()).thenReturn("div");
        when(element.getAttribute("data-testid")).thenReturn(null);
        assertInstanceOf(GenericCascaderHandler.class, ComponentHandlerFactory.getCascaderHandler(element, driver));
    }

    @Test
    void getSelectedPath_antdLabel_returnsPath() {
        WebElement label = mock(WebElement.class);
        when(label.getText()).thenReturn("Asia / India / Chennai");
        when(element.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(element.findElements(By.cssSelector(
                ".ant-cascader-picker-label, " +
                ".p-cascadeselect-label, " +
                "input, " +
                "[class*='cascader-label'], " +
                "[class*='selected-path']"))).thenReturn(List.of(label));

        assertEquals("Asia/India/Chennai", handler.getSelectedPath(driver, element));
    }

    @Test
    void getSelectedPath_empty_returnsEmptyString() {
        when(element.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(element.getAttribute("title")).thenReturn(null);
        assertEquals("", handler.getSelectedPath(driver, element));
    }

    @Test
    void clearSelection_clicksClearButton() {
        WebElement clearBtn = mock(WebElement.class);
        when(clearBtn.isDisplayed()).thenReturn(true);
        when(element.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(element.findElements(By.cssSelector(
                ".ant-cascader-picker-clear, " +
                ".ant-select-clear, " +
                "[class*='clear'], " +
                "[aria-label='Clear']"))).thenReturn(List.of(clearBtn));

        handler.clearSelection(driver, element);
        verify(clearBtn).click();
    }
}
