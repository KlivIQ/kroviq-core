package kroviq.wrapper.datetime;

import kroviq.wrapper.core.DateTimeHandler;
import kroviq.wrapper.factory.ComponentHandlerFactory;
import kroviq.wrapper.factory.GenericDateTimeHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DateTimeHandlerTest {

    private WebDriver driver;
    private WebElement element;
    private GenericDateTimeHandler handler;

    @BeforeEach
    void setUp() {
        driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        element = mock(WebElement.class);
        handler = new GenericDateTimeHandler(driver);
    }

    @Test
    void implementsInterface() {
        assertInstanceOf(DateTimeHandler.class, handler);
    }

    @Test
    void factory_returnsGenericHandler() {
        when(element.getAttribute("class")).thenReturn("datetime-field");
        when(element.getTagName()).thenReturn("div");
        when(element.getAttribute("data-testid")).thenReturn(null);
        DateTimeHandler h = ComponentHandlerFactory.getDateTimeHandler(element, driver);
        assertInstanceOf(GenericDateTimeHandler.class, h);
    }

    @Test
    void parseDateTime_ddMMyyyyHHmm() {
        LocalDateTime dt = GenericDateTimeHandler.parseDateTime("15/06/2025 14:30");
        assertEquals(2025, dt.getYear());
        assertEquals(6, dt.getMonthValue());
        assertEquals(15, dt.getDayOfMonth());
        assertEquals(14, dt.getHour());
        assertEquals(30, dt.getMinute());
    }

    @Test
    void parseDateTime_yyyyMMddTHHmm() {
        LocalDateTime dt = GenericDateTimeHandler.parseDateTime("2025-06-15T09:00");
        assertEquals(2025, dt.getYear());
        assertEquals(9, dt.getHour());
        assertEquals(0, dt.getMinute());
    }

    @Test
    void parseDateTime_MMddyyyyHHmm() {
        LocalDateTime dt = GenericDateTimeHandler.parseDateTime("06/15/2025 23:59");
        assertEquals(6, dt.getMonthValue());
        assertEquals(15, dt.getDayOfMonth());
        assertEquals(23, dt.getHour());
    }

    @Test
    void parseDateTime_dateOnlyFallback() {
        LocalDateTime dt = GenericDateTimeHandler.parseDateTime("15/06/2025");
        assertEquals(2025, dt.getYear());
        assertEquals(0, dt.getHour());
        assertEquals(0, dt.getMinute());
    }

    @Test
    void parseDateTime_invalid_throwsException() {
        assertThrows(RuntimeException.class, () -> GenericDateTimeHandler.parseDateTime("not-a-date"));
    }

    @Test
    void selectDateTime_html5Input_setsViaJs() {
        when(element.getTagName()).thenReturn("input");
        when(element.getAttribute("type")).thenReturn("datetime-local");
        when(element.findElements(any(By.class))).thenReturn(Collections.emptyList());

        handler.selectDateTime(driver, element, LocalDateTime.of(2025, 6, 15, 14, 30));

        verify((JavascriptExecutor) driver).executeScript(contains("nativeInputValueSetter"), eq(element), eq("2025-06-15T14:30"));
    }

    @Test
    void getCurrentValue_returnsInputValue() {
        when(element.getTagName()).thenReturn("input");
        when(element.getAttribute("value")).thenReturn("2025-06-15T14:30");
        when(element.findElements(any(By.class))).thenReturn(Collections.emptyList());

        assertEquals("2025-06-15T14:30", handler.getCurrentValue(driver, element));
    }
}
