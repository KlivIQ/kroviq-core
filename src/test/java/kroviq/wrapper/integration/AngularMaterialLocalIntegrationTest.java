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
 * Angular Material Phase 1 POC — Local fixture validation.
 * Uses local HTML file simulating Angular Material DOM structure.
 * Eliminates network dependency for reliable validation.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AngularMaterialLocalIntegrationTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static final String SCREENSHOT_DIR = "target/screenshots/angularmaterial-local/";
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
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        new File(SCREENSHOT_DIR).mkdirs();
        log("=== Angular Material Local Integration Test Started ===");

        // Load local fixture
        String fixturePath = Paths.get("src/test/resources/angularmaterial/angular-material-fixture.html")
                .toAbsolutePath().toUri().toString();
        driver.get(fixturePath);
        log("Loaded fixture: " + fixturePath);
    }

    @AfterAll
    static void tearDown() {
        log("\n=== SUMMARY ===");
        log("Passed: " + passed);
        log("Failed: " + failed);
        log("Total:  " + (passed + failed));
        log("Result: " + (failed == 0 ? "ALL PASS ✓" : "FAILURES DETECTED ✗"));
        log("===============");
        System.out.println(LOG);

        try {
            Files.writeString(Paths.get(SCREENSHOT_DIR, "angularmaterial-local-results.log"), LOG.toString());
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }

        if (driver != null) driver.quit();
    }

    // ==================== DETECTION TESTS ====================

    @Test
    @Order(1)
    void test01_DetectorIdentifiesMatSelect() {
        String testName = "Detector_mat-select";
        try {
            WebElement matSelect = driver.findElement(By.cssSelector("mat-select"));
            UIFrameworkDetector.clearCache();
            UIFramework detected = UIFrameworkDetector.detect(matSelect);
            log("[" + testName + "] tag: " + matSelect.getTagName() + " | class: " + matSelect.getAttribute("class") + " | detected: " + detected);

            Assertions.assertEquals(UIFramework.ANGULAR_MATERIAL, detected);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(2)
    void test02_DetectorIdentifiesMatTable() {
        String testName = "Detector_mat-table";
        try {
            WebElement matTable = driver.findElement(By.cssSelector("table.mat-mdc-table"));
            UIFrameworkDetector.clearCache();
            UIFramework detected = UIFrameworkDetector.detect(matTable);
            log("[" + testName + "] tag: " + matTable.getTagName() + " | class: " + matTable.getAttribute("class") + " | detected: " + detected);

            Assertions.assertEquals(UIFramework.ANGULAR_MATERIAL, detected);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(3)
    void test03_DetectorIdentifiesMatAutocompleteInput() {
        String testName = "Detector_matAutocomplete_input";
        try {
            WebElement input = driver.findElement(By.cssSelector("input[matAutocomplete]"));
            UIFrameworkDetector.clearCache();
            UIFramework detected = UIFrameworkDetector.detect(input);
            log("[" + testName + "] tag: " + input.getTagName() + " | matAutocomplete attr: " + input.getAttribute("matAutocomplete") + " | detected: " + detected);

            Assertions.assertEquals(UIFramework.ANGULAR_MATERIAL, detected);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== FACTORY DISPATCH TESTS ====================

    @Test
    @Order(4)
    void test04_FactoryReturnsDropdownHandler() {
        String testName = "Factory_DropdownHandler";
        try {
            WebElement matSelect = driver.findElement(By.cssSelector("mat-select"));
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
    @Order(5)
    void test05_FactoryReturnsAutoCompleteHandler() {
        String testName = "Factory_AutoCompleteHandler";
        try {
            WebElement input = driver.findElement(By.cssSelector("input[matAutocomplete]"));
            UIFrameworkDetector.clearCache();
            AutoCompleteHandler handler = ComponentHandlerFactory.getAutoCompleteHandler(input);
            log("[" + testName + "] Handler: " + handler.getClass().getSimpleName());

            Assertions.assertInstanceOf(AngularMaterialAutoCompleteHandler.class, handler);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(6)
    void test06_FactoryReturnsTableHandler() {
        String testName = "Factory_TableHandler";
        try {
            WebElement matTable = driver.findElement(By.cssSelector("table.mat-mdc-table"));
            UIFrameworkDetector.clearCache();
            GridHandler handler = ComponentHandlerFactory.getGridHandler(matTable, driver);
            log("[" + testName + "] Handler: " + handler.getClass().getSimpleName());

            Assertions.assertInstanceOf(AngularMaterialTableHandler.class, handler);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== DROPDOWN TESTS ====================

    @Test
    @Order(7)
    void test07_DropdownGetOptions() {
        String testName = "Dropdown_GetOptions";
        try {
            WebElement matSelect = driver.findElement(By.cssSelector("mat-select"));
            AngularMaterialDropdownHandler handler = new AngularMaterialDropdownHandler();
            List<String> options = handler.getOptions(driver, matSelect);
            log("[" + testName + "] Options: " + options);

            Assertions.assertEquals(5, options.size());
            Assertions.assertTrue(options.contains("Apple"));
            Assertions.assertTrue(options.contains("Cherry"));
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(8)
    void test08_DropdownSelectOption() {
        String testName = "Dropdown_Select";
        try {
            WebElement matSelect = driver.findElement(By.cssSelector("mat-select"));
            AngularMaterialDropdownHandler handler = new AngularMaterialDropdownHandler();
            handler.select(driver, matSelect, "Banana");

            String displayedValue = driver.findElement(By.id("select-value")).getText().trim();
            log("[" + testName + "] Selected: 'Banana' | Displayed: '" + displayedValue + "'");

            Assertions.assertEquals("Banana", displayedValue);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== AUTOCOMPLETE TESTS ====================

    @Test
    @Order(9)
    void test09_AutocompleteGetSuggestions() {
        String testName = "Autocomplete_GetSuggestions";
        try {
            WebElement input = driver.findElement(By.id("test-autocomplete"));
            AngularMaterialAutoCompleteHandler handler = new AngularMaterialAutoCompleteHandler();
            List<String> suggestions = handler.getSuggestions(driver, input, "Cal");
            log("[" + testName + "] Suggestions for 'Cal': " + suggestions);

            Assertions.assertFalse(suggestions.isEmpty());
            Assertions.assertTrue(suggestions.contains("California"));
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(10)
    void test10_AutocompleteSearchAndSelect() {
        String testName = "Autocomplete_SearchAndSelect";
        try {
            WebElement input = driver.findElement(By.id("test-autocomplete"));
            AngularMaterialAutoCompleteHandler handler = new AngularMaterialAutoCompleteHandler();
            handler.searchAndSelect(driver, input, "Ala", "Alaska");

            String value = input.getAttribute("value");
            log("[" + testName + "] Input value after select: '" + value + "'");

            Assertions.assertEquals("Alaska", value);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== DIALOG TESTS ====================

    @Test
    @Order(11)
    void test11_DialogNotOpenInitially() {
        String testName = "Dialog_Not_Open_Initially";
        try {
            AngularMaterialDialogHandler handler = new AngularMaterialDialogHandler();
            boolean isOpen = handler.isOpen(driver);
            log("[" + testName + "] Dialog open: " + isOpen);

            Assertions.assertFalse(isOpen);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(12)
    void test12_DialogOpenAndDetect() {
        String testName = "Dialog_Open_Detect";
        try {
            // Open dialog
            driver.findElement(By.id("open-dialog-btn")).click();
            Thread.sleep(300);

            AngularMaterialDialogHandler handler = new AngularMaterialDialogHandler();
            boolean isOpen = handler.isOpen(driver);
            log("[" + testName + "] Dialog open after click: " + isOpen);

            Assertions.assertTrue(isOpen);
            takeScreenshot(testName + "_dialog_visible");
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(13)
    void test13_DialogGetContent() {
        String testName = "Dialog_GetContent";
        try {
            // Ensure dialog is open
            AngularMaterialDialogHandler handler = new AngularMaterialDialogHandler();
            if (!handler.isOpen(driver)) {
                driver.findElement(By.id("open-dialog-btn")).click();
                Thread.sleep(300);
            }

            WebElement content = handler.getDialogContent(driver);
            String text = content.getText().trim();
            log("[" + testName + "] Content: '" + text.substring(0, Math.min(80, text.length())) + "'");

            Assertions.assertTrue(text.contains("proceed with this operation"));
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(14)
    void test14_DialogClickAction() {
        String testName = "Dialog_ClickAction";
        try {
            AngularMaterialDialogHandler handler = new AngularMaterialDialogHandler();
            if (!handler.isOpen(driver)) {
                driver.findElement(By.id("open-dialog-btn")).click();
                Thread.sleep(300);
            }

            handler.clickAction(driver, "Confirm");
            Thread.sleep(300);

            // Dialog should be closed after confirm
            boolean isOpen = handler.isOpen(driver);
            log("[" + testName + "] Dialog open after Confirm: " + isOpen);

            // Check confirmation result
            WebElement result = driver.findElement(By.id("dialog-result"));
            log("[" + testName + "] Result text: '" + result.getText() + "'");

            Assertions.assertFalse(isOpen);
            Assertions.assertEquals("Action confirmed!", result.getText());
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(15)
    void test15_DialogCloseViaEscape() {
        String testName = "Dialog_Close_Escape";
        try {
            // Re-open dialog
            driver.findElement(By.id("open-dialog-btn")).click();
            Thread.sleep(300);

            AngularMaterialDialogHandler handler = new AngularMaterialDialogHandler();
            Assertions.assertTrue(handler.isOpen(driver), "Dialog should be open");

            handler.close(driver);
            Thread.sleep(300);

            boolean isOpen = handler.isOpen(driver);
            log("[" + testName + "] Dialog open after close: " + isOpen);

            Assertions.assertFalse(isOpen);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== TABLE TESTS ====================

    @Test
    @Order(16)
    void test16_TableColumnLookup() {
        String testName = "Table_Column_Lookup";
        try {
            WebElement matTable = driver.findElement(By.cssSelector("table.mat-mdc-table"));
            AngularMaterialTableHandler handler = new AngularMaterialTableHandler(matTable);

            int noIdx = handler.getColumnIndex(driver, "No.");
            int nameIdx = handler.getColumnIndex(driver, "Name");
            int weightIdx = handler.getColumnIndex(driver, "Weight");
            int symbolIdx = handler.getColumnIndex(driver, "Symbol");
            log("[" + testName + "] No.=" + noIdx + " Name=" + nameIdx + " Weight=" + weightIdx + " Symbol=" + symbolIdx);

            Assertions.assertEquals(0, noIdx);
            Assertions.assertEquals(1, nameIdx);
            Assertions.assertEquals(2, weightIdx);
            Assertions.assertEquals(3, symbolIdx);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(17)
    void test17_TableCellRead() {
        String testName = "Table_Cell_Read";
        try {
            WebElement matTable = driver.findElement(By.cssSelector("table.mat-mdc-table"));
            AngularMaterialTableHandler handler = new AngularMaterialTableHandler(matTable);

            String val0 = handler.getCellValue(driver, "No.", 0);
            String val1 = handler.getCellValue(driver, "Name", 0);
            String val2 = handler.getCellValue(driver, "Symbol", 2);
            log("[" + testName + "] Cell[No.,0]='" + val0 + "' Cell[Name,0]='" + val1 + "' Cell[Symbol,2]='" + val2 + "'");

            Assertions.assertEquals("1", val0);
            Assertions.assertEquals("Hydrogen", val1);
            Assertions.assertEquals("Li", val2);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(18)
    void test18_TableCellClick() {
        String testName = "Table_Cell_Click";
        try {
            WebElement matTable = driver.findElement(By.cssSelector("table.mat-mdc-table"));
            AngularMaterialTableHandler handler = new AngularMaterialTableHandler(matTable);

            handler.clickCell(driver, "Name", 1);
            log("[" + testName + "] Clicked cell[Name, row 1] successfully");

            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(19)
    void test19_TableRowSelection() {
        String testName = "Table_Row_Selection";
        try {
            WebElement matTable = driver.findElement(By.cssSelector("table.mat-mdc-table"));
            AngularMaterialTableHandler handler = new AngularMaterialTableHandler(matTable);

            handler.selectRow(driver, 2);
            WebElement row = handler.getRow(driver, 2);
            String rowClass = row.getAttribute("class");
            log("[" + testName + "] Row 2 class after select: '" + rowClass + "'");

            // Row should have 'selected' class after click
            Assertions.assertTrue(rowClass.contains("selected"));
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(20)
    void test20_TableGetRow() {
        String testName = "Table_GetRow";
        try {
            WebElement matTable = driver.findElement(By.cssSelector("table.mat-mdc-table"));
            AngularMaterialTableHandler handler = new AngularMaterialTableHandler(matTable);

            WebElement row0 = handler.getRow(driver, 0);
            WebElement row4 = handler.getRow(driver, 4);
            log("[" + testName + "] Row 0 tag: " + row0.getTagName() + " | Row 4 tag: " + row4.getTagName());

            Assertions.assertNotNull(row0);
            Assertions.assertNotNull(row4);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== HELPERS ====================

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
