package kroviq.wrapper.integration;

import io.github.bonigarcia.wdm.WebDriverManager;
import kroviq.wrapper.core.AutoCompleteHandler;
import kroviq.wrapper.core.DialogHandler;
import kroviq.wrapper.core.DropdownHandler;
import kroviq.wrapper.factory.ComponentHandlerFactory;
import kroviq.wrapper.factory.UIFrameworkDetector;
import kroviq.wrapper.factory.UIFrameworkDetector.UIFramework;
import kroviq.wrapper.mui.MUIAutoCompleteHandler;
import kroviq.wrapper.mui.MUIDialogHandler;
import kroviq.wrapper.mui.MUIDropdownHandler;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

/**
 * POC Integration Tests — validates MUI handlers against live MUI demo pages.
 * NOT production code. R&D only.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MUILiveIntegrationTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static final String SCREENSHOT_DIR = "target/screenshots/";
    private static final StringBuilder LOG = new StringBuilder();
    private static int passed = 0;
    private static int failed = 0;

    @BeforeAll
    static void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        new File(SCREENSHOT_DIR).mkdirs();
        log("=== MUI Live Integration Test Started ===");
    }

    @AfterAll
    static void tearDown() {
        log("\n=== SUMMARY ===");
        log("Passed: " + passed);
        log("Failed: " + failed);
        log("Total:  " + (passed + failed));
        log("===============");
        System.out.println(LOG);

        try {
            Files.writeString(Paths.get(SCREENSHOT_DIR, "test-results.log"), LOG.toString());
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }

        if (driver != null) driver.quit();
    }

    // ==================== DROPDOWN TESTS ====================

    @Test
    @Order(1)
    void test01_DetectorIdentifiesMUIOnSelectPage() {
        String testName = "Detector_MUI_Select_Page";
        try {
            driver.get("https://mui.com/material-ui/react-select/");
            waitForPageLoad();

            WebElement muiElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("[class*='MuiSelect-select']")));

            UIFrameworkDetector.clearCache();
            UIFramework detected = UIFrameworkDetector.detect(muiElement);
            log("[" + testName + "] Detected: " + detected + " | class: " + muiElement.getAttribute("class"));

            Assertions.assertEquals(UIFramework.MUI, detected);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(2)
    void test02_FactoryReturnsMUIDropdownHandler() {
        String testName = "Factory_Returns_MUIDropdownHandler";
        try {
            driver.get("https://mui.com/material-ui/react-select/");
            waitForPageLoad();

            WebElement muiElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("[class*='MuiSelect-select']")));

            UIFrameworkDetector.clearCache();
            DropdownHandler handler = ComponentHandlerFactory.getDropdownHandler(muiElement, driver);
            log("[" + testName + "] Handler: " + handler.getClass().getSimpleName());

            Assertions.assertInstanceOf(MUIDropdownHandler.class, handler);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(3)
    void test03_MUIDropdownSelectOption() {
        String testName = "MUI_Dropdown_Select_Option";
        try {
            driver.get("https://mui.com/material-ui/react-select/");
            waitForPageLoad();

            // Find the first MUI Select trigger (the div with role=combobox)
            WebElement selectTrigger = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("[role='combobox'][class*='MuiSelect']")));
            log("[" + testName + "] Trigger class: " + selectTrigger.getAttribute("class"));
            log("[" + testName + "] Trigger text before: '" + selectTrigger.getText() + "'");

            // Use our handler
            MUIDropdownHandler handler = new MUIDropdownHandler();
            handler.select(driver, selectTrigger, "Twenty");

            // Verify selection
            // Re-find the element as it may have been re-rendered
            selectTrigger = driver.findElement(By.cssSelector("[role='combobox'][class*='MuiSelect']"));
            String selectedValue = selectTrigger.getText().trim();
            log("[" + testName + "] After selection: '" + selectedValue + "'");

            Assertions.assertEquals("Twenty", selectedValue);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(4)
    void test04_MUIDropdownGetOptions() {
        String testName = "MUI_Dropdown_Get_Options";
        try {
            driver.get("https://mui.com/material-ui/react-select/");
            waitForPageLoad();

            WebElement selectTrigger = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("[role='combobox'][class*='MuiSelect']")));

            MUIDropdownHandler handler = new MUIDropdownHandler();
            List<String> options = handler.getOptions(driver, selectTrigger);
            log("[" + testName + "] Options: " + options);

            Assertions.assertFalse(options.isEmpty(), "Options list should not be empty");
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== AUTOCOMPLETE TESTS ====================

    @Test
    @Order(5)
    void test05_DetectorIdentifiesMUIOnAutocompletePage() {
        String testName = "Detector_MUI_Autocomplete_Page";
        try {
            driver.get("https://mui.com/material-ui/react-autocomplete/");
            waitForPageLoad();

            WebElement muiElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("[class*='MuiAutocomplete-root']")));

            UIFrameworkDetector.clearCache();
            UIFramework detected = UIFrameworkDetector.detect(muiElement);
            log("[" + testName + "] Detected: " + detected);

            Assertions.assertEquals(UIFramework.MUI, detected);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(6)
    void test06_FactoryReturnsMUIAutoCompleteHandler() {
        String testName = "Factory_Returns_MUIAutoCompleteHandler";
        try {
            driver.get("https://mui.com/material-ui/react-autocomplete/");
            waitForPageLoad();

            WebElement muiElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("[class*='MuiAutocomplete-root']")));

            UIFrameworkDetector.clearCache();
            AutoCompleteHandler handler = ComponentHandlerFactory.getAutoCompleteHandler(muiElement);
            log("[" + testName + "] Handler: " + handler.getClass().getSimpleName());

            Assertions.assertInstanceOf(MUIAutoCompleteHandler.class, handler);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(7)
    void test07_MUIAutocompleteSearchAndSelect() {
        String testName = "MUI_Autocomplete_Search_Select";
        try {
            driver.get("https://mui.com/material-ui/react-autocomplete/");
            waitForPageLoad();

            // Find the first autocomplete input directly on the page
            WebElement input = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[role='combobox']")));
            log("[" + testName + "] Input placeholder: " + input.getAttribute("placeholder"));

            // Use our handler
            MUIAutoCompleteHandler handler = new MUIAutoCompleteHandler();
            handler.searchAndSelect(driver, input, "The God", "The Godfather");

            // Verify selection
            String inputValue = input.getAttribute("value");
            log("[" + testName + "] After selection, value: '" + inputValue + "'");

            Assertions.assertTrue(inputValue.contains("Godfather"),
                    "Expected 'Godfather' in value, got: " + inputValue);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(8)
    void test08_MUIAutocompleteGetSuggestions() {
        String testName = "MUI_Autocomplete_Get_Suggestions";
        try {
            driver.get("https://mui.com/material-ui/react-autocomplete/");
            waitForPageLoad();

            WebElement input = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[role='combobox']")));

            MUIAutoCompleteHandler handler = new MUIAutoCompleteHandler();
            List<String> suggestions = handler.getSuggestions(driver, input, "Star");
            log("[" + testName + "] Suggestions for 'Star': " + suggestions);

            Assertions.assertFalse(suggestions.isEmpty(), "Suggestions should not be empty");
            boolean hasStarWars = suggestions.stream().anyMatch(s -> s.contains("Star Wars"));
            Assertions.assertTrue(hasStarWars, "Expected Star Wars in suggestions: " + suggestions);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== DIALOG TESTS ====================

    @Test
    @Order(9)
    void test09_DetectorIdentifiesMUIOnDialogPage() {
        String testName = "Detector_MUI_Dialog_Page";
        try {
            driver.get("https://mui.com/material-ui/react-dialog/");
            waitForPageLoad();

            // Dialog page has MUI buttons directly
            WebElement muiElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("[class*='MuiButton']")));

            UIFrameworkDetector.clearCache();
            UIFramework detected = UIFrameworkDetector.detect(muiElement);
            log("[" + testName + "] Detected: " + detected);

            Assertions.assertEquals(UIFramework.MUI, detected);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(10)
    void test10_MUIDialogOpenAndDetect() {
        String testName = "MUI_Dialog_Open_Detect";
        try {
            driver.get("https://mui.com/material-ui/react-dialog/#alerts");
            waitForPageLoad();

            WebElement openButton = findAndScrollToAlertDialogButton();
            log("[" + testName + "] Found button: '" + openButton.getText() + "'");
            jsClick(openButton);

            // Wait for dialog
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("[role='dialog'], .MuiDialog-root")));

            MUIDialogHandler handler = new MUIDialogHandler();
            boolean isOpen = handler.isOpen(driver);
            log("[" + testName + "] Dialog isOpen: " + isOpen);

            Assertions.assertTrue(isOpen, "Dialog should be open");

            WebElement content = handler.getDialogContent(driver);
            String contentText = content.getText();
            log("[" + testName + "] Content: " + contentText.substring(0, Math.min(80, contentText.length())));
            Assertions.assertFalse(contentText.isEmpty());

            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(11)
    void test11_MUIDialogClickAction() {
        String testName = "MUI_Dialog_Click_Action";
        try {
            driver.get("https://mui.com/material-ui/react-dialog/#alerts");
            waitForPageLoad();

            WebElement openButton = findAndScrollToAlertDialogButton();
            jsClick(openButton);

            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".MuiDialog-root.MuiModal-root:not(.MuiModal-hidden)")));
            Thread.sleep(500);

            MUIDialogHandler handler = new MUIDialogHandler();
            Assertions.assertTrue(handler.isOpen(driver), "Dialog should be open");

            // Click "Agree" button in dialog
            handler.clickAction(driver, "Agree");
            log("[" + testName + "] Clicked 'Agree'");

            // Wait for dialog to close (MUI demo's Agree calls handleClose)
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> !handler.isOpen(d));

            Assertions.assertFalse(handler.isOpen(driver), "Dialog should be closed");
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(12)
    void test12_MUIDialogClose() {
        String testName = "MUI_Dialog_Close_Escape";
        try {
            driver.get("https://mui.com/material-ui/react-dialog/#alerts");
            waitForPageLoad();

            WebElement openButton = findAndScrollToAlertDialogButton();
            jsClick(openButton);

            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".MuiDialog-root.MuiModal-root:not(.MuiModal-hidden)")));
            Thread.sleep(500);

            MUIDialogHandler handler = new MUIDialogHandler();
            Assertions.assertTrue(handler.isOpen(driver), "Dialog should be open");

            // Close via handler (uses close button or ESC)
            handler.close(driver);
            log("[" + testName + "] Called close()");

            Assertions.assertFalse(handler.isOpen(driver), "Dialog should be closed");
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== HELPERS ====================

    private WebElement findAndScrollToAlertDialogButton() throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[contains(translate(text(),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OPEN ALERT DIALOG')]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(500);
        return btn;
    }

    private void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    private void waitForPageLoad() {
        wait.until(d -> ((JavascriptExecutor) d)
                .executeScript("return document.readyState").equals("complete"));
        // Wait for React hydration on MUI docs
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
    }

    private void recordPass(String testName) {
        passed++;
        log("[PASS] " + testName);
        takeScreenshot(testName + "_PASS");
    }

    private void recordFail(String testName, Exception e) {
        failed++;
        log("[FAIL] " + testName + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
        takeScreenshot(testName + "_FAIL");
        Assertions.fail(testName + " failed: " + e.getMessage());
    }

    private void takeScreenshot(String name) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path dest = Paths.get(SCREENSHOT_DIR, name + ".png");
            Files.copy(src.toPath(), dest);
        } catch (Exception e) {
            log("[Screenshot] Failed: " + e.getMessage());
        }
    }

    private static void log(String message) {
        String line = "[" + java.time.LocalTime.now().toString().substring(0, 8) + "] " + message;
        LOG.append(line).append("\n");
        System.out.println(line);
    }
}
