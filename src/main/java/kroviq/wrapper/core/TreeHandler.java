package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.List;

public interface TreeHandler {

    void expandNode(WebDriver driver, WebElement treeRoot, String nodePath);

    void collapseNode(WebDriver driver, WebElement treeRoot, String nodePath);

    boolean isNodeExpanded(WebDriver driver, WebElement treeRoot, String nodePath);

    void selectNode(WebDriver driver, WebElement treeRoot, String nodePath);

    void checkNode(WebDriver driver, WebElement treeRoot, String nodePath);

    void uncheckNode(WebDriver driver, WebElement treeRoot, String nodePath);

    boolean isNodeChecked(WebDriver driver, WebElement treeRoot, String nodePath);

    List<String> getCheckedNodes(WebDriver driver, WebElement treeRoot);
}
