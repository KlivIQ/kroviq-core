package kroviq.wrapper.mui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MUIDropdownHandlerTest {

    private MUIDropdownHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MUIDropdownHandler();
    }

    @Test
    void selectClicksTriggerElement() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        WebElement trigger = mock(WebElement.class);
        // Stub findElement to throw NoSuchElementException (FluentWait retries until timeout)
        when(driver.findElement(any(By.class))).thenThrow(new NoSuchElementException("no listbox"));

        try {
            handler.select(driver, trigger, "Option A");
        } catch (TimeoutException | NoSuchElementException e) {
            // Expected — no real DOM
        }

        verify(trigger).click();
    }

    @Test
    void getOptionsClicksTriggerElement() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        WebElement trigger = mock(WebElement.class);
        when(driver.findElement(any(By.class))).thenThrow(new NoSuchElementException("no listbox"));

        try {
            handler.getOptions(driver, trigger);
        } catch (TimeoutException | NoSuchElementException e) {
            // Expected — no real DOM
        }

        verify(trigger).click();
    }

    @Test
    void implementsDropdownHandlerInterface() {
        assertInstanceOf(kroviq.wrapper.core.DropdownHandler.class, handler);
    }
}
