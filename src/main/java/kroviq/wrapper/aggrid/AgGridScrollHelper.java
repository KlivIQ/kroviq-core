package kroviq.wrapper.aggrid;

import kroviq.wrapper.core.HandlerUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class AgGridScrollHelper {

    private AgGridScrollHelper() {}

    public static WebElement scrollToRow(WebDriver driver, WebElement gridRoot, int rowIndex) {
        int timeout = HandlerUtils.getTimeout("aggrid");
        int maxAttempts = HandlerUtils.getMaxScrollAttempts("aggrid");

        WebElement viewport = gridRoot.findElement(By.cssSelector(AgGridLocators.BODY_VIEWPORT));
        String rowSelector = AgGridLocators.rowByIndex(rowIndex);

        // Check if row is already rendered
        List<WebElement> rows = gridRoot.findElements(By.cssSelector(rowSelector));
        if (!rows.isEmpty()) return rows.get(0);

        // Scroll incrementally to find the row
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int scrollStep = viewport.getSize().getHeight();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            js.executeScript("arguments[0].scrollTop += arguments[1];", viewport, scrollStep);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
            try {
                return wait.until(ExpectedConditions.presenceOfNestedElementLocatedBy(
                        gridRoot, By.cssSelector(rowSelector)));
            } catch (Exception e) {
                // Row not yet in viewport, continue scrolling
            }
        }

        throw new org.openqa.selenium.NoSuchElementException(
                "AG Grid row at index " + rowIndex + " not found after " + maxAttempts + " scroll attempts");
    }
}
