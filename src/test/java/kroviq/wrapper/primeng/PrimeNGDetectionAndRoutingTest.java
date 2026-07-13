package kroviq.wrapper.primeng;

import kroviq.wrapper.core.AutoCompleteHandler;
import kroviq.wrapper.core.DialogHandler;
import kroviq.wrapper.core.DropdownHandler;
import kroviq.wrapper.core.GridHandler;
import kroviq.wrapper.factory.ComponentHandlerFactory;
import kroviq.wrapper.factory.UIFrameworkDetector;
import kroviq.wrapper.factory.UIFrameworkDetector.UIFramework;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrimeNGDetectionAndRoutingTest {

    @BeforeEach
    void setUp() {
        UIFrameworkDetector.clearCache();
    }

    @Nested
    class Detection {

        @Test
        void detectsPrimeNGFromPDropdownTag() {
            WebElement element = mockElement("p-dropdown", "p-component p-dropdown");
            assertEquals(UIFramework.PRIMENG, UIFrameworkDetector.detect(element));
        }

        @Test
        void detectsPrimeNGFromPAutocompleteTag() {
            WebElement element = mockElement("p-autocomplete", "p-component p-autocomplete");
            assertEquals(UIFramework.PRIMENG, UIFrameworkDetector.detect(element));
        }

        @Test
        void detectsPrimeNGFromPDialogTag() {
            WebElement element = mockElement("p-dialog", "p-component");
            assertEquals(UIFramework.PRIMENG, UIFrameworkDetector.detect(element));
        }

        @Test
        void detectsPrimeNGFromPTableTag() {
            WebElement element = mockElement("p-table", "p-component p-datatable");
            assertEquals(UIFramework.PRIMENG, UIFrameworkDetector.detect(element));
        }

        @Test
        void detectsPrimeNGFromPComponentClass() {
            WebElement element = mockElement("div", "p-component p-dropdown-trigger");
            assertEquals(UIFramework.PRIMENG, UIFrameworkDetector.detect(element));
        }

        @Test
        void detectsPrimeNGFromPElementClass() {
            WebElement element = mockElement("div", "p-element p-inputtext");
            assertEquals(UIFramework.PRIMENG, UIFrameworkDetector.detect(element));
        }

        @Test
        void doesNotConfusePrimeNGWithParagraphTag() {
            // <p> tag should NOT trigger PrimeNG detection
            WebElement element = mock(WebElement.class);
            when(element.getTagName()).thenReturn("p");
            when(element.getAttribute("class")).thenReturn("text-content");
            when(element.getAttribute("role")).thenReturn(null);
            when(element.getAttribute("data-testid")).thenReturn(null);
            when(element.getAttribute("matAutocomplete")).thenReturn(null);
            when(element.getAttribute("matInput")).thenReturn(null);
            // "p" does not start with "p-" so should not match
            assertEquals(UIFramework.UNKNOWN, UIFrameworkDetector.detect(element));
        }

        @Test
        void primeNGDetectionDoesNotConflictWithAngularMaterial() {
            WebElement matElement = mockElement("mat-select", "mat-mdc-select");
            assertEquals(UIFramework.ANGULAR_MATERIAL, UIFrameworkDetector.detect(matElement));
        }
    }

    @Nested
    class FactoryRouting {

        private WebDriver driver;

        @BeforeEach
        void setUp() {
            driver = mock(WebDriver.class);
        }

        @Test
        void routesPrimeNGDropdownHandler() {
            WebElement element = mockElement("p-dropdown", "p-component p-dropdown");
            DropdownHandler handler = ComponentHandlerFactory.getDropdownHandler(element, driver);
            assertInstanceOf(PrimeNGDropdownHandler.class, handler);
        }

        @Test
        void routesPrimeNGAutoCompleteHandler() {
            WebElement element = mockElement("p-autocomplete", "p-component p-autocomplete");
            AutoCompleteHandler handler = ComponentHandlerFactory.getAutoCompleteHandler(element);
            assertInstanceOf(PrimeNGAutoCompleteHandler.class, handler);
        }

        @Test
        void routesPrimeNGDialogHandler() {
            WebElement element = mockElement("p-dialog", "p-component");
            DialogHandler handler = ComponentHandlerFactory.getDialogHandler(element);
            assertInstanceOf(PrimeNGDialogHandler.class, handler);
        }

        @Test
        void routesPrimeNGTableHandler() {
            WebElement element = mockElement("p-table", "p-component p-datatable");
            GridHandler handler = ComponentHandlerFactory.getGridHandler(element, driver);
            assertInstanceOf(PrimeNGTableHandler.class, handler);
        }
    }

    private WebElement mockElement(String tagName, String classAttribute) {
        WebElement element = mock(WebElement.class);
        when(element.getTagName()).thenReturn(tagName);
        when(element.getAttribute("class")).thenReturn(classAttribute);
        return element;
    }
}
