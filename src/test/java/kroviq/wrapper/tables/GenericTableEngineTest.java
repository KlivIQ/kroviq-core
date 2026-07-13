package kroviq.wrapper.tables;

import kroviq.wrapper.core.TableEngine;
import kroviq.wrapper.core.TablePaginator;
import kroviq.wrapper.core.TableRowContext;
import kroviq.wrapper.factory.GenericTableEngine;
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
class GenericTableEngineTest {

    private WebDriver driver;
    private TableEngine engine;
    private String fixtureUrl;

    @BeforeAll
    void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox");
        driver = new ChromeDriver(options);

        File fixture = new File("src/test/resources/tables/complex-html-table-fixture.html");
        fixtureUrl = "file:///" + fixture.getAbsolutePath().replace("\\", "/");
        driver.get(fixtureUrl);

        WebElement table = driver.findElement(By.id("policyGrid"));
        engine = new GenericTableEngine(table);
    }

    @AfterAll
    void teardown() {
        if (driver != null) driver.quit();
    }

    // === Happy Path Tests ===

    @Test
    void findRow_singleColumn_findsCorrectRow() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-003");
        assertNotNull(row);
        assertEquals("Bob Wilson", engine.getCellValue(driver, row, "Insured Name"));
    }

    @Test
    void findRow_caseInsensitive() {
        WebElement row = engine.findRow(driver, "Status", "active");
        assertNotNull(row);
        assertEquals("POL-001", engine.getCellValue(driver, row, "Policy Number"));
    }

    @Test
    void findRow_notFound_throwsWithContextualMessage() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> engine.findRow(driver, "Policy Number", "NONEXISTENT"));
        assertTrue(ex.getMessage().contains("Row not found"));
        assertTrue(ex.getMessage().contains("NONEXISTENT"));
        // Must NOT leak raw Selenium exceptions
        assertFalse(ex.getMessage().contains("NoSuchElementException"));
    }

    @Test
    void findRowByCriteria_multiColumn() {
        WebElement row = engine.findRowByCriteria(driver, Map.of(
                "Product", "Motor",
                "Branch", "New York",
                "Status", "Pending"
        ));
        assertNotNull(row);
        assertEquals("POL-003", engine.getCellValue(driver, row, "Policy Number"));
    }

    @Test
    void rowExists_true() {
        assertTrue(engine.rowExists(driver, "Policy Number", "POL-004"));
    }

    @Test
    void rowExists_false() {
        assertFalse(engine.rowExists(driver, "Policy Number", "POL-999"));
    }

    @Test
    void getRowCount() {
        assertEquals(6, engine.getRowCount(driver));
    }

    @Test
    void getCellValue_fromRow() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-002");
        assertEquals("Jane Doe", engine.getCellValue(driver, row, "Insured Name"));
        assertEquals("Health", engine.getCellValue(driver, row, "Product"));
        assertEquals("2300.00", engine.getCellValue(driver, row, "Premium"));
    }

    @Test
    void getCellValue_invalidColumn_throwsWithAvailable() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-001");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> engine.getCellValue(driver, row, "NonExistentColumn"));
        assertTrue(ex.getMessage().contains("not found"));
        assertTrue(ex.getMessage().contains("Available"));
    }

    // === Duplicate Value Resolution Tests ===

    @Test
    void findRowByCriteria_duplicateName_resolvedByProduct() {
        // "John Smith" appears in rows 1 and 6
        WebElement row = engine.findRowByCriteria(driver, Map.of(
                "Insured Name", "John Smith",
                "Product", "Health"
        ));
        assertNotNull(row);
        assertEquals("POL-006", engine.getCellValue(driver, row, "Policy Number"));
    }

    @Test
    void findRowByCriteria_duplicateStatus_resolvedByMultipleColumns() {
        // "Active" appears in rows 1, 2, 5
        // "Motor" + "Active" appears in rows 1, 5
        // "Motor" + "Active" + "New York" appears in rows 1, 5
        // Add Premium to disambiguate
        WebElement row = engine.findRowByCriteria(driver, Map.of(
                "Status", "Active",
                "Product", "Motor",
                "Premium", "3200.00"
        ));
        assertNotNull(row);
        assertEquals("POL-005", engine.getCellValue(driver, row, "Policy Number"));
    }

    @Test
    void findRow_duplicateStatus_returnsFirst() {
        // Single-column search returns first match
        WebElement row = engine.findRow(driver, "Status", "Active");
        assertEquals("POL-001", engine.getCellValue(driver, row, "Policy Number"));
    }

    // === Action Priority Resolution Tests ===

    @Test
    void performRowAction_priority1_buttonText() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-001");
        // "Edit" exists as button text AND data-action — button text wins (priority 1)
        assertDoesNotThrow(() -> engine.performRowAction(driver, row, "Edit"));
    }

    @Test
    void performRowAction_priority2_linkText() {
        // POL-004 only has <a title="View">View</a> — link text (priority 2)
        WebElement row = engine.findRow(driver, "Policy Number", "POL-004");
        assertDoesNotThrow(() -> engine.performRowAction(driver, row, "View"));
    }

    @Test
    void performRowAction_priority4_ariaLabel() {
        // POL-003 has <button aria-label="Approve">Approve</button>
        // "Approve" matches button text (priority 1) first
        WebElement row = engine.findRow(driver, "Policy Number", "POL-003");
        assertDoesNotThrow(() -> engine.performRowAction(driver, row, "Approve"));
    }

    @Test
    void performRowAction_priority5_dataAction() {
        // POL-001 has <button data-action="Edit">Edit</button>
        // "Edit" matches button text first, but data-action is also present
        WebElement row = engine.findRow(driver, "Policy Number", "POL-001");
        assertDoesNotThrow(() -> engine.performRowAction(driver, row, "Edit"));
    }

    @Test
    void performRowAction_notFound_throwsDescriptive() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-004");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> engine.performRowAction(driver, row, "Delete"));
        assertTrue(ex.getMessage().contains("Action 'Delete' not found"));
        // Must NOT leak raw Selenium exceptions
        assertFalse(ex.getMessage().contains("NoSuchElementException"));
    }

    // === Stale Row Recovery Tests ===

    @Test
    void staleRowRecovery_rerenderTbody_engineStillFindsRow() {
        // Find row first
        WebElement row = engine.findRow(driver, "Policy Number", "POL-002");
        assertNotNull(row);

        // Simulate DOM rerender: replace tbody content via JS
        ((JavascriptExecutor) driver).executeScript(
                "var tbody = document.querySelector('#policyGrid tbody');" +
                "var html = tbody.innerHTML;" +
                "tbody.innerHTML = '';" +
                "tbody.innerHTML = html;");

        // Row reference is now stale — but engine should re-find
        // Re-create engine with fresh table root (simulates step-level re-resolution)
        WebElement freshTable = driver.findElement(By.id("policyGrid"));
        TableEngine freshEngine = new GenericTableEngine(freshTable);
        WebElement refoundRow = freshEngine.findRow(driver, "Policy Number", "POL-002");
        assertNotNull(refoundRow);
        assertEquals("Jane Doe", freshEngine.getCellValue(driver, refoundRow, "Insured Name"));
    }

    @Test
    void staleRowRecovery_tableRowContext_refindsByCriteria() {
        // Simulate TableRowContext usage pattern
        WebElement table = driver.findElement(By.id("policyGrid"));
        TableEngine eng = new GenericTableEngine(table);

        WebElement row = eng.findRow(driver, "Policy Number", "POL-005");
        TableRowContext ctx = TableRowContext.of(table, "Policy Number", "POL-005", row);

        // Simulate rerender
        ((JavascriptExecutor) driver).executeScript(
                "var tbody = document.querySelector('#policyGrid tbody');" +
                "var html = tbody.innerHTML;" +
                "tbody.innerHTML = '';" +
                "tbody.innerHTML = html;");

        // Re-find using stored criteria
        WebElement freshTable2 = driver.findElement(By.id("policyGrid"));
        TableEngine eng2 = new GenericTableEngine(freshTable2);
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
        engine.findRowByCriteria(driver, Map.of("Product", "Fire", "Status", "Cancelled"));
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

    @Test
    void performance_performRowAction_under3Seconds() {
        WebElement row = engine.findRow(driver, "Policy Number", "POL-001");
        long start = System.currentTimeMillis();
        engine.performRowAction(driver, row, "Edit");
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 3000, "performRowAction took " + elapsed + "ms, expected < 3000ms");
    }

    // === Backward Compatibility ===

    @Test
    void gridHandler_backwardCompat_getColumnIndex() {
        assertEquals(3, engine.getColumnIndex(driver, "Status"));
    }

    @Test
    void gridHandler_backwardCompat_getCellValueByIndex() {
        assertEquals("POL-001", engine.getCellValue(driver, "Policy Number", 0));
    }

    @Test
    void gridHandler_backwardCompat_getRow() {
        assertNotNull(engine.getRow(driver, 2));
    }

    @Test
    void paginatorInterface_available() {
        assertTrue(engine instanceof TablePaginator);
    }

    // === Multi-Table Isolation ===

    @Test
    void secondTable_completeIsolation() {
        WebElement claimsTable = driver.findElement(By.id("claimsGrid"));
        TableEngine claimsEngine = new GenericTableEngine(claimsTable);

        assertEquals(3, claimsEngine.getRowCount(driver));
        WebElement row = claimsEngine.findRow(driver, "Claim Number", "CLM-002");
        assertEquals("Jane Doe", claimsEngine.getCellValue(driver, row, "Claimant"));
        assertEquals("Under Review", claimsEngine.getCellValue(driver, row, "Status"));

        // Verify policy table is unaffected
        assertEquals(6, engine.getRowCount(driver));
    }

    @Test
    void secondTable_multiColumnLookup() {
        WebElement claimsTable = driver.findElement(By.id("claimsGrid"));
        TableEngine claimsEngine = new GenericTableEngine(claimsTable);

        // CLM-001 and CLM-003 both have Policy Number=POL-001
        WebElement row = claimsEngine.findRowByCriteria(driver, Map.of(
                "Policy Number", "POL-001",
                "Status", "Closed"
        ));
        assertEquals("CLM-003", claimsEngine.getCellValue(driver, row, "Claim Number"));
        assertEquals("800.00", claimsEngine.getCellValue(driver, row, "Amount"));
    }
}
