package kroviq.wrapper.factory;

import kroviq.wrapper.primeng.PrimeNGAutoCompleteHandler;
import kroviq.wrapper.primeng.PrimeNGDialogHandler;
import kroviq.wrapper.primeng.PrimeNGDropdownHandler;
import kroviq.wrapper.primeng.PrimeNGTableHandler;
import kroviq.wrapper.antd.AntDDropdownAdapter;
import kroviq.wrapper.core.AutoCompleteHandler;
import kroviq.wrapper.core.DialogHandler;
import kroviq.wrapper.core.DropdownHandler;
import kroviq.wrapper.core.GridHandler;
import kroviq.wrapper.mui.MUIAutoCompleteHandler;
import kroviq.wrapper.mui.MUIDialogHandler;
import kroviq.wrapper.mui.MUIDropdownHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComponentHandlerFactoryTest {

    private WebDriver driver;

    @BeforeEach
    void setUp() {
        UIFrameworkDetector.clearCache();
        driver = mock(WebDriver.class);
    }

    // --- Dropdown Handler routing ---

    @Test
    void returnsMUIDropdownHandlerForMUIElement() {
        WebElement element = muiElement("MuiSelect-root");
        DropdownHandler handler = ComponentHandlerFactory.getDropdownHandler(element, driver);
        assertInstanceOf(MUIDropdownHandler.class, handler);
    }

    @Test
    void returnsAntDDropdownAdapterForAntDElement() {
        WebElement element = antdElement("ant-select ant-select-single");
        DropdownHandler handler = ComponentHandlerFactory.getDropdownHandler(element, driver);
        assertInstanceOf(AntDDropdownAdapter.class, handler);
    }

    @Test
    void returnsGenericDropdownHandlerForUnknownElement() {
        WebElement element = unknownElement();
        DropdownHandler handler = ComponentHandlerFactory.getDropdownHandler(element, driver);
        assertInstanceOf(GenericDropdownHandler.class, handler);
    }

    // --- AutoComplete Handler routing ---

    @Test
    void returnsMUIAutoCompleteHandlerForMUIElement() {
        WebElement element = muiElement("MuiAutocomplete-root");
        AutoCompleteHandler handler = ComponentHandlerFactory.getAutoCompleteHandler(element);
        assertInstanceOf(MUIAutoCompleteHandler.class, handler);
    }

    @Test
    void returnsMUIAutoCompleteHandlerAsDefaultForUnknown() {
        WebElement element = unknownElement();
        AutoCompleteHandler handler = ComponentHandlerFactory.getAutoCompleteHandler(element);
        // Generic autocomplete follows MUI pattern (type + select from listbox)
        assertInstanceOf(MUIAutoCompleteHandler.class, handler);
    }

    // --- Dialog Handler routing ---

    @Test
    void returnsMUIDialogHandlerForMUIElement() {
        WebElement element = muiElement("MuiDialog-root");
        DialogHandler handler = ComponentHandlerFactory.getDialogHandler(element);
        assertInstanceOf(MUIDialogHandler.class, handler);
    }

    @Test
    void returnsGenericDialogHandlerForUnknownElement() {
        WebElement element = unknownElement();
        DialogHandler handler = ComponentHandlerFactory.getDialogHandler(element);
        assertInstanceOf(GenericDialogHandler.class, handler);
    }

    // --- PrimeNG Handler routing ---

    @Test
    void returnsPrimeNGDropdownHandlerForPrimeNGElement() {
        WebElement element = primeNGElement("p-dropdown", "p-component p-dropdown");
        DropdownHandler handler = ComponentHandlerFactory.getDropdownHandler(element, driver);
        assertInstanceOf(PrimeNGDropdownHandler.class, handler);
    }

    @Test
    void returnsPrimeNGAutoCompleteHandlerForPrimeNGElement() {
        WebElement element = primeNGElement("p-autocomplete", "p-component p-autocomplete");
        AutoCompleteHandler handler = ComponentHandlerFactory.getAutoCompleteHandler(element);
        assertInstanceOf(PrimeNGAutoCompleteHandler.class, handler);
    }

    @Test
    void returnsPrimeNGDialogHandlerForPrimeNGElement() {
        WebElement element = primeNGElement("p-dialog", "p-component");
        DialogHandler handler = ComponentHandlerFactory.getDialogHandler(element);
        assertInstanceOf(PrimeNGDialogHandler.class, handler);
    }

    @Test
    void returnsPrimeNGTableHandlerForPrimeNGElement() {
        WebElement element = primeNGElement("p-table", "p-component p-datatable");
        GridHandler handler = ComponentHandlerFactory.getGridHandler(element, driver);
        assertInstanceOf(PrimeNGTableHandler.class, handler);
    }

    // --- Helpers ---

    private WebElement primeNGElement(String tagName, String className) {
        WebElement element = mock(WebElement.class);
        when(element.getTagName()).thenReturn(tagName);
        when(element.getAttribute("class")).thenReturn(className);
        return element;
    }

    private WebElement muiElement(String className) {
        WebElement element = mock(WebElement.class);
        when(element.getAttribute("class")).thenReturn(className);
        return element;
    }

    private WebElement antdElement(String className) {
        WebElement element = mock(WebElement.class);
        when(element.getAttribute("class")).thenReturn(className);
        return element;
    }

    private WebElement unknownElement() {
        WebElement element = mock(WebElement.class);
        when(element.getAttribute("class")).thenReturn("plain-select");
        when(element.getAttribute("role")).thenReturn(null);
        when(element.getAttribute("data-testid")).thenReturn(null);
        return element;
    }
}
