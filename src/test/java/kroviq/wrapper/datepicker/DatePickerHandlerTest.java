package kroviq.wrapper.datepicker;

import kroviq.wrapper.antd.AntDDatePickerHandler;
import kroviq.wrapper.angularmaterial.AngularMaterialDatePickerHandler;
import kroviq.wrapper.core.DatePickerHandler;
import kroviq.wrapper.factory.GenericDatePickerHandler;
import kroviq.wrapper.mui.MUIDatePickerHandler;
import kroviq.wrapper.primeng.PrimeNGDatePickerHandler;
import kroviq.wrapper.factory.ComponentHandlerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DatePickerHandlerTest {

    @Mock private WebDriver driver;
    @Mock private WebElement element;
    @Mock private WebElement inputElement;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // --- Interface compliance ---

    @Test
    void antDHandler_implementsInterface() {
        assertInstanceOf(DatePickerHandler.class, new AntDDatePickerHandler(driver));
    }

    @Test
    void muiHandler_implementsInterface() {
        assertInstanceOf(DatePickerHandler.class, new MUIDatePickerHandler(driver));
    }

    @Test
    void angularMaterialHandler_implementsInterface() {
        assertInstanceOf(DatePickerHandler.class, new AngularMaterialDatePickerHandler(driver));
    }

    @Test
    void primeNGHandler_implementsInterface() {
        assertInstanceOf(DatePickerHandler.class, new PrimeNGDatePickerHandler(driver));
    }

    @Test
    void genericHandler_implementsInterface() {
        assertInstanceOf(DatePickerHandler.class, new GenericDatePickerHandler(driver));
    }

    // --- GenericDatePickerHandler tests ---

    @Test
    void generic_html5DateInput_setsValueViaJs() {
        WebDriver jsDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        when(element.getTagName()).thenReturn("input");
        when(element.getAttribute("type")).thenReturn("date");
        when(element.findElements(any(By.class))).thenReturn(Collections.emptyList());

        GenericDatePickerHandler handler = new GenericDatePickerHandler(jsDriver);
        LocalDate date = LocalDate.of(2025, 6, 15);

        handler.selectDate(jsDriver, element, date);

        verify((JavascriptExecutor) jsDriver).executeScript(contains("nativeInputValueSetter"), eq(element), eq("2025-06-15"));
    }

    @Test
    void generic_textInput_setsViaJs() {
        WebDriver jsDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        when(element.getTagName()).thenReturn("input");
        when(element.getAttribute("type")).thenReturn("text");
        when(element.getAttribute("value")).thenReturn("15/06/2025");
        when(element.findElements(any(By.class))).thenReturn(Collections.emptyList());

        GenericDatePickerHandler handler = new GenericDatePickerHandler(jsDriver);
        handler.selectDate(jsDriver, element, LocalDate.of(2025, 6, 15));

        verify((JavascriptExecutor) jsDriver, atLeastOnce()).executeScript(anyString(), any());
    }

    @Test
    void generic_getCurrentValue_returnsInputValue() {
        when(element.getTagName()).thenReturn("input");
        when(element.getAttribute("value")).thenReturn("15/06/2025");
        when(element.findElements(any(By.class))).thenReturn(Collections.emptyList());

        GenericDatePickerHandler handler = new GenericDatePickerHandler(driver);
        String value = handler.getCurrentValue(driver, element);

        assertEquals("15/06/2025", value);
    }

    @Test
    void generic_getCurrentValue_nullReturnsEmpty() {
        when(element.getTagName()).thenReturn("input");
        when(element.getAttribute("value")).thenReturn(null);
        when(element.findElements(any(By.class))).thenReturn(Collections.emptyList());

        GenericDatePickerHandler handler = new GenericDatePickerHandler(driver);
        String value = handler.getCurrentValue(driver, element);

        assertEquals("", value);
    }

    @Test
    void generic_setDateValue_parsesAndDelegates() {
        WebDriver jsDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        when(element.getTagName()).thenReturn("input");
        when(element.getAttribute("type")).thenReturn("date");
        when(element.findElements(any(By.class))).thenReturn(Collections.emptyList());

        GenericDatePickerHandler handler = new GenericDatePickerHandler(jsDriver);
        handler.setDateValue(jsDriver, element, "15/06/2025");

        verify((JavascriptExecutor) jsDriver).executeScript(contains("nativeInputValueSetter"), eq(element), eq("2025-06-15"));
    }

    // --- MUI handler input resolution ---

    @Test
    void mui_resolvesInputFromWrapper() {
        when(element.getTagName()).thenReturn("div");
        when(element.findElements(By.cssSelector("input"))).thenReturn(List.of(inputElement));
        when(inputElement.getTagName()).thenReturn("input");
        when(inputElement.getAttribute("type")).thenReturn("text");
        when(inputElement.getAttribute("value")).thenReturn("06/15/2025");

        MUIDatePickerHandler handler = new MUIDatePickerHandler(driver);
        String value = handler.getCurrentValue(driver, element);

        assertEquals("06/15/2025", value);
    }

    // --- Angular Material handler input resolution ---

    @Test
    void angularMaterial_resolvesInputFromWrapper() {
        when(element.getTagName()).thenReturn("div");
        when(element.findElements(By.cssSelector("input[matInput], input[matDatepicker], input")))
                .thenReturn(List.of(inputElement));
        when(inputElement.getAttribute("value")).thenReturn("6/15/2025");

        AngularMaterialDatePickerHandler handler = new AngularMaterialDatePickerHandler(driver);
        String value = handler.getCurrentValue(driver, element);

        assertEquals("6/15/2025", value);
    }

    // --- PrimeNG handler input resolution ---

    @Test
    void primeNG_resolvesInputFromSpan() {
        when(element.getTagName()).thenReturn("span");
        when(element.findElements(By.cssSelector("input.p-inputtext, input")))
                .thenReturn(List.of(inputElement));
        when(inputElement.getAttribute("value")).thenReturn("06/15/2025");

        PrimeNGDatePickerHandler handler = new PrimeNGDatePickerHandler(driver);
        String value = handler.getCurrentValue(driver, element);

        assertEquals("06/15/2025", value);
    }

    // --- AntD handler input resolution ---

    @Test
    void antD_resolvesInputFromWrapper() {
        when(element.findElement(By.cssSelector(".ant-picker-input > input, input")))
                .thenReturn(inputElement);
        when(inputElement.getAttribute("value")).thenReturn("15/06/2025");

        AntDDatePickerHandler handler = new AntDDatePickerHandler(driver);
        String value = handler.getCurrentValue(driver, element);

        assertEquals("15/06/2025", value);
    }

    // --- Factory dispatch ---

    @Test
    void factory_antdElement_returnsAntDHandler() {
        when(element.getAttribute("class")).thenReturn("ant-picker ant-picker-focused");
        when(element.getTagName()).thenReturn("div");

        DatePickerHandler handler = ComponentHandlerFactory.getDatePickerHandler(element, driver);
        assertInstanceOf(AntDDatePickerHandler.class, handler);
    }

    @Test
    void factory_muiElement_returnsMUIHandler() {
        when(element.getAttribute("class")).thenReturn("MuiFormControl-root MuiTextField-root");
        when(element.getTagName()).thenReturn("div");
        when(element.getAttribute("data-testid")).thenReturn(null);

        DatePickerHandler handler = ComponentHandlerFactory.getDatePickerHandler(element, driver);
        assertInstanceOf(MUIDatePickerHandler.class, handler);
    }

    @Test
    void factory_angularElement_returnsAngularHandler() {
        when(element.getAttribute("class")).thenReturn("mat-form-field mat-mdc-form-field");
        when(element.getTagName()).thenReturn("mat-form-field");

        DatePickerHandler handler = ComponentHandlerFactory.getDatePickerHandler(element, driver);
        assertInstanceOf(AngularMaterialDatePickerHandler.class, handler);
    }

    @Test
    void factory_primengElement_returnsPrimeNGHandler() {
        when(element.getAttribute("class")).thenReturn("p-calendar p-component");
        when(element.getTagName()).thenReturn("p-calendar");

        DatePickerHandler handler = ComponentHandlerFactory.getDatePickerHandler(element, driver);
        assertInstanceOf(PrimeNGDatePickerHandler.class, handler);
    }

    @Test
    void factory_unknownElement_returnsGenericHandler() {
        when(element.getAttribute("class")).thenReturn("custom-date-field");
        when(element.getTagName()).thenReturn("div");
        when(element.getAttribute("data-testid")).thenReturn(null);

        DatePickerHandler handler = ComponentHandlerFactory.getDatePickerHandler(element, driver);
        assertInstanceOf(GenericDatePickerHandler.class, handler);
    }
}
