package kroviq.wrapper.factory;

import kroviq.wrapper.factory.UIFrameworkDetector.UIFramework;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UIFrameworkDetectorTest {

    @BeforeEach
    void setUp() {
        UIFrameworkDetector.clearCache();
    }

    @Test
    void detectsMUIFromClassAttribute() {
        WebElement element = mockElement("div", "MuiSelect-root MuiInputBase-root");
        assertEquals(UIFramework.MUI, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsMUIFromAutocompleteClass() {
        WebElement element = mockElement("div", "MuiAutocomplete-root MuiFormControl-root");
        assertEquals(UIFramework.MUI, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsMUIFromDialogClass() {
        WebElement element = mockElement("div", "MuiDialog-root MuiModal-root");
        assertEquals(UIFramework.MUI, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsMUIFromDataTestId() {
        WebElement element = mock(WebElement.class);
        when(element.getTagName()).thenReturn("div");
        when(element.getAttribute("class")).thenReturn("custom-class");
        when(element.getAttribute("role")).thenReturn(null);
        when(element.getAttribute("data-testid")).thenReturn("MuiSelect-demo");
        assertEquals(UIFramework.MUI, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsAntDFromClassAttribute() {
        WebElement element = mockElement("div", "ant-select ant-select-single");
        assertEquals(UIFramework.ANTD, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsAntDFromDropdownClass() {
        WebElement element = mockElement("div", "ant-picker ant-picker-focused");
        assertEquals(UIFramework.ANTD, UIFrameworkDetector.detect(element));
    }

    // ==================== ANGULAR MATERIAL DETECTION ====================

    @Test
    void detectsAngularMaterialFromTagName() {
        WebElement element = mockElement("mat-select", "mat-mdc-select");
        assertEquals(UIFramework.ANGULAR_MATERIAL, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsAngularMaterialFromMatTableTag() {
        WebElement element = mockElement("mat-table", "");
        assertEquals(UIFramework.ANGULAR_MATERIAL, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsAngularMaterialFromMatMdcClass() {
        WebElement element = mockElement("table", "mat-mdc-table cdk-table");
        assertEquals(UIFramework.ANGULAR_MATERIAL, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsAngularMaterialFromMatSelectClass() {
        WebElement element = mockElement("div", "mat-select-trigger");
        assertEquals(UIFramework.ANGULAR_MATERIAL, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsAngularMaterialFromMatDialogClass() {
        WebElement element = mockElement("div", "mat-dialog-container");
        assertEquals(UIFramework.ANGULAR_MATERIAL, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsAngularMaterialFromMatAutocompleteAttribute() {
        WebElement element = mock(WebElement.class);
        when(element.getTagName()).thenReturn("input");
        when(element.getAttribute("class")).thenReturn("custom-input");
        when(element.getAttribute("role")).thenReturn(null);
        when(element.getAttribute("data-testid")).thenReturn(null);
        when(element.getAttribute("matAutocomplete")).thenReturn("auto");
        assertEquals(UIFramework.ANGULAR_MATERIAL, UIFrameworkDetector.detect(element));
    }

    // ==================== PRIMENG DETECTION ====================

    @Test
    void detectsPrimeNGFromPDropdownTag() {
        WebElement element = mockElement("p-dropdown", "p-component p-dropdown");
        assertEquals(UIFramework.PRIMENG, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsPrimeNGFromPComponentClass() {
        WebElement element = mockElement("div", "p-component p-inputtext");
        assertEquals(UIFramework.PRIMENG, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsPrimeNGFromPElementClass() {
        WebElement element = mockElement("span", "p-element p-button");
        assertEquals(UIFramework.PRIMENG, UIFrameworkDetector.detect(element));
    }

    // ==================== UNKNOWN / EDGE CASES ====================

    @Test
    void returnsUnknownForPlainHTML() {
        WebElement element = mock(WebElement.class);
        when(element.getTagName()).thenReturn("div");
        when(element.getAttribute("class")).thenReturn("custom-dropdown my-select");
        when(element.getAttribute("role")).thenReturn(null);
        when(element.getAttribute("data-testid")).thenReturn(null);
        when(element.getAttribute("matAutocomplete")).thenReturn(null);
        when(element.getAttribute("matInput")).thenReturn(null);
        assertEquals(UIFramework.UNKNOWN, UIFrameworkDetector.detect(element));
    }

    @Test
    void returnsUnknownForNullClass() {
        WebElement element = mock(WebElement.class);
        when(element.getTagName()).thenReturn("div");
        when(element.getAttribute("class")).thenReturn(null);
        when(element.getAttribute("role")).thenReturn(null);
        when(element.getAttribute("data-testid")).thenReturn(null);
        when(element.getAttribute("matAutocomplete")).thenReturn(null);
        when(element.getAttribute("matInput")).thenReturn(null);
        assertEquals(UIFramework.UNKNOWN, UIFrameworkDetector.detect(element));
    }

    @Test
    void returnsUnknownForEmptyClass() {
        WebElement element = mock(WebElement.class);
        when(element.getTagName()).thenReturn("div");
        when(element.getAttribute("class")).thenReturn("");
        when(element.getAttribute("role")).thenReturn(null);
        when(element.getAttribute("data-testid")).thenReturn(null);
        when(element.getAttribute("matAutocomplete")).thenReturn(null);
        when(element.getAttribute("matInput")).thenReturn(null);
        assertEquals(UIFramework.UNKNOWN, UIFrameworkDetector.detect(element));
    }

    @Test
    void cachesDetectionResult() {
        WebElement element = mockElement("div", "MuiSelect-root");
        UIFrameworkDetector.detect(element);
        UIFrameworkDetector.detect(element);
        UIFrameworkDetector.detect(element);
        // getTagName called only once due to cache
        verify(element, times(1)).getTagName();
    }

    @Test
    void clearCacheAllowsRedetection() {
        WebElement element = mockElement("div", "MuiSelect-root");
        UIFrameworkDetector.detect(element);
        verify(element, times(1)).getTagName();

        UIFrameworkDetector.clearCache();
        UIFrameworkDetector.detect(element);
        verify(element, times(2)).getTagName();
    }

    @Test
    void handlesExceptionGracefully() {
        WebElement element = mock(WebElement.class);
        when(element.getTagName()).thenThrow(new RuntimeException("stale element"));
        assertEquals(UIFramework.UNKNOWN, UIFrameworkDetector.detect(element));
    }

    @Test
    void muiTakesPriorityOverAntDWhenBothPresent() {
        // Edge case: element has both markers (unlikely but defensive)
        WebElement element = mockElement("div", "MuiSelect-root ant-select");
        // "Mui" appears first in the string, so MUI wins
        assertEquals(UIFramework.MUI, UIFrameworkDetector.detect(element));
    }

    private WebElement mockElement(String tagName, String classAttribute) {
        WebElement element = mock(WebElement.class);
        when(element.getTagName()).thenReturn(tagName);
        when(element.getAttribute("class")).thenReturn(classAttribute);
        return element;
    }
}
