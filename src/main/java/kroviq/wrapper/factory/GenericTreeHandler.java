package kroviq.wrapper.factory;

import kroviq.wrapper.core.HandlerUtils;
import kroviq.wrapper.core.TreeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GenericTreeHandler implements TreeHandler {

    private static final Logger logger = LogManager.getLogger(GenericTreeHandler.class);
    private static final String PATH_SEPARATOR = "/";
    private final WebDriver driver;

    public GenericTreeHandler(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void expandNode(WebDriver driver, WebElement treeRoot, String nodePath) {
        WebElement node = navigateToNode(driver, treeRoot, nodePath, true);
        if (!isExpanded(node)) {
            clickToggle(driver, node);
            waitForChildren(driver, node);
            logger.info("[Tree] Expanded: {}", nodePath);
        }
    }

    @Override
    public void collapseNode(WebDriver driver, WebElement treeRoot, String nodePath) {
        WebElement node = navigateToNode(driver, treeRoot, nodePath, false);
        if (isExpanded(node)) {
            clickToggle(driver, node);
            logger.info("[Tree] Collapsed: {}", nodePath);
        }
    }

    @Override
    public boolean isNodeExpanded(WebDriver driver, WebElement treeRoot, String nodePath) {
        WebElement node = navigateToNode(driver, treeRoot, nodePath, false);
        return isExpanded(node);
    }

    @Override
    public void selectNode(WebDriver driver, WebElement treeRoot, String nodePath) {
        WebElement node = navigateToNode(driver, treeRoot, nodePath, true);
        WebElement clickTarget = getNodeLabel(node);
        HandlerUtils.scrollIntoViewAndClick(driver, clickTarget);
        logger.info("[Tree] Selected: {}", nodePath);
    }

    @Override
    public void checkNode(WebDriver driver, WebElement treeRoot, String nodePath) {
        WebElement node = navigateToNode(driver, treeRoot, nodePath, true);
        if (!isChecked(node)) {
            clickCheckbox(driver, node);
            logger.info("[Tree] Checked: {}", nodePath);
        }
    }

    @Override
    public void uncheckNode(WebDriver driver, WebElement treeRoot, String nodePath) {
        WebElement node = navigateToNode(driver, treeRoot, nodePath, true);
        if (isChecked(node)) {
            clickCheckbox(driver, node);
            logger.info("[Tree] Unchecked: {}", nodePath);
        }
    }

    @Override
    public boolean isNodeChecked(WebDriver driver, WebElement treeRoot, String nodePath) {
        WebElement node = navigateToNode(driver, treeRoot, nodePath, false);
        return isChecked(node);
    }

    @Override
    public List<String> getCheckedNodes(WebDriver driver, WebElement treeRoot) {
        List<String> checked = new ArrayList<>();
        List<WebElement> allNodes = treeRoot.findElements(By.cssSelector(
                "[role='treeitem'][aria-checked='true'], " +
                ".ant-tree-treenode-checkbox-checked, " +
                ".p-treenode .p-checkbox-checked, " +
                "[class*='tree'] [class*='checked']"));
        for (WebElement node : allNodes) {
            String text = getNodeText(node);
            if (!text.isEmpty()) checked.add(text);
        }
        return checked;
    }

    // --- Path Navigation ---

    protected WebElement navigateToNode(WebDriver driver, WebElement treeRoot, String nodePath, boolean expandPath) {
        String[] segments = nodePath.split(PATH_SEPARATOR);
        WebElement currentScope = treeRoot;

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            boolean isLast = (i == segments.length - 1);

            WebElement node = findNodeInScope(currentScope, segment);
            if (node == null) {
                throw new RuntimeException("[Tree] Node '" + segment + "' not found at level " + (i + 1)
                        + " in path: " + nodePath);
            }

            if (!isLast && expandPath && !isExpanded(node)) {
                clickToggle(driver, node);
                waitForChildren(driver, node);
            }

            if (!isLast) {
                currentScope = getChildrenContainer(node);
                if (currentScope == null) currentScope = node;
            } else {
                return node;
            }
        }
        throw new RuntimeException("[Tree] Path navigation failed: " + nodePath);
    }

    protected WebElement findNodeInScope(WebElement scope, String nodeText) {
        // Strategy 1: ARIA treeitem
        List<WebElement> items = scope.findElements(By.cssSelector("[role='treeitem']"));
        for (WebElement item : items) {
            if (getNodeText(item).equalsIgnoreCase(nodeText)) return item;
        }

        // Strategy 2: AntD tree nodes
        items = scope.findElements(By.cssSelector(".ant-tree-treenode"));
        for (WebElement item : items) {
            if (getNodeText(item).equalsIgnoreCase(nodeText)) return item;
        }

        // Strategy 3: PrimeNG tree nodes
        items = scope.findElements(By.cssSelector(".p-treenode"));
        for (WebElement item : items) {
            if (getNodeText(item).equalsIgnoreCase(nodeText)) return item;
        }

        // Strategy 4: Angular Material tree nodes
        items = scope.findElements(By.cssSelector("mat-tree-node, mat-nested-tree-node"));
        for (WebElement item : items) {
            if (getNodeText(item).equalsIgnoreCase(nodeText)) return item;
        }

        // Strategy 5: Generic — any element with matching text in tree context
        items = scope.findElements(By.xpath(
                ".//*[contains(@class,'tree') or contains(@class,'node') or @role='treeitem']" +
                "[.//text()[normalize-space()='" + nodeText + "']]"));
        if (!items.isEmpty()) return items.get(0);

        return null;
    }

    protected String getNodeText(WebElement node) {
        // Try specific label elements first
        String[] labelSelectors = {
                ".ant-tree-title",
                ".p-treenode-label",
                "[class*='tree-node-label']",
                "[class*='node-content']",
                "span[class*='label']"
        };
        for (String sel : labelSelectors) {
            List<WebElement> labels = node.findElements(By.cssSelector(sel));
            for (WebElement label : labels) {
                String text = label.getText().trim();
                if (!text.isEmpty()) return text;
            }
        }
        // Fallback: direct text of node (excluding child node text)
        String text = node.getText().trim();
        if (text.contains("\n")) text = text.split("\n")[0].trim();
        return text;
    }

    protected WebElement getNodeLabel(WebElement node) {
        String[] selectors = { ".ant-tree-title", ".p-treenode-label", "span[class*='label']", "[class*='content']" };
        for (String sel : selectors) {
            List<WebElement> labels = node.findElements(By.cssSelector(sel));
            if (!labels.isEmpty()) return labels.get(0);
        }
        return node;
    }

    protected boolean isExpanded(WebElement node) {
        String expanded = node.getAttribute("aria-expanded");
        if (expanded != null) return "true".equals(expanded);

        String className = node.getAttribute("class");
        if (className != null) {
            return className.contains("expanded") || className.contains("ant-tree-treenode-switcher-open")
                    || className.contains("p-treenode-expanded");
        }

        // Check switcher icon state
        List<WebElement> switchers = node.findElements(By.cssSelector(
                ".ant-tree-switcher_open, [class*='expanded'], [class*='open']"));
        return !switchers.isEmpty();
    }

    protected boolean isChecked(WebElement node) {
        String checked = node.getAttribute("aria-checked");
        if (checked != null) return "true".equals(checked);

        String selected = node.getAttribute("aria-selected");
        if (selected != null) return "true".equals(selected);

        String className = node.getAttribute("class");
        if (className != null) {
            return className.contains("checked") || className.contains("selected");
        }

        // Check inner checkbox
        List<WebElement> checkboxes = node.findElements(By.cssSelector(
                "input[type='checkbox'], .ant-tree-checkbox, .p-checkbox"));
        for (WebElement cb : checkboxes) {
            if (cb.isSelected()) return true;
            String cbClass = cb.getAttribute("class");
            if (cbClass != null && (cbClass.contains("checked") || cbClass.contains("selected"))) return true;
            String cbAria = cb.getAttribute("aria-checked");
            if ("true".equals(cbAria)) return true;
        }
        return false;
    }

    protected void clickToggle(WebDriver driver, WebElement node) {
        String[] toggleSelectors = {
                ".ant-tree-switcher",
                ".p-tree-toggler",
                "[matTreeNodeToggle]",
                "button[aria-label*='expand' i]",
                "button[aria-label*='toggle' i]",
                "[class*='toggle']",
                "[class*='switcher']",
                "[class*='expand-icon']"
        };
        for (String sel : toggleSelectors) {
            List<WebElement> toggles = node.findElements(By.cssSelector(sel));
            for (WebElement toggle : toggles) {
                try {
                    if (toggle.isDisplayed()) {
                        HandlerUtils.scrollIntoViewAndClick(driver, toggle);
                        return;
                    }
                } catch (Exception e) { /* try next */ }
            }
        }
        // Fallback: click the node itself (some trees expand on node click)
        HandlerUtils.clickSafe(driver, node);
    }

    protected void clickCheckbox(WebDriver driver, WebElement node) {
        String[] cbSelectors = {
                ".ant-tree-checkbox",
                ".p-checkbox",
                "mat-checkbox",
                "input[type='checkbox']",
                "[class*='checkbox']",
                "[role='checkbox']"
        };
        for (String sel : cbSelectors) {
            List<WebElement> checkboxes = node.findElements(By.cssSelector(sel));
            for (WebElement cb : checkboxes) {
                try {
                    if (cb.isDisplayed()) {
                        HandlerUtils.scrollIntoViewAndClick(driver, cb);
                        return;
                    }
                } catch (Exception e) { /* try next */ }
            }
        }
        throw new RuntimeException("[Tree] No checkbox found in node");
    }

    protected WebElement getChildrenContainer(WebElement node) {
        String[] childSelectors = {
                "[role='group']",
                ".ant-tree-treenode-children, .ant-tree-child-tree",
                ".p-treenode-children",
                "[class*='children']",
                "[class*='subtree']"
        };
        for (String sel : childSelectors) {
            List<WebElement> containers = node.findElements(By.cssSelector(sel));
            if (!containers.isEmpty()) return containers.get(0);
        }
        // For flat DOM trees, the parent scope may contain siblings
        return null;
    }

    protected void waitForChildren(WebDriver driver, WebElement node) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> {
                WebElement container = getChildrenContainer(node);
                if (container != null) {
                    List<WebElement> children = container.findElements(By.cssSelector(
                            "[role='treeitem'], .ant-tree-treenode, .p-treenode, mat-tree-node"));
                    return !children.isEmpty();
                }
                return true; // No container means flat tree or already loaded
            });
        } catch (Exception e) {
            logger.debug("[Tree] Wait for children timed out, proceeding");
        }
    }
}
