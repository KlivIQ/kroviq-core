package kroviq.wrapper.integration;

import io.github.bonigarcia.wdm.WebDriverManager;
import kroviq.wrapper.aggrid.AgGridHandler;
import kroviq.wrapper.core.GridHandler;
import kroviq.wrapper.factory.ComponentHandlerFactory;
import kroviq.wrapper.factory.GenericGridHandler;
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

/**
 * AG Grid Phase 1 POC — Live browser validation.
 * Tests against AG Grid official demo pages.
 * NOT production code. R&D only.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AgGridLiveIntegrationTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static final String SCREENSHOT_DIR = "target/screenshots/aggrid/";
    private static final StringBuilder LOG = new StringBuilder();
    private static int passed = 0;
    private static int failed = 0;

    // AG Grid demo URLs
    private static final String BASIC_GRID_URL = "https://www.ag-grid.com/example/";
    private static final String SIMPLE_GRID_URL = "https://www.ag-grid.com/javascript-data-grid/getting-started/";

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
        log("=== AG Grid Live Integration Test Started ===");
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
            Files.writeString(Paths.get(SCREENSHOT_DIR, "aggrid-test-results.log"), LOG.toString());
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }

        if (driver != null) driver.quit();
    }

    // ==================== DETECTION TESTS ====================

    @Test
    @Order(1)
    void test01_DetectorIdentifiesAgGrid() {
        String testName = "Detector_AG_GRID";
        try {
            navigateToBasicGrid();

            WebElement gridRoot = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("[class*='ag-root-wrapper'], [class*='ag-grid']")));

            UIFrameworkDetector.clearCache();
            UIFramework detected = UIFrameworkDetector.detect(gridRoot);
            log("[" + testName + "] Detected: " + detected + " | class: " + gridRoot.getAttribute("class"));

            Assertions.assertEquals(UIFramework.AG_GRID, detected);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    @Test
    @Order(2)
    void test02_FactoryReturnsAgGridHandler() {
        String testName = "Factory_Returns_AgGridHandler";
        try {
            navigateToBasicGrid();

            WebElement gridRoot = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("[class*='ag-root-wrapper'], [class*='ag-grid']")));

            UIFrameworkDetector.clearCache();
            GridHandler handler = ComponentHandlerFactory.getGridHandler(gridRoot, driver);
            log("[" + testName + "] Handler: " + handler.getClass().getSimpleName());

            Assertions.assertInstanceOf(AgGridHandler.class, handler);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== COLUMN LOOKUP ====================

    @Test
    @Order(3)
    void test03_ColumnLookupByHeaderText() {
        String testName = "Column_Lookup_By_Header";
        try {
            navigateToBasicGrid();

            WebElement gridRoot = findGridRoot();
            GridHandler grid = ComponentHandlerFactory.getGridHandler(gridRoot, driver);

            // AG Grid demo typically has columns like "Make", "Model", "Price" or similar
            // Try to find any header and get its index
            WebElement firstHeader = gridRoot.findElement(By.cssSelector(".ag-header-cell-text"));
            String headerText = firstHeader.getText().trim();
            log("[" + testName + "] First header found: '" + headerText + "'");

            int colIndex = grid.getColumnIndex(driver, headerText);
            log("[" + testName + "] Column index for '" + headerText + "': " + colIndex);

            Assertions.assertTrue(colIndex >= 0);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== CELL READ ====================

    @Test
    @Order(4)
    void test04_CellReadByColumnAndRow() {
        String testName = "Cell_Read";
        try {
            navigateToBasicGrid();

            WebElement gridRoot = findGridRoot();
            GridHandler grid = ComponentHandlerFactory.getGridHandler(gridRoot, driver);

            // Get first column header
            WebElement firstHeader = gridRoot.findElement(By.cssSelector(".ag-header-cell-text"));
            String headerText = firstHeader.getText().trim();

            String cellValue = grid.getCellValue(driver, headerText, 0);
            log("[" + testName + "] Cell['" + headerText + "', row 0] = '" + cellValue + "'");

            Assertions.assertNotNull(cellValue);
            Assertions.assertFalse(cellValue.isEmpty(), "Cell value should not be empty");
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== CELL CLICK ====================

    @Test
    @Order(5)
    void test05_CellClick() {
        String testName = "Cell_Click";
        try {
            navigateToBasicGrid();

            WebElement gridRoot = findGridRoot();
            GridHandler grid = ComponentHandlerFactory.getGridHandler(gridRoot, driver);

            WebElement firstHeader = gridRoot.findElement(By.cssSelector(".ag-header-cell-text"));
            String headerText = firstHeader.getText().trim();

            // Should not throw
            grid.clickCell(driver, headerText, 0);
            log("[" + testName + "] Clicked cell['" + headerText + "', row 0] successfully");

            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== ROW SELECTION ====================

    @Test
    @Order(6)
    void test06_RowSelection() {
        String testName = "Row_Selection";
        try {
            navigateToBasicGrid();

            WebElement gridRoot = findGridRoot();
            GridHandler grid = ComponentHandlerFactory.getGridHandler(gridRoot, driver);

            grid.selectRow(driver, 0);
            log("[" + testName + "] Selected row 0 successfully");

            // Verify row element exists
            WebElement row = grid.getRow(driver, 0);
            Assertions.assertNotNull(row);
            log("[" + testName + "] Row 0 element found: " + row.getAttribute("class"));

            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== VIRTUAL SCROLLING ====================

    @Test
    @Order(7)
    void test07_VirtualScrollToLaterRow() {
        String testName = "Virtual_Scroll";
        try {
            navigateToBasicGrid();

            WebElement gridRoot = findGridRoot();
            GridHandler grid = ComponentHandlerFactory.getGridHandler(gridRoot, driver);

            // Try to access a row that's likely outside initial viewport (row 15+)
            WebElement row = grid.getRow(driver, 15);
            Assertions.assertNotNull(row);
            log("[" + testName + "] Row 15 found after scroll: " + row.getAttribute("row-index"));

            // Read a cell from the scrolled row
            WebElement firstHeader = gridRoot.findElement(By.cssSelector(".ag-header-cell-text"));
            String headerText = firstHeader.getText().trim();
            String value = grid.getCellValue(driver, headerText, 15);
            log("[" + testName + "] Cell['" + headerText + "', row 15] = '" + value + "'");

            Assertions.assertNotNull(value);
            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== GENERIC FALLBACK ====================

    @Test
    @Order(8)
    void test08_GenericGridHandlerFallback() {
        String testName = "Generic_Grid_Fallback";
        try {
            // Navigate to a page with a plain HTML table (not AG Grid)
            driver.get("https://www.w3schools.com/html/html_tables.asp");
            waitForPageLoad();

            WebElement table = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("table#customers")));

            UIFrameworkDetector.clearCache();
            UIFramework detected = UIFrameworkDetector.detect(table);
            log("[" + testName + "] Detected framework: " + detected);

            GridHandler handler = ComponentHandlerFactory.getGridHandler(table, driver);
            log("[" + testName + "] Handler: " + handler.getClass().getSimpleName());

            Assertions.assertInstanceOf(GenericGridHandler.class, handler);

            // Verify GenericGridHandler can read from plain HTML table
            String value = handler.getCellValue(driver, "Company", 0);
            log("[" + testName + "] GenericGrid cell['Company', row 0] = '" + value + "'");
            Assertions.assertNotNull(value);
            Assertions.assertFalse(value.isEmpty());

            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e);
        }
    }

    // ==================== HELPERS ====================

    private void navigateToBasicGrid() {
        driver.get(BASIC_GRID_URL);
        waitForPageLoad();
        // Wait for AG Grid to render
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".ag-row")));
    }

    private WebElement findGridRoot() {
        return wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[class*='ag-root-wrapper'], [class*='ag-grid']")));
    }

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
