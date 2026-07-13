package kroviq.wrapper.tables;

import kroviq.wrapper.core.TableEngine;
import kroviq.wrapper.core.TablePaginator;
import kroviq.wrapper.core.TableRowContext;
import kroviq.wrapper.antd.AntDTableEngine;
import org.junit.jupiter.api.*;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AntDTableEngineTest {

    private WebDriver driver;
    private TableEngine engine;
    private String fixtureUrl;

    @BeforeAll
    void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox");
        driver = new ChromeDriver(options);

        File fixture = new File("src/test/resources/tables/antd-table-fixture.html");
        fixtureUrl = "file:///" + fixture.getAbsolutePath().replace("\\", "/");
        driver.get(fixtureUrl);

        WebElement table = driver.findElement(By.id("antdTableFixture"));
        engine = new AntDTableEngine(table, driver);
    }

    @AfterAll
    void teardown() {
        if (driver != null) driver.quit();
    }

    // === Happy Path Tests ===

    @Test
    void findRow_byPolicyNumber() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-002");
        assertNotNull(row);
        assertEquals("Jane Doe", engine.getCellValue(driver, row, "Insured Name"));
    }

    @Test
    void findRow_byStatus_findsFirst() {
        WebElement row = engine.findRow(driver, "Status", "Active");
        assertNotNull(row);
        assertEquals("POL-001", engine.getCellValue(driver, row, "Policy Number"));
    }

    @Test
    void findRow_notFound_throwsDescriptive() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> engine.findRow(driver, "Policy Number", "POL-999"));
        assertTrue(ex.getMessage().contains("Row not found"));
        assertFalse(ex.getMessage().contains("NoSuchElementException"));
    }

    @Test
    void findRowByCriteria_multiColumn() {
        WebElement row = engine.findRowByCriteria(driver, Map.of(
                "Status", "Active",
                "Insured Name", "Bob Wilson"
        ));
        assertNotNull(row);
        assertEquals("980.00", engine.getCellValue(driver, row, "Premium"));
    }

    @Test
    void rowExists_true() {
        assertTrue(engine.rowExists(driver, "Insured Name", "Alice Brown"));
    }

    @Test
    void rowExists_false() {
        assertFalse(engine.rowExists(driver, "Insured Name", "Nobody"));
    }

    @Test
    void getRowCount() {
        assertEquals(5, engine.getRowCount(driver));
    }

    // === AntD-Specific: Tag Extraction Tests ===

    @Test
    void getCellValue_extractsFromSpan() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-001");
        assertEquals("John Smith", engine.getCellValue(driver, row, "Insured Name"));
        assertEquals("1500.00", engine.getCellValue(driver, row, "Premium"));
    }

    @Test
    void getCellValue_extractsFromAntTag_active() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-001");
        String status = engine.getCellValue(driver, row, "Status");
        assertEquals("Active", status);
        // Must NOT contain HTML or class names
        assertFalse(status.contains("ant-tag"));
        assertFalse(status.contains("<"));
    }

    @Test
    void getCellValue_extractsFromAntTag_cancelled() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-004");
        String status = engine.getCellValue(driver, row, "Status");
        assertEquals("Cancelled", status);
    }

    @Test
    void getCellValue_extractsFromAntTag_pending() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-002");
        String status = engine.getCellValue(driver, row, "Status");
        assertEquals("Pending", status);
    }

    @Test
    void getCellValue_invalidColumn_throws() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-001");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> engine.getCellValue(driver, row, "FakeColumn"));
        assertTrue(ex.getMessage().contains("not found"));
        assertTrue(ex.getMessage().contains("Available"));
    }

    // === AntD-Specific: Column Mapping Tests ===

    @Test
    void columnMapping_allColumnsResolved() {
        // Verify all expected columns are found
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Policy Number"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Insured Name"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Status"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Premium"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Actions"));
    }

    @Test
    void columnMapping_correctOrder() {
        assertEquals(0, engine.getColumnIndex(driver, "Policy Number"));
        assertEquals(1, engine.getColumnIndex(driver, "Insured Name"));
        assertEquals(2, engine.getColumnIndex(driver, "Status"));
        assertEquals(3, engine.getColumnIndex(driver, "Premium"));
        assertEquals(4, engine.getColumnIndex(driver, "Actions"));
    }

    // === Pagination Tests ===

    @Test
    void paginatorInterface_implemented() {
        assertTrue(engine instanceof TablePaginator);
    }

    @Test
    void hasNextPage_true() {
        assertTrue(((TablePaginator) engine).hasNextPage(driver));
    }

    // === Stale Recovery Tests ===

    @Test
    void staleRecovery_afterTbodyRerender_engineRecovers() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-003");
        assertNotNull(row);

        // Simulate AntD table rerender
        ((JavascriptExecutor) driver).executeScript(
                "var tbody = document.querySelector('.ant-table-tbody');" +
                "var html = tbody.innerHTML;" +
                "tbody.innerHTML = '';" +
                "tbody.innerHTML = html;");

        // Re-create engine (simulates step-level re-resolution)
        WebElement freshTable = driver.findElement(By.id("antdTableFixture"));
        TableEngine freshEngine = new AntDTableEngine(freshTable, driver);
        WebElement refound = freshEngine.findRow(driver, "Policy Number", "POL-003");
        assertNotNull(refound);
        assertEquals("Bob Wilson", freshEngine.getCellValue(driver, refound, "Insured Name"));
    }

    @Test
    void staleRecovery_tableRowContext_refindsByCriteria() {
        WebElement table = driver.findElement(By.id("antdTableFixture"));
        TableEngine eng = new AntDTableEngine(table, driver);

        WebElement row = eng.findRow(driver, "Policy Number", "POL-005");
        TableRowContext ctx = TableRowContext.of(table, "Policy Number", "POL-005", row);

        // Simulate rerender
        ((JavascriptExecutor) driver).executeScript(
                "var tbody = document.querySelector('.ant-table-tbody');" +
                "var html = tbody.innerHTML;" +
                "tbody.innerHTML = '';" +
                "tbody.innerHTML = html;");

        // Recover using stored criteria
        WebElement freshTable = driver.findElement(By.id("antdTableFixture"));
        TableEngine eng2 = new AntDTableEngine(freshTable, driver);
        WebElement recovered = eng2.findRow(driver, ctx.getFindColumn(), ctx.getFindValue());
        assertNotNull(recovered);
        assertEquals("Charlie Davis", eng2.getCellValue(driver, recovered, "Insured Name"));
    }

    // === Performance Guardrails ===

    @Test
    void performance_findRow_under3Seconds() {
        long start = System.currentTimeMillis();
        engine.findRow(driver, "Policy Number", "POL-005");
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 3000, "findRow took " + elapsed + "ms, expected < 3000ms");
    }

    @Test
    void performance_findRowByCriteria_under3Seconds() {
        long start = System.currentTimeMillis();
        engine.findRowByCriteria(driver, Map.of("Status", "Cancelled"));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 3000, "findRowByCriteria took " + elapsed + "ms, expected < 3000ms");
    }

    @Test
    void performance_getRowCount_under1Second() {
        long start = System.currentTimeMillis();
        engine.getRowCount(driver);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 1000, "getRowCount took " + elapsed + "ms, expected < 1000ms");
    }

    @Test
    void performance_getCellValue_under1Second() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-001");
        long start = System.currentTimeMillis();
        engine.getCellValue(driver, row, "Premium");
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 1000, "getCellValue took " + elapsed + "ms, expected < 1000ms");
    }

    // === Backward Compatibility ===

    @Test
    void gridHandler_backwardCompat_getColumnIndex() {
        assertEquals(3, engine.getColumnIndex(driver, "Premium"));
    }

    @Test
    void gridHandler_backwardCompat_getCellValueByIndex() {
        assertEquals("POL-001", engine.getCellValue(driver, "Policy Number", 0));
    }

    @Test
    void gridHandler_backwardCompat_getRow() {
        assertNotNull(engine.getRow(driver, 2));
    }
}
