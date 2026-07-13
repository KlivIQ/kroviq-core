package kroviq.wrapper.tree;

import kroviq.wrapper.core.TreeHandler;
import kroviq.wrapper.factory.ComponentHandlerFactory;
import kroviq.wrapper.factory.GenericTreeHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TreeHandlerTest {

    @Mock private WebDriver driver;
    @Mock private WebElement treeRoot;
    @Mock private WebElement nodeElement;
    @Mock private WebElement labelElement;

    private GenericTreeHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new GenericTreeHandler(driver);
    }

    @Test
    void implementsInterface() {
        assertInstanceOf(TreeHandler.class, handler);
    }

    @Test
    void factory_returnsGenericHandler() {
        when(treeRoot.getAttribute("class")).thenReturn("custom-tree");
        when(treeRoot.getTagName()).thenReturn("div");
        when(treeRoot.getAttribute("data-testid")).thenReturn(null);
        TreeHandler h = ComponentHandlerFactory.getTreeHandler(treeRoot, driver);
        assertInstanceOf(GenericTreeHandler.class, h);
    }

    @Test
    void isNodeExpanded_ariaTrue_returnsTrue() {
        // Build mock tree: root → nodeElement with aria-expanded=true
        when(treeRoot.findElements(By.cssSelector("[role='treeitem']"))).thenReturn(List.of(nodeElement));
        when(nodeElement.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(nodeElement.getText()).thenReturn("TestNode");
        when(nodeElement.getAttribute("aria-expanded")).thenReturn("true");

        assertTrue(handler.isNodeExpanded(driver, treeRoot, "TestNode"));
    }

    @Test
    void isNodeExpanded_ariaFalse_returnsFalse() {
        when(treeRoot.findElements(By.cssSelector("[role='treeitem']"))).thenReturn(List.of(nodeElement));
        when(nodeElement.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(nodeElement.getText()).thenReturn("TestNode");
        when(nodeElement.getAttribute("aria-expanded")).thenReturn("false");

        assertFalse(handler.isNodeExpanded(driver, treeRoot, "TestNode"));
    }

    @Test
    void isNodeChecked_ariaTrue_returnsTrue() {
        when(treeRoot.findElements(By.cssSelector("[role='treeitem']"))).thenReturn(List.of(nodeElement));
        when(nodeElement.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(nodeElement.getText()).thenReturn("PermNode");
        when(nodeElement.getAttribute("aria-checked")).thenReturn("true");

        assertTrue(handler.isNodeChecked(driver, treeRoot, "PermNode"));
    }

    @Test
    void isNodeChecked_ariaFalse_returnsFalse() {
        when(treeRoot.findElements(By.cssSelector("[role='treeitem']"))).thenReturn(List.of(nodeElement));
        when(nodeElement.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(nodeElement.getText()).thenReturn("PermNode");
        when(nodeElement.getAttribute("aria-checked")).thenReturn("false");
        when(nodeElement.getAttribute("aria-selected")).thenReturn(null);
        when(nodeElement.getAttribute("class")).thenReturn("");

        assertFalse(handler.isNodeChecked(driver, treeRoot, "PermNode"));
    }

    @Test
    void navigateToNode_nodeNotFound_throwsException() {
        when(treeRoot.findElements(any(By.class))).thenReturn(Collections.emptyList());

        assertThrows(RuntimeException.class, () ->
                handler.isNodeExpanded(driver, treeRoot, "NonexistentNode"));
    }

    @Test
    void getCheckedNodes_returnsCheckedItemTexts() {
        WebElement checked1 = mock(WebElement.class);
        WebElement checked2 = mock(WebElement.class);
        when(checked1.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(checked1.getText()).thenReturn("Node A");
        when(checked2.findElements(any(By.class))).thenReturn(Collections.emptyList());
        when(checked2.getText()).thenReturn("Node B");
        when(treeRoot.findElements(any(By.class))).thenReturn(List.of(checked1, checked2));

        List<String> result = handler.getCheckedNodes(driver, treeRoot);
        assertEquals(2, result.size());
    }

    @Test
    void getCheckedNodes_emptyTree_returnsEmptyList() {
        when(treeRoot.findElements(any(By.class))).thenReturn(Collections.emptyList());
        List<String> result = handler.getCheckedNodes(driver, treeRoot);
        assertTrue(result.isEmpty());
    }
}
