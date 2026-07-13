package kroviq.wrapper.aggrid;

import kroviq.wrapper.core.GridHandler;
import kroviq.wrapper.factory.ComponentHandlerFactory;
import kroviq.wrapper.factory.GenericGridHandler;
import kroviq.wrapper.factory.UIFrameworkDetector;
import kroviq.wrapper.factory.UIFrameworkDetector.UIFramework;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgGridDetectionTest {

    @BeforeEach
    void setUp() {
        UIFrameworkDetector.clearCache();
    }

    @Test
    void detectsAgGridFromAgRootClass() {
        WebElement element = mockElement("ag-root-wrapper ag-ltr");
        assertEquals(UIFramework.AG_GRID, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsAgGridFromAgGridClass() {
        WebElement element = mockElement("ag-grid ag-theme-alpine");
        assertEquals(UIFramework.AG_GRID, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsAgGridFromAgBodyViewport() {
        WebElement element = mockElement("ag-body-viewport ag-layout-normal");
        assertEquals(UIFramework.AG_GRID, UIFrameworkDetector.detect(element));
    }

    @Test
    void detectsAgGridFromAgRow() {
        WebElement element = mockElement("ag-row ag-row-even ag-row-level-0");
        assertEquals(UIFramework.AG_GRID, UIFrameworkDetector.detect(element));
    }

    @Test
    void factoryReturnsAgGridHandler() {
        WebElement element = mockElement("ag-root-wrapper ag-ltr");
        WebDriver driver = mock(WebDriver.class);
        GridHandler handler = ComponentHandlerFactory.getGridHandler(element, driver);
        assertInstanceOf(AgGridHandler.class, handler);
    }

    @Test
    void factoryReturnsGenericGridHandlerForUnknown() {
        WebElement element = mock(WebElement.class);
        when(element.getAttribute("class")).thenReturn("custom-table-wrapper");
        when(element.getAttribute("role")).thenReturn(null);
        when(element.getAttribute("data-testid")).thenReturn(null);
        WebDriver driver = mock(WebDriver.class);
        GridHandler handler = ComponentHandlerFactory.getGridHandler(element, driver);
        assertInstanceOf(GenericGridHandler.class, handler);
    }

    @Test
    void agGridDoesNotConflictWithMUI() {
        WebElement element = mockElement("MuiDataGrid-root");
        assertEquals(UIFramework.MUI, UIFrameworkDetector.detect(element));
    }

    @Test
    void agGridDoesNotConflictWithAntD() {
        WebElement element = mockElement("ant-table ant-table-bordered");
        assertEquals(UIFramework.ANTD, UIFrameworkDetector.detect(element));
    }

    private WebElement mockElement(String classAttribute) {
        WebElement element = mock(WebElement.class);
        when(element.getAttribute("class")).thenReturn(classAttribute);
        return element;
    }
}
