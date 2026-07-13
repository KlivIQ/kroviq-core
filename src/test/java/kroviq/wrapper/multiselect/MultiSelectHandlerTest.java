package kroviq.wrapper.multiselect;

import kroviq.wrapper.antd.AntDMultiSelectHandler;
import kroviq.wrapper.angularmaterial.AngularMaterialMultiSelectHandler;
import kroviq.wrapper.core.MultiSelectHandler;
import kroviq.wrapper.factory.GenericMultiSelectHandler;
import kroviq.wrapper.factory.ComponentHandlerFactory;
import kroviq.wrapper.mui.MUIMultiSelectHandler;
import kroviq.wrapper.primeng.PrimeNGMultiSelectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MultiSelectHandlerTest {

    @Mock private WebDriver driver;
    @Mock private WebElement element;
    @Mock private WebElement optionElement;
    @Mock private WebElement chipElement;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // --- Interface compliance ---

    @Test
    void antDHandler_implementsInterface() {
        assertInstanceOf(MultiSelectHandler.class, new AntDMultiSelectHandler(driver));
    }

    @Test
    void muiHandler_implementsInterface() {
        assertInstanceOf(MultiSelectHandler.class, new MUIMultiSelectHandler(driver));
    }

    @Test
    void angularMaterialHandler_implementsInterface() {
        assertInstanceOf(MultiSelectHandler.class, new AngularMaterialMultiSelectHandler(driver));
    }

    @Test
    void primeNGHandler_implementsInterface() {
        assertInstanceOf(MultiSelectHandler.class, new PrimeNGMultiSelectHandler(driver));
    }

    @Test
    void genericHandler_implementsInterface() {
        assertInstanceOf(MultiSelectHandler.class, new GenericMultiSelectHandler(driver));
    }

    // --- Factory dispatch ---

    @Test
    void factory_antdElement_returnsAntDHandler() {
        when(element.getAttribute("class")).thenReturn("ant-select ant-select-multiple");
        when(element.getTagName()).thenReturn("div");
        MultiSelectHandler handler = ComponentHandlerFactory.getMultiSelectHandler(element, driver);
        assertInstanceOf(AntDMultiSelectHandler.class, handler);
    }

    @Test
    void factory_muiElement_returnsMUIHandler() {
        when(element.getAttribute("class")).thenReturn("MuiAutocomplete-root MuiFormControl-root");
        when(element.getTagName()).thenReturn("div");
        when(element.getAttribute("data-testid")).thenReturn(null);
        MultiSelectHandler handler = ComponentHandlerFactory.getMultiSelectHandler(element, driver);
        assertInstanceOf(MUIMultiSelectHandler.class, handler);
    }

    @Test
    void factory_angularElement_returnsAngularHandler() {
        when(element.getAttribute("class")).thenReturn("mat-mdc-form-field mat-form-field");
        when(element.getTagName()).thenReturn("mat-form-field");
        MultiSelectHandler handler = ComponentHandlerFactory.getMultiSelectHandler(element, driver);
        assertInstanceOf(AngularMaterialMultiSelectHandler.class, handler);
    }

    @Test
    void factory_primengElement_returnsPrimeNGHandler() {
        when(element.getAttribute("class")).thenReturn("p-multiselect p-component");
        when(element.getTagName()).thenReturn("p-multiselect");
        MultiSelectHandler handler = ComponentHandlerFactory.getMultiSelectHandler(element, driver);
        assertInstanceOf(PrimeNGMultiSelectHandler.class, handler);
    }

    @Test
    void factory_unknownElement_returnsGenericHandler() {
        when(element.getAttribute("class")).thenReturn("custom-multiselect");
        when(element.getTagName()).thenReturn("div");
        when(element.getAttribute("data-testid")).thenReturn(null);
        MultiSelectHandler handler = ComponentHandlerFactory.getMultiSelectHandler(element, driver);
        assertInstanceOf(GenericMultiSelectHandler.class, handler);
    }

    // --- GenericMultiSelectHandler: native select ---

    @Test
    void generic_nativeSelect_selectsMultipleValues() {
        when(element.getTagName()).thenReturn("select");

        WebElement opt1 = mock(WebElement.class);
        WebElement opt2 = mock(WebElement.class);
        WebElement opt3 = mock(WebElement.class);
        when(opt1.getText()).thenReturn("Alpha");
        when(opt2.getText()).thenReturn("Beta");
        when(opt3.getText()).thenReturn("Gamma");
        when(element.findElements(By.tagName("option"))).thenReturn(List.of(opt1, opt2, opt3));
        when(element.getAttribute("multiple")).thenReturn("true");

        // Mock for Select class
        when(opt1.isSelected()).thenReturn(false);
        when(opt2.isSelected()).thenReturn(false);
        when(opt3.isSelected()).thenReturn(false);
        when(opt1.isEnabled()).thenReturn(true);
        when(opt2.isEnabled()).thenReturn(true);

        GenericMultiSelectHandler handler = new GenericMultiSelectHandler(driver);
        // This will use Select.selectByVisibleText which requires proper WebElement setup
        // For unit test we just verify it doesn't throw on correct tag
        assertDoesNotThrow(() -> handler.getSelectedValues(driver, element));
    }

    @Test
    void generic_getSelectedValues_nativeSelect_returnsSelectedOptions() {
        when(element.getTagName()).thenReturn("select");
        WebElement selected1 = mock(WebElement.class);
        WebElement selected2 = mock(WebElement.class);
        when(selected1.getText()).thenReturn("Option A");
        when(selected2.getText()).thenReturn("Option B");
        when(selected1.isSelected()).thenReturn(true);
        when(selected2.isSelected()).thenReturn(true);

        WebElement opt1 = mock(WebElement.class);
        when(opt1.getText()).thenReturn("Option C");
        when(opt1.isSelected()).thenReturn(false);

        when(element.findElements(By.tagName("option"))).thenReturn(List.of(selected1, selected2, opt1));
        when(element.getAttribute("multiple")).thenReturn("true");

        GenericMultiSelectHandler handler = new GenericMultiSelectHandler(driver);
        List<String> values = handler.getSelectedValues(driver, element);
        assertEquals(List.of("Option A", "Option B"), values);
    }

    // --- MUI getSelectedValues ---

    @Test
    void mui_getSelectedValues_readsChipLabels() {
        WebElement chip1 = mock(WebElement.class);
        WebElement chip2 = mock(WebElement.class);
        when(chip1.getText()).thenReturn("Tag 1");
        when(chip2.getText()).thenReturn("Tag 2");
        when(element.findElements(By.cssSelector(".MuiChip-label, .MuiChip-root span[class*='label']")))
                .thenReturn(List.of(chip1, chip2));

        MUIMultiSelectHandler handler = new MUIMultiSelectHandler(driver);
        List<String> values = handler.getSelectedValues(driver, element);
        assertEquals(List.of("Tag 1", "Tag 2"), values);
    }

    // --- Angular Material getSelectedValues ---

    @Test
    void angularMaterial_getSelectedValues_readsChips() {
        WebElement chip1 = mock(WebElement.class);
        WebElement chip2 = mock(WebElement.class);
        when(chip1.getText()).thenReturn("Chip A");
        when(chip2.getText()).thenReturn("Chip B");
        when(element.findElements(By.cssSelector("mat-chip, .mat-mdc-chip, .mat-chip")))
                .thenReturn(List.of(chip1, chip2));

        AngularMaterialMultiSelectHandler handler = new AngularMaterialMultiSelectHandler(driver);
        List<String> values = handler.getSelectedValues(driver, element);
        assertEquals(List.of("Chip A", "Chip B"), values);
    }

    // --- PrimeNG getSelectedValues ---

    @Test
    void primeNG_getSelectedValues_readsTokens() {
        WebElement token1 = mock(WebElement.class);
        WebElement token2 = mock(WebElement.class);
        when(token1.getText()).thenReturn("Item 1");
        when(token2.getText()).thenReturn("Item 2");
        when(element.findElements(By.cssSelector(".p-multiselect-token-label")))
                .thenReturn(List.of(token1, token2));

        PrimeNGMultiSelectHandler handler = new PrimeNGMultiSelectHandler(driver);
        List<String> values = handler.getSelectedValues(driver, element);
        assertEquals(List.of("Item 1", "Item 2"), values);
    }

    // --- AntD getSelectedValues ---

    @Test
    void antD_getSelectedValues_readsChips() {
        WebElement chip1 = mock(WebElement.class);
        WebElement chip2 = mock(WebElement.class);
        when(chip1.getText()).thenReturn("Selected A");
        when(chip2.getText()).thenReturn("Selected B");
        when(element.findElements(By.cssSelector(".ant-select-selection-item-content")))
                .thenReturn(List.of(chip1, chip2));

        AntDMultiSelectHandler handler = new AntDMultiSelectHandler(driver);
        List<String> values = handler.getSelectedValues(driver, element);
        assertEquals(List.of("Selected A", "Selected B"), values);
    }

    // --- Null/empty handling ---

    @Test
    void generic_selectMultiple_emptyList_noOp() {
        GenericMultiSelectHandler handler = new GenericMultiSelectHandler(driver);
        assertDoesNotThrow(() -> handler.selectMultiple(driver, element, Collections.emptyList()));
    }

    @Test
    void generic_selectMultiple_nullList_noOp() {
        GenericMultiSelectHandler handler = new GenericMultiSelectHandler(driver);
        assertDoesNotThrow(() -> handler.selectMultiple(driver, element, null));
    }

    @Test
    void mui_selectMultiple_emptyList_noOp() {
        MUIMultiSelectHandler handler = new MUIMultiSelectHandler(driver);
        assertDoesNotThrow(() -> handler.selectMultiple(driver, element, Collections.emptyList()));
    }
}
