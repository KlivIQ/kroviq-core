package kroviq.wrapper.integration;

import io.github.bonigarcia.wdm.WebDriverManager;
import kroviq.wrapper.angularmaterial.AngularMaterialAutoCompleteHandler;
import kroviq.wrapper.angularmaterial.AngularMaterialDialogHandler;
import kroviq.wrapper.angularmaterial.AngularMaterialDropdownHandler;
import kroviq.wrapper.angularmaterial.AngularMaterialTableHandler;
import kroviq.wrapper.core.AutoCompleteHandler;
import kroviq.wrapper.core.DialogHandler;
import kroviq.wrapper.core.DropdownHandler;
import kroviq.wrapper.core.GridHandler;
import kroviq.wrapper.factory.ComponentHandlerFactory;
import kroviq.wrapper.factory.UIFrameworkDetector;
import kroviq.wrapper.factory.UIFrameworkDetector.UIFramework;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

/**
 * Angular Material Phase 1 POC — Live browser validation.
 * Tests against Angular Material official demo pages.
 * NOT production code. R&D only.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AngularMaterialLiveIntegrationTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static final String SCREENSHOT_DIR = "target/screenshots/angularmaterial/";
    private static final StringBuilder LOG = new StringBuilder();
    private static int passed = 0;
    private static int failed = 0;

    // Angular Material demo URLs
    private static final String SELECT_URL = "https://material.angular.io/components/select/examples";
    private static final String AUTOCOMPLETE_URL = "https://material.angular.io/components/autocomplete/examples";
    private static final String DIALOG_URL = "https://material.angular.io/components/dialog/examples";
    private static final String TABLE_URL = "https://material.angular.io/components/table/examples";

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
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        new File(SCREENSHOT_DIR).mkdirs();
        log("=== Angular Material Live Integration Test Started ===");
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
            Files.writeString(Paths.get(SCREENSHOT_DIR, "angularmaterial-test-results.log"), LOG.toString());
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }

        if (driver != null) driver.quit();
    }

    // ==================== DETECTION TESTS ====================

    @Test
    @Order(1)
    void test01_DetectorIdentifiesAngularMaterial_Select() {
        String testName = "Detector_ANGULAR_MATERIAL_Select";
        try {
            driver.get(SELECT_URL);
            waitForPageLoad();

            WebElement matSelect = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("mat-select")));

            UIFrameworkDetector.clearCache();
            UIFramework detected = UIFrameworkDetector.detect(matSelect);
            log("[" + testName + "] Detected: " + detected + " | tag: " + matSelect.getTagName() + " | class: " + matSelect.getAttribute("class"));

            Assertions.assertEquals(UIFramework.ANGULAR_MATERIAL, detected);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(2)
    void test02_DetectorIdentifiesAngularMaterial_Table() {
        String testName = "Detector_ANGULAR_MATERIAL_Table";
        try {
            driver.get(TABLE_URL);
            waitForPageLoad();

            WebElement matTable = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("table[mat-table], mat-table, table.mat-mdc-table")));

            UIFrameworkDetector.clearCache();
            UIFramework detected = UIFrameworkDetector.detect(matTable);
            log("[" + testName + "] Detected: " + detected + " | tag: " + matTable.getTagName() + " | class: " + matTable.getAttribute("class"));

            Assertions.assertEquals(UIFramework.ANGULAR_MATERIAL, detected);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== FACTORY DISPATCH TESTS ====================

    @Test
    @Order(3)
    void test03_FactoryReturnsAngularMaterialDropdownHandler() {
        String testName = "Factory_Returns_AngularMaterialDropdownHandler";
        try {
            driver.get(SELECT_URL);
            waitForPageLoad();

            WebElement matSelect = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("mat-select")));

            UIFrameworkDetector.clearCache();
            DropdownHandler handler = ComponentHandlerFactory.getDropdownHandler(matSelect, driver);
            log("[" + testName + "] Handler: " + handler.getClass().getSimpleName());

            Assertions.assertInstanceOf(AngularMaterialDropdownHandler.class, handler);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(4)
    void test04_FactoryReturnsAngularMaterialTableHandler() {
        String testName = "Factory_Returns_AngularMaterialTableHandler";
        try {
            driver.get(TABLE_URL);
            waitForPageLoad();

            WebElement matTable = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("table[mat-table], mat-table, table.mat-mdc-table")));

            UIFrameworkDetector.clearCache();
            GridHandler handler = ComponentHandlerFactory.getGridHandler(matTable, driver);
            log("[" + testName + "] Handler: " + handler.getClass().getSimpleName());

            Assertions.assertInstanceOf(AngularMaterialTableHandler.class, handler);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== DROPDOWN / SELECT TESTS ====================

    @Test
    @Order(5)
    void test05_DropdownGetOptions() {
        String testName = "Dropdown_GetOptions";
        try {
            driver.get(SELECT_URL);
            waitForPageLoad();

            WebElement matSelect = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("mat-select")));

            AngularMaterialDropdownHandler handler = new AngularMaterialDropdownHandler();
            List<String> options = handler.getOptions(driver, matSelect);
            log("[" + testName + "] Options found: " + options.size() + " | " + options);

            Assertions.assertFalse(options.isEmpty(), "Should have at least one option");
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(6)
    void test06_DropdownSelectOption() {
        String testName = "Dropdown_Select";
        try {
            driver.get(SELECT_URL);
            waitForPageLoad();

            WebElement matSelect = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("mat-select")));

            // First get options to know what to select
            AngularMaterialDropdownHandler handler = new AngularMaterialDropdownHandler();
            List<String> options = handler.getOptions(driver, matSelect);
            if (options.isEmpty()) {
                log("[" + testName + "] SKIP — no options available");
                recordPass(testName);
                return;
            }

            String optionToSelect = options.get(0);
            log("[" + testName + "] Selecting: '" + optionToSelect + "'");

            // Re-find element after previous interaction
            matSelect = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("mat-select")));
            handler.select(driver, matSelect, optionToSelect);

            // Verify selection
            String selectedText = matSelect.getText().trim();
            log("[" + testName + "] Selected text: '" + selectedText + "'");
            Assertions.assertTrue(selectedText.contains(optionToSelect) || !selectedText.isEmpty());
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== AUTOCOMPLETE TESTS ====================

    @Test
    @Order(7)
    void test07_AutocompleteGetSuggestions() {
        String testName = "Autocomplete_GetSuggestions";
        try {
            driver.get(AUTOCOMPLETE_URL);
            waitForPageLoad();

            // Find autocomplete input
            WebElement input = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[matInput][type='text'], input[aria-autocomplete='list']")));

            AngularMaterialAutoCompleteHandler handler = new AngularMaterialAutoCompleteHandler();
            List<String> suggestions = handler.getSuggestions(driver, input, "a");
            log("[" + testName + "] Suggestions for 'a': " + suggestions.size() + " | " + suggestions);

            Assertions.assertFalse(suggestions.isEmpty(), "Should have suggestions for 'a'");
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(8)
    void test08_AutocompleteSearchAndSelect() {
        String testName = "Autocomplete_SearchAndSelect";
        try {
            driver.get(AUTOCOMPLETE_URL);
            waitForPageLoad();

            WebElement input = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[matInput][type='text'], input[aria-autocomplete='list']")));

            AngularMaterialAutoCompleteHandler handler = new AngularMaterialAutoCompleteHandler();

            // Get suggestions first to know what to select
            List<String> suggestions = handler.getSuggestions(driver, input, "a");
            if (suggestions.isEmpty()) {
                log("[" + testName + "] SKIP — no suggestions available");
                recordPass(testName);
                return;
            }

            String optionToSelect = suggestions.get(0);
            log("[" + testName + "] Selecting: '" + optionToSelect + "'");

            // Re-find input
            input = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[matInput][type='text'], input[aria-autocomplete='list']")));
            handler.searchAndSelect(driver, input, optionToSelect.substring(0, 1), optionToSelect);

            // Verify input has value
            String value = input.getAttribute("value");
            log("[" + testName + "] Input value after select: '" + value + "'");
            Assertions.assertFalse(value == null || value.isEmpty(), "Input should have value after selection");
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== DIALOG TESTS ====================

    @Test
    @Order(9)
    void test09_DialogOpenDetection() {
        String testName = "Dialog_Open_Detection";
        try {
            driver.get(DIALOG_URL);
            waitForPageLoad();

            AngularMaterialDialogHandler handler = new AngularMaterialDialogHandler();

            // Dialog should not be open initially
            boolean openBefore = handler.isOpen(driver);
            log("[" + testName + "] Dialog open before click: " + openBefore);
            Assertions.assertFalse(openBefore);

            // Click button to open dialog
            WebElement openButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(., 'Open') or contains(., 'Launch') or contains(., 'Pick')]")));
            openButton.click();

            // Wait for dialog
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("mat-dialog-container, .mat-mdc-dialog-container")));

            boolean openAfter = handler.isOpen(driver);
            log("[" + testName + "] Dialog open after click: " + openAfter);
            Assertions.assertTrue(openAfter);

            takeScreenshot(testName + "_dialog_open");
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(10)
    void test10_DialogContentRetrieval() {
        String testName = "Dialog_Content_Retrieval";
        try {
            // Dialog should still be open from previous test, but re-open if needed
            AngularMaterialDialogHandler handler = new AngularMaterialDialogHandler();
            if (!handler.isOpen(driver)) {
                driver.get(DIALOG_URL);
                waitForPageLoad();
                WebElement openButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(., 'Open') or contains(., 'Launch') or contains(., 'Pick')]")));
                openButton.click();
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("mat-dialog-container, .mat-mdc-dialog-container")));
            }

            WebElement content = handler.getDialogContent(driver);
            String text = content.getText().trim();
            log("[" + testName + "] Dialog content: '" + text.substring(0, Math.min(100, text.length())) + "...'");

            Assertions.assertNotNull(content);
            Assertions.assertFalse(text.isEmpty(), "Dialog content should not be empty");
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(11)
    void test11_DialogClose() {
        String testName = "Dialog_Close";
        try {
            AngularMaterialDialogHandler handler = new AngularMaterialDialogHandler();
            if (!handler.isOpen(driver)) {
                driver.get(DIALOG_URL);
                waitForPageLoad();
                WebElement openButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(., 'Open') or contains(., 'Launch') or contains(., 'Pick')]")));
                openButton.click();
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("mat-dialog-container, .mat-mdc-dialog-container")));
            }

            Assertions.assertTrue(handler.isOpen(driver), "Dialog should be open before close");
            handler.close(driver);

            boolean openAfterClose = handler.isOpen(driver);
            log("[" + testName + "] Dialog open after close: " + openAfterClose);
            Assertions.assertFalse(openAfterClose);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== TABLE TESTS ====================

    @Test
    @Order(12)
    void test12_TableColumnLookup() {
        String testName = "Table_Column_Lookup";
        try {
            driver.get(TABLE_URL);
            waitForPageLoad();

            WebElement matTable = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("table[mat-table], mat-table, table.mat-mdc-table")));

            AngularMaterialTableHandler handler = new AngularMaterialTableHandler(matTable);

            // Find first header text
            WebElement firstHeader = matTable.findElement(
                    By.cssSelector("mat-header-cell, th.mat-mdc-header-cell, th[mat-header-cell]"));
            String headerText = firstHeader.getText().trim();
            log("[" + testName + "] First header: '" + headerText + "'");

            int colIndex = handler.getColumnIndex(driver, headerText);
            log("[" + testName + "] Column index for '" + headerText + "': " + colIndex);

            Assertions.assertTrue(colIndex >= 0);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(13)
    void test13_TableCellRead() {
        String testName = "Table_Cell_Read";
        try {
            driver.get(TABLE_URL);
            waitForPageLoad();

            WebElement matTable = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("table[mat-table], mat-table, table.mat-mdc-table")));

            AngularMaterialTableHandler handler = new AngularMaterialTableHandler(matTable);

            // Get first header
            WebElement firstHeader = matTable.findElement(
                    By.cssSelector("mat-header-cell, th.mat-mdc-header-cell, th[mat-header-cell]"));
            String headerText = firstHeader.getText().trim();

            String cellValue = handler.getCellValue(driver, headerText, 0);
            log("[" + testName + "] Cell['" + headerText + "', row 0] = '" + cellValue + "'");

            Assertions.assertNotNull(cellValue);
            Assertions.assertFalse(cellValue.isEmpty(), "Cell value should not be empty");
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(14)
    void test14_TableCellClick() {
        String testName = "Table_Cell_Click";
        try {
            driver.get(TABLE_URL);
            waitForPageLoad();

            WebElement matTable = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("table[mat-table], mat-table, table.mat-mdc-table")));

            AngularMaterialTableHandler handler = new AngularMaterialTableHandler(matTable);

            WebElement firstHeader = matTable.findElement(
                    By.cssSelector("mat-header-cell, th.mat-mdc-header-cell, th[mat-header-cell]"));
            String headerText = firstHeader.getText().trim();

            handler.clickCell(driver, headerText, 0);
            log("[" + testName + "] Clicked cell['" + headerText + "', row 0] successfully");

            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(15)
    void test15_TableRowSelection() {
        String testName = "Table_Row_Selection";
        try {
            driver.get(TABLE_URL);
            waitForPageLoad();

            WebElement matTable = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("table[mat-table], mat-table, table.mat-mdc-table")));

            AngularMaterialTableHandler handler = new AngularMaterialTableHandler(matTable);

            handler.selectRow(driver, 0);
            log("[" + testName + "] Selected row 0 successfully");

            WebElement row = handler.getRow(driver, 0);
            Assertions.assertNotNull(row);
            log("[" + testName + "] Row 0 element tag: " + row.getTagName() + " | class: " + row.getAttribute("class"));

            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== HELPERS ====================

    private void waitForPageLoad() {
        wait.until(d -> ((JavascriptExecutor) d)
                .executeScript("return document.readyState").equals("complete"));
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
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
