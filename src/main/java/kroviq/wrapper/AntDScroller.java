package kroviq.wrapper;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.List;

public class AntDScroller {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Actions actions;

    public AntDScroller(WebDriver driver, int timeoutSec) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSec));
        this.actions = new Actions(driver);
    }

    /** Select an AntD option by visible text, scrolling the dropdown until found. */
    public void selectWithScroll(By selectRoot, String visibleText) {
        // 1) Open the select
        WebElement root = wait.until(ExpectedConditions.visibilityOfElementLocated(selectRoot));
        WebElement trigger = root.findElement(By.cssSelector(".ant-select-selector"));
        trigger.click();

        // 2) Get the active panel (AntD renders under <body>)
        WebElement panel = getActivePanel();

        // 3) If searchable, type to filter (fast path)
        typeIfSearchable(panel, visibleText);

        // 4) Try to find the option (exact, then contains)
        By exact = By.xpath(".//*[@role='option' or contains(@class,'ant-select-item-option')]"
                + "[.//span[normalize-space(.)=" + x(visibleText) + "]]");
        By contains = By.xpath(".//*[@role='option' or contains(@class,'ant-select-item-option')]"
                + "[.//span[contains(normalize-space(.)," + x(visibleText) + ")]]");

        WebElement row = findWithScroll(panel, exact, contains, 16, 260); // ~16 page steps
        if (row == null) throw new NoSuchElementException("AntD option not found: " + visibleText);

        // 5) Click the row (or its checkbox/content)
        WebElement clickTarget = firstDisplayed(row,
                By.cssSelector(".custom-checkbox, .checkboxComponent, .ant-select-item-option-content"));
        if (clickTarget == null) clickTarget = row;

        scrollIntoView(clickTarget);
        safeClick(clickTarget);

        // 6) Close (multi-select may stay open)
        try { wait.until(ExpectedConditions.invisibilityOf(panel)); }
        catch (TimeoutException ignored) { actions.sendKeys(Keys.ESCAPE).perform(); }
    }

    /* ---------- internals ---------- */

    private WebElement getActivePanel() {
        wait.until(d -> !d.findElements(By.cssSelector(".ant-select-dropdown, .ant-dropdown")).isEmpty());
        List<WebElement> panels = driver.findElements(By.cssSelector(".ant-select-dropdown, .ant-dropdown"));
        for (int i = panels.size() - 1; i >= 0; i--) if (panels.get(i).isDisplayed()) return panels.get(i);
        return wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".ant-select-dropdown, .ant-dropdown")));
    }

    private void typeIfSearchable(WebElement panel, String q) {
        List<WebElement> ins = panel.findElements(By.cssSelector("input.ant-select-selection-search-input"));
        for (WebElement in : ins) {
            if (in.isDisplayed()) { in.clear(); in.sendKeys(q); sleep(250); return; }
        }
    }

    /** Scroll the virtual list with PAGE_DOWN/JS until one of the locators is found. */
    private WebElement findWithScroll(WebElement panel, By exact, By contains, int maxPages, int pauseMs) {
        // try without scrolling
        WebElement found = first(panel, exact, contains);
        if (found != null) return found;

        WebElement scroller = firstDisplayed(panel,
                By.cssSelector(".rc-virtual-list-holder, .rc-virtual-list, [role='listbox'], .ant-select-dropdown"));
        for (int i = 0; i < maxPages && found == null; i++) {
            // send PAGE_DOWN (keyboard) + JS scroll as backup
            try { actions.moveToElement(scroller).sendKeys(Keys.PAGE_DOWN).perform(); } catch (Exception ignored) {}
            try { ((JavascriptExecutor)driver).executeScript("arguments[0].scrollTop = arguments[0].scrollTop + 400;", scroller); } catch (Exception ignored) {}
            sleep(pauseMs);
            found = first(panel, exact, contains);
        }
        return found;
    }

    private WebElement first(WebElement scope, By... locators) {
        for (By by : locators) {
            List<WebElement> els = scope.findElements(by);
            for (WebElement e : els) if (e.isDisplayed()) return e;
        }
        return null;
    }

    private WebElement firstDisplayed(WebElement scope, By by) {
        for (WebElement e : scope.findElements(by)) if (e.isDisplayed()) return e;
        return scope;
    }

    private void scrollIntoView(WebElement el) {
        ((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }

    private void safeClick(WebElement el) {
        try { el.click(); }
        catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor)driver).executeScript("arguments[0].click();", el);
        }
    }

    private static String x(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        if (!s.contains("\"")) return "\"" + s + "\"";
        String[] p = s.split("'");
        StringBuilder b = new StringBuilder("concat(");
        for (int i = 0; i < p.length; i++) { b.append("'").append(p[i]).append("'"); if (i < p.length - 1) b.append(", \"'\", "); }
        b.append(")");
        return b.toString();
    }

    private void sleep(long ms) {
        try {
            new WebDriverWait(driver, Duration.ofMillis(ms)).until(d -> true);
        } catch (Exception ignored) {
            // Timeout is expected for delay
        }
    }
}