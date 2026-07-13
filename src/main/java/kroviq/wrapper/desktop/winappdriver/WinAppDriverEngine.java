package kroviq.wrapper.desktop.winappdriver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kroviq.utils.LoadProperties;
import kroviq.wrapper.desktop.core.DesktopEngine;
import kroviq.wrapper.desktop.core.DesktopLocator;
import kroviq.wrapper.desktop.session.DesktopSessionConfig;
import kroviq.wrapper.desktop.wait.DesktopWaitHandler;
import kroviq.wrapper.desktop.wait.DesktopWaitHandler.WaitTier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WinAppDriverEngine implements DesktopEngine {

    private static final Logger logger = LogManager.getLogger(WinAppDriverEngine.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private RemoteWebDriver driver;
    private URL serverUrl;

    @Override
    public void launchApplication(DesktopSessionConfig config) {
        try {
            String winAppDriverUrl = LoadProperties.get("desktop.winappdriver.url", "http://127.0.0.1:4723");
            serverUrl = new URL(winAppDriverUrl);
            Map<String, Object> caps = new HashMap<>();
            caps.put("app", config.getApplicationPath());
            caps.put("platformName", "Windows");
            caps.put("deviceName", "WindowsPC");
            if (config.getApplicationArguments() != null && !config.getApplicationArguments().isEmpty()) {
                caps.put("appArguments", config.getApplicationArguments());
            }
            driver = createWinAppDriverSession(caps);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
            DesktopWaitHandler.waitForIdle(config.getLaunchTimeoutSeconds() > 0 ? 2000 : 1000);
            logger.info("Application launched: {}", config.getApplicationPath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to launch desktop application: " + e.getMessage(), e);
        }
    }

    @Override
    public void attachToApplication(String windowTitle) {
        try {
            String winAppDriverUrl = LoadProperties.get("desktop.winappdriver.url", "http://127.0.0.1:4723");
            serverUrl = new URL(winAppDriverUrl);
            Map<String, Object> rootCaps = new HashMap<>();
            rootCaps.put("app", "Root");
            rootCaps.put("platformName", "Windows");
            rootCaps.put("deviceName", "WindowsPC");
            RemoteWebDriver rootSession = createWinAppDriverSession(rootCaps);
            WebElement appWindow = rootSession.findElement(By.name(windowTitle));
            String windowHandle = appWindow.getAttribute("NativeWindowHandle");
            int handleInt = Integer.parseInt(windowHandle);
            String handleHex = Integer.toHexString(handleInt);
            Map<String, Object> appCaps = new HashMap<>();
            appCaps.put("appTopLevelWindow", handleHex);
            appCaps.put("platformName", "Windows");
            appCaps.put("deviceName", "WindowsPC");
            driver = createWinAppDriverSession(appCaps);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
            rootSession.quit();
            logger.info("Attached to application window: {}", windowTitle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to attach to application '" + windowTitle + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void closeApplication() {
        if (driver != null) {
            try {
                driver.quit();
                logger.info("Desktop application closed");
            } catch (Exception e) {
                logger.warn("Error closing desktop application: {}", e.getMessage());
            } finally {
                driver = null;
            }
        }
    }

    @Override
    public void click(DesktopLocator locator) {
        WebElement element = findElement(locator, WaitTier.SHORT);
        element.click();
        logger.debug("Clicked: {}", locator);
    }

    @Override
    public void doubleClick(DesktopLocator locator) {
        WebElement element = findElement(locator, WaitTier.SHORT);
        new org.openqa.selenium.interactions.Actions(driver).doubleClick(element).perform();
        logger.debug("Double-clicked: {}", locator);
    }

    @Override
    public void type(DesktopLocator locator, String text) {
        WebElement element = findElement(locator, WaitTier.SHORT);
        element.sendKeys(text);
        logger.debug("Typed '{}' into: {}", text, locator);
    }

    @Override
    public void clear(DesktopLocator locator) {
        WebElement element = findElement(locator, WaitTier.SHORT);
        element.clear();
        logger.debug("Cleared: {}", locator);
    }

    @Override
    public String getText(DesktopLocator locator) {
        WebElement element = findElement(locator, WaitTier.SHORT);
        String text = element.getText();
        if (text == null || text.isEmpty()) {
            text = element.getAttribute("Name");
        }
        if (text == null || text.isEmpty()) {
            text = element.getAttribute("Value.Value");
        }
        logger.debug("Got text '{}' from: {}", text, locator);
        return text != null ? text : "";
    }

    @Override
    public boolean isVisible(DesktopLocator locator) {
        try {
            List<WebElement> elements = findElements(locator);
            return !elements.isEmpty() && elements.get(0).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isEnabled(DesktopLocator locator) {
        try {
            WebElement element = findElement(locator, WaitTier.SHORT);
            return element.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void selectDropdownValue(DesktopLocator locator, String value) {
        WebElement element = findElement(locator, WaitTier.MEDIUM);
        element.click();
        DesktopWaitHandler.waitForIdle(500);
        try {
            WebElement option = driver.findElement(By.name(value));
            option.click();
        } catch (NoSuchElementException e) {
            element.sendKeys(value);
            element.sendKeys(Keys.ENTER);
        }
        logger.debug("Selected '{}' from dropdown: {}", value, locator);
    }

    @Override
    public void setCheckbox(DesktopLocator locator, boolean checked) {
        WebElement element = findElement(locator, WaitTier.SHORT);
        boolean currentState = element.isSelected();
        if (currentState != checked) {
            element.click();
        }
        logger.debug("Set checkbox to {} for: {}", checked, locator);
    }

    @Override
    public boolean isChecked(DesktopLocator locator) {
        WebElement element = findElement(locator, WaitTier.SHORT);
        return element.isSelected();
    }

    @Override
    public String getCurrentWindowTitle() {
        return driver.getTitle();
    }

    @Override
    public void switchToWindow(String title) {
        try {
            Map<String, Object> rootCaps = new HashMap<>();
            rootCaps.put("app", "Root");
            rootCaps.put("platformName", "Windows");
            rootCaps.put("deviceName", "WindowsPC");
            RemoteWebDriver rootSession = createWinAppDriverSession(rootCaps);
            WebElement targetWindow = rootSession.findElement(By.name(title));
            String windowHandle = targetWindow.getAttribute("NativeWindowHandle");
            int handleInt = Integer.parseInt(windowHandle);
            String handleHex = Integer.toHexString(handleInt);
            Map<String, Object> appCaps = new HashMap<>();
            appCaps.put("appTopLevelWindow", handleHex);
            appCaps.put("platformName", "Windows");
            appCaps.put("deviceName", "WindowsPC");
            if (driver != null) driver.quit();
            driver = createWinAppDriverSession(appCaps);
            rootSession.quit();
            logger.info("Switched to window: {}", title);
        } catch (Exception e) {
            throw new RuntimeException("Failed to switch to window '" + title + "': " + e.getMessage(), e);
        }
    }

    public RemoteWebDriver getDriver() {
        return driver;
    }

    public WebElement findElement(DesktopLocator locator, WaitTier tier) {
        return DesktopWaitHandler.waitForElement(driver, d -> resolveElement(d, locator), tier);
    }

    private List<WebElement> findElements(DesktopLocator locator) {
        By by = toBy(locator);
        return driver.findElements(by);
    }

    private WebElement resolveElement(WebDriver d, DesktopLocator locator) {
        By by = toBy(locator);
        List<WebElement> elements = d.findElements(by);
        return elements.isEmpty() ? null : elements.get(0);
    }

    private By toBy(DesktopLocator locator) {
        // WinAppDriver doesn't support CSS selectors — Selenium 4 W3C codec converts By.id() to CSS.
        // Use XPath for all strategies to ensure WinAppDriver compatibility.
        return switch (locator.strategy()) {
            case "automationId" -> By.xpath("//*[@AutomationId='" + locator.value() + "']");
            case "name" -> By.name(locator.value());
            case "className" -> By.className(locator.value());
            case "xpath" -> By.xpath(locator.value());
            default -> throw new IllegalArgumentException("Unsupported locator strategy: " + locator.strategy());
        };
    }

    private RemoteWebDriver createWinAppDriverSession(Map<String, Object> capabilities) {
        try {
            // Step 1: Create session via raw HTTP (bypasses Selenium 4 W3C validation)
            String sessionId = createSessionViaHttp(capabilities);
            // Step 2: Create HttpCommandExecutor pointing to WinAppDriver
            HttpCommandExecutor executor = new HttpCommandExecutor(serverUrl);
            // Step 3: Create RemoteWebDriver subclass that skips startSession()
            RemoteWebDriver wd = new WinAppDriverRemoteDriver(executor, sessionId);
            logger.debug("WinAppDriver session created: {}", sessionId);
            return wd;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create WinAppDriver session: " + e.getMessage(), e);
        }
    }

    private String createSessionViaHttp(Map<String, Object> capabilities) throws IOException {
        URL sessionUrl = new URL(serverUrl, "/session");
        HttpURLConnection conn = (HttpURLConnection) sessionUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        String payload = objectMapper.writeValueAsString(Map.of("desiredCapabilities", capabilities));
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
        int responseCode = conn.getResponseCode();
        String responseBody;
        try (var is = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
            responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (responseCode >= 400) {
            throw new RuntimeException("WinAppDriver session creation failed (HTTP " + responseCode + "): " + responseBody);
        }
        JsonNode responseJson = objectMapper.readTree(responseBody);
        String sessionId = responseJson.has("sessionId")
                ? responseJson.get("sessionId").asText()
                : responseJson.path("value").path("sessionId").asText();
        if (sessionId == null || sessionId.isEmpty()) {
            throw new RuntimeException("WinAppDriver did not return a session ID. Response: " + responseBody);
        }
        return sessionId;
    }

    /**
     * Custom RemoteWebDriver that attaches to an existing WinAppDriver session
     * without triggering Selenium 4's W3C capability validation.
     */
    private static class WinAppDriverRemoteDriver extends RemoteWebDriver {

        WinAppDriverRemoteDriver(CommandExecutor executor, String sessionId) {
            super(executor, new ImmutableCapabilities());
            // Override the session ID with the one from WinAppDriver
            super.setSessionId(sessionId);
            // Initialize the command codec (normally done in startSession)
            initializeCodec(executor);
        }

        @Override
        protected void startSession(Capabilities capabilities) {
            // No-op: session already created via raw HTTP
        }

        private void initializeCodec(CommandExecutor executor) {
            if (executor instanceof HttpCommandExecutor httpExecutor) {
                try {
                    // In Selenium 4.25.0, HttpCommandExecutor stores codec in commandCodec/responseCodec fields
                    var commandCodecField = HttpCommandExecutor.class.getDeclaredField("commandCodec");
                    commandCodecField.setAccessible(true);
                    commandCodecField.set(httpExecutor, new org.openqa.selenium.remote.codec.w3c.W3CHttpCommandCodec());
                    var responseCodecField = HttpCommandExecutor.class.getDeclaredField("responseCodec");
                    responseCodecField.setAccessible(true);
                    responseCodecField.set(httpExecutor, new org.openqa.selenium.remote.codec.w3c.W3CHttpResponseCodec());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize command codec: " + e.getMessage(), e);
                }
            }
        }
    }
}
