package kroviq.wrapper.mui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MUIDialogHandlerTest {

    private MUIDialogHandler handler;
    private WebDriver driver;

    @BeforeEach
    void setUp() {
        handler = new MUIDialogHandler();
        driver = mock(WebDriver.class);
    }

    @Test
    void isOpenReturnsTrueWhenDialogVisible() {
        WebElement dialog = mock(WebElement.class);
        when(dialog.isDisplayed()).thenReturn(true);
        when(driver.findElements(By.cssSelector(".MuiDialog-root.MuiModal-root:not(.MuiModal-hidden)")))
                .thenReturn(List.of(dialog));

        assertTrue(handler.isOpen(driver));
    }

    @Test
    void isOpenReturnsFalseWhenNoDialog() {
        when(driver.findElements(By.cssSelector(".MuiDialog-root.MuiModal-root:not(.MuiModal-hidden)")))
                .thenReturn(Collections.emptyList());

        assertFalse(handler.isOpen(driver));
    }

    @Test
    void isOpenReturnsFalseWhenDialogHidden() {
        WebElement dialog = mock(WebElement.class);
        when(dialog.isDisplayed()).thenReturn(false);
        when(driver.findElements(By.cssSelector(".MuiDialog-root.MuiModal-root:not(.MuiModal-hidden)")))
                .thenReturn(List.of(dialog));

        assertFalse(handler.isOpen(driver));
    }

    @Test
    void isOpenHandlesStaleElementGracefully() {
        WebElement dialog = mock(WebElement.class);
        when(dialog.isDisplayed()).thenThrow(new org.openqa.selenium.StaleElementReferenceException("stale"));
        when(driver.findElements(By.cssSelector(".MuiDialog-root.MuiModal-root:not(.MuiModal-hidden)")))
                .thenReturn(List.of(dialog));

        assertFalse(handler.isOpen(driver));
    }
}
