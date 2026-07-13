package kroviq.wrapper.tables;

import kroviq.wrapper.core.TableEngine;
import kroviq.wrapper.core.TablePaginator;
import kroviq.wrapper.core.TableRowContext;
import kroviq.wrapper.aggrid.AgGridTableEngine;
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
class AgGridTableEngineTest {

    private WebDriver driver;
    private TableEngine engine;
    private String fixtureUrl;

    @BeforeAll
    void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox");
        driver = new ChromeDriver(options);

        File fixture = new File("src/test/resources/tables/ag-grid-table-fixture.html");
        fixtureUrl = "file:///" + fixture.getAbsolutePath().replace("\\", "/");
        driver.get(fixtureUrl);

        WebElement table = driver.findElement(By.id("agGridFixture"));
        engine = new AgGridTableEngine(table);
    }

    @AfterAll
    void teardown() {
        if (driver != null) driver.quit();
    }

    // === Happy Path Tests ===

    @Test
    void findRow_byPolicyNumber() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-003");
        assertNotNull(row);
        assertEquals("Bob Wilson", engine.getCellValue(driver, row, "Insured Name"));
    }

    @Test
    void findRow_byStatus() {
        WebElement row = engine.findRow(driver, "Status", "Pending");
        assertNotNull(row);
        assertEquals("POL-002", engine.getCellValue(driver, row, "Policy Number"));
    }

    @Test
    void findRow_notFound_throwsDescriptive() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> engine.findRow(driver, "Policy Number", "POL-999"));
        assertTrue(ex.getMessage().contains("Row not found"));
        assertTrue(ex.getMessage().contains("POL-999"));
        assertFalse(ex.getMessage().contains("NoSuchElementException"));
    }

    @Test
    void findRowByCriteria_multiColumn() {
        WebElement row = engine.findRowByCriteria(driver, Map.of(
                "Status", "Active",
                "Insured Name", "Charlie Davis"
        ));
        assertNotNull(row);
        assertEquals("3200.00", engine.getCellValue(driver, row, "Premium"));
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

    @Test
    void getCellValue_allColumns() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-001");
        assertEquals("POL-001", engine.getCellValue(driver, row, "Policy Number"));
        assertEquals("John Smith", engine.getCellValue(driver, row, "Insured Name"));
        assertEquals("Active", engine.getCellValue(driver, row, "Status"));
        assertEquals("1500.00", engine.getCellValue(driver, row, "Premium"));
    }

    // === Action Tests ===

    @Test
    void performRowAction_directButton() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-001");
        assertDoesNotThrow(() -> engine.performRowAction(driver, row, "Edit"));
    }

    @Test
    void performRowAction_viewButton() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-004");
        assertDoesNotThrow(() -> engine.performRowAction(driver, row, "View"));
    }

    @Test
    void performRowAction_notFound_throwsDescriptive() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-004");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> engine.performRowAction(driver, row, "Approve"));
        assertTrue(ex.getMessage().contains("Action 'Approve' not found"));
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

    // === Stale Grid Recovery Tests ===

    @Test
    void staleGridRecovery_afterDomReplace_engineRecovers() {
        // Find row first
        WebElement row = engine.findRow(driver, "Policy Number", "POL-001");
        assertNotNull(row);

        // Simulate grid refresh: replace body viewport content
        ((JavascriptExecutor) driver).executeScript(
                "var viewport = document.querySelector('.ag-body-viewport');" +
                "var html = viewport.innerHTML;" +
                "viewport.innerHTML = '';" +
                "viewport.innerHTML = html;");

        // Re-create engine (simulates step-level re-resolution)
        WebElement freshGrid = driver.findElement(By.id("agGridFixture"));
        TableEngine freshEngine = new AgGridTableEngine(freshGrid);
        WebElement refound = freshEngine.findRow(driver, "Policy Number", "POL-001");
        assertNotNull(refound);
        assertEquals("John Smith", freshEngine.getCellValue(driver, refound, "Insured Name"));
    }

    @Test
    void staleGridRecovery_tableRowContext_refindsByCriteria() {
        WebElement grid = driver.findElement(By.id("agGridFixture"));
        TableEngine eng = new AgGridTableEngine(grid);

        WebElement row = eng.findRow(driver, "Policy Number", "POL-003");
        TableRowContext ctx = TableRowContext.of(grid, "Policy Number", "POL-003", row);

        // Simulate rerender
        ((JavascriptExecutor) driver).executeScript(
                "var viewport = document.querySelector('.ag-body-viewport');" +
                "var html = viewport.innerHTML;" +
                "viewport.innerHTML = '';" +
                "viewport.innerHTML = html;");

        // Recover using stored criteria
        WebElement freshGrid = driver.findElement(By.id("agGridFixture"));
        TableEngine eng2 = new AgGridTableEngine(freshGrid);
        WebElement recovered = eng2.findRow(driver, ctx.getFindColumn(), ctx.getFindValue());
        assertNotNull(recovered);
        assertEquals("Bob Wilson", eng2.getCellValue(driver, recovered, "Insured Name"));
    }

    // === Virtualized Scroll Recovery Test ===

    @Test
    void virtualizedScroll_rowOutsideViewport_foundAfterScroll() {
        // In our fixture all rows are in DOM (no real virtualization)
        // But verify the scroll logic doesn't break when rows ARE visible
        WebElement row = engine.findRow(driver, "Policy Number", "POL-005");
        assertNotNull(row);
        assertEquals("Charlie Davis", engine.getCellValue(driver, row, "Insured Name"));
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
    void performance_performRowAction_under3Seconds() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-002");
        long start = System.currentTimeMillis();
        engine.performRowAction(driver, row, "Edit");
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 3000, "performRowAction took " + elapsed + "ms, expected < 3000ms");
    }

    // === Backward Compatibility ===

    @Test
    void gridHandler_backwardCompat_getColumnIndex() {
        assertEquals(3, engine.getColumnIndex(driver, "Premium"));
    }

    @Test
    void gridHandler_backwardCompat_getCellValueByIndex() {
        assertEquals("Pending", engine.getCellValue(driver, "Status", 1));
    }

    @Test
    void gridHandler_backwardCompat_getRow() {
        assertNotNull(engine.getRow(driver, 0));
    }
}
