package kroviq.wrapper;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;
import java.util.List;

public class ToastWaiter {
    private final WebDriver driver;
    private final Duration poll = Duration.ofMillis(150);

    public ToastWaiter(WebDriver driver) { this.driver = driver; }

    public String waitForToastText(long maxWaitSeconds) {
        Wait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(maxWaitSeconds))
                .pollingEvery(poll)
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);

        return wait.until(d -> {
            WebElement el = firstVisible(d, By.cssSelector(
                // AntD Message
                "div.ant-message div.ant-message-notice div.ant-message-custom-content, " +
                "div.ant-message div.ant-message-notice, " +
                // AntD Notification
                "div.ant-notification div.ant-notification-notice-message, " +
                "div.ant-notification div.ant-notification-notice, " +
                // React-Toastify
                "div.Toastify__toast-body, div.Toastify__toast, " +
                // MUI Snackbar / Alert
                "div.MuiSnackbar-root .MuiAlert-message, " +
                "div.MuiSnackbar-root .MuiSnackbarContent-message, " +
                "div.MuiSnackbar-root, " +
                "div.MuiAlert-root .MuiAlert-message, " +
                // Angular Material MatSnackBar
                ".mat-mdc-snack-bar-container .mdc-snackbar__label, " +
                "mat-snack-bar-container .mat-mdc-snack-bar-label, " +
                ".mat-mdc-snack-bar-container, " +
                // PrimeNG Toast
                ".p-toast-message .p-toast-message-text .p-toast-detail, " +
                ".p-toast-message .p-toast-message-text .p-toast-summary, " +
                ".p-toast-message, " +
                // PrimeNG Message / Inline Message
                ".p-message .p-message-text, " +
                ".p-inline-message .p-inline-message-text, " +
                // Element Plus Message / Notification
                ".el-message .el-message__content, " +
                ".el-notification .el-notification__content, " +
                ".el-message, .el-notification, " +
                // Generic ARIA alert
                "[role='alert']"
            ));
            return el != null ? el.getText().trim() : null;
        });
    }

    public void waitToastToDisappear(long seconds) {
        WebElement el = firstVisible(driver, By.cssSelector(
            "div.ant-message div.ant-message-notice, " +
            "div.ant-notification div.ant-notification-notice, " +
            "div.Toastify__toast, " +
            "div.MuiSnackbar-root, " +
            ".mat-mdc-snack-bar-container, mat-snack-bar-container, " +
            ".p-toast-message, " +
            ".el-message, .el-notification, " +
            "[role='alert']"
        ));
        if (el == null) return;
        new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .pollingEvery(poll)
                .ignoring(StaleElementReferenceException.class)
                .until(ExpectedConditions.invisibilityOf(el));
    }

    private WebElement firstVisible(WebDriver d, By by) {
        List<WebElement> list = d.findElements(by);
        for (WebElement el : list) {
            try {
                if (el.isDisplayed() && el.getSize().height > 0 && el.getSize().width > 0) return el;
            } catch (StaleElementReferenceException ignore) {}
        }
        return null;
    }
}