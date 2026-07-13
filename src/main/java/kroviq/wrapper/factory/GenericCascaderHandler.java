package kroviq.wrapper.factory;

import kroviq.wrapper.core.CascaderHandler;
import kroviq.wrapper.core.HandlerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class GenericCascaderHandler implements CascaderHandler {

    private static final Logger logger = LogManager.getLogger(GenericCascaderHandler.class);
    private static final String PATH_SEPARATOR = "/";
    private final WebDriver driver;

    // Selectors for cascader menus (ordered by framework specificity)
    private static final String[] MENU_SELECTORS = {
            // AntD Cascader
            ".ant-cascader-menu",
            ".ant-cascader-dropdown .ant-cascader-menu",
            // PrimeNG CascadeSelect
            ".p-cascadeselect-panel .p-cascadeselect-items",
            ".p-cascadeselect-sublist",
            // Generic nested menus
            "[class*='cascader-menu']",
            "[class*='submenu']",
            "[role='menu']",
            "[role='listbox']"
    };

    private static final String[] OPTION_SELECTORS = {
            // AntD
            ".ant-cascader-menu-item",
            // PrimeNG
            ".p-cascadeselect-item",
            ".p-menuitem",
            // Generic
            "[class*='cascader-item']",
            "[class*='menu-item']",
            "[role='option']",
            "[role='menuitem']",
            "li"
    };

    public GenericCascaderHandler(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void selectPath(WebDriver driver, WebElement cascaderElement, String path) {
        String[] segments = path.split(PATH_SEPARATOR);

        // Open the cascader
        openCascader(driver, cascaderElement);

        for (int level = 0; level < segments.length; level++) {
            String segment = segments[level].trim();
            boolean isLast = (level == segments.length - 1);

            WebElement option = findOptionAtLevel(driver, segment, level);
            if (option == null) {
                throw new RuntimeException("[Cascader] Option '" + segment + "' not found at level " + (level + 1)
                        + " in path: " + path);
            }

            HandlerUtils.scrollIntoViewAndClick(driver, option);
            logger.debug("[Cascader] Selected level {}: '{}'", level + 1, segment);

            if (!isLast) {
                waitForNextLevel(driver, level + 1);
            }
        }

        logger.info("[Cascader] Path selected: {}", path);
    }

    @Override
    public String getSelectedPath(WebDriver driver, WebElement cascaderElement) {
        // Read from display label/input
        List<WebElement> labels = cascaderElement.findElements(By.cssSelector(
                ".ant-cascader-picker-label, " +
                ".p-cascadeselect-label, " +
                "input, " +
                "[class*='cascader-label'], " +
                "[class*='selected-path']"));

        for (WebElement label : labels) {
            String text = label.getText().trim();
            if (text.isEmpty()) text = label.getAttribute("value");
            if (text != null && !text.isEmpty() && !text.equals("Select")) {
                return text.replace(" / ", "/").replace(" > ", "/");
            }
        }

        // Fallback: read from aria-label or title
        String title = cascaderElement.getAttribute("title");
        if (title != null && !title.isEmpty()) return title;

        return "";
    }

    @Override
    public void clearSelection(WebDriver driver, WebElement cascaderElement) {
        // Try clear button
        List<WebElement> clearBtns = cascaderElement.findElements(By.cssSelector(
                ".ant-cascader-picker-clear, " +
                ".ant-select-clear, " +
                "[class*='clear'], " +
                "[aria-label='Clear']"));
        for (WebElement btn : clearBtns) {
            try {
                if (btn.isDisplayed()) {
                    btn.click();
                    logger.info("[Cascader] Selection cleared");
                    return;
                }
            } catch (Exception e) { /* try next */ }
        }

        // Fallback: clear input value
        List<WebElement> inputs = cascaderElement.findElements(By.cssSelector("input"));
        for (WebElement input : inputs) {
            try {
                input.clear();
                input.sendKeys(Keys.ESCAPE);
                return;
            } catch (Exception e) { /* try next */ }
        }
    }

    private void openCascader(WebDriver driver, WebElement cascaderElement) {
        // Click to open dropdown
        List<WebElement> triggers = cascaderElement.findElements(By.cssSelector(
                ".ant-cascader-input, " +
                ".ant-cascader-picker, " +
                ".p-cascadeselect-trigger, " +
                "input, " +
                "[class*='cascader-trigger']"));
        if (!triggers.isEmpty()) {
            triggers.get(0).click();
        } else {
            cascaderElement.click();
        }

        // Wait for first menu to appear
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> {
            for (String sel : MENU_SELECTORS) {
                List<WebElement> menus = d.findElements(By.cssSelector(sel));
                for (WebElement m : menus) {
                    try { if (m.isDisplayed()) return true; } catch (Exception e) { /* next */ }
                }
            }
            return false;
        });
    }

    private WebElement findOptionAtLevel(WebDriver driver, String text, int level) {
        // Find all visible menus — the nth menu corresponds to the nth level
        List<WebElement> menus = findVisibleMenus(driver);

        WebElement targetMenu;
        if (level < menus.size()) {
            targetMenu = menus.get(level);
        } else {
            // Fallback: use last visible menu
            targetMenu = menus.isEmpty() ? null : menus.get(menus.size() - 1);
        }

        if (targetMenu == null) {
            // Try page-level search
            return findOptionByText(driver.findElement(By.tagName("body")), text);
        }

        return findOptionByText(targetMenu, text);
    }

    private WebElement findOptionByText(WebElement scope, String text) {
        for (String selector : OPTION_SELECTORS) {
            List<WebElement> options = scope.findElements(By.cssSelector(selector));
            for (WebElement opt : options) {
                try {
                    String optText = opt.getText().trim();
                    if (optText.equals(text) || optText.equalsIgnoreCase(text)) return opt;
                } catch (Exception e) { /* next */ }
            }
        }
        return null;
    }

    private List<WebElement> findVisibleMenus(WebDriver driver) {
        for (String sel : MENU_SELECTORS) {
            List<WebElement> menus = driver.findElements(By.cssSelector(sel));
            List<WebElement> visible = menus.stream()
                    .filter(m -> { try { return m.isDisplayed(); } catch (Exception e) { return false; } })
                    .toList();
            if (!visible.isEmpty()) return visible;
        }
        return List.of();
    }

    private void waitForNextLevel(WebDriver driver, int expectedLevel) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> {
                List<WebElement> menus = findVisibleMenus(d);
                return menus.size() > expectedLevel;
            });
        } catch (Exception e) {
            // Some cascaders render next level within same container
            try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
    }
}
