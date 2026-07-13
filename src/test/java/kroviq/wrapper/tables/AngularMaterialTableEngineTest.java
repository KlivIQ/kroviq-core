package kroviq.wrapper.tables;

import kroviq.wrapper.core.TableEngine;
import kroviq.wrapper.core.TablePaginator;
import kroviq.wrapper.angularmaterial.AngularMaterialTableEngine;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AngularMaterialTableEngineTest {

    private WebDriver driver;
    private TableEngine engine;
    private String fixtureUrl;

    @BeforeAll
    void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox");
        driver = new ChromeDriver(options);

        File fixture = new File("src/test/resources/tables/angular-material-table-fixture.html");
        fixtureUrl = "file:///" + fixture.getAbsolutePath().replace("\\", "/");
        driver.get(fixtureUrl);

        WebElement table = driver.findElement(By.id("angularMaterialTableFixture"));
        engine = new AngularMaterialTableEngine(table, driver);
    }

    @AfterAll
    void teardown() {
        if (driver != null) driver.quit();
    }

    // === Happy Path Tests ===

    @Test
    void findRow_byTicketId() {
        WebElement row = engine.findRow(driver, "Ticket ID", "TKT-002");
        assertNotNull(row);
        assertEquals("James Wilson", engine.getCellValue(driver, row, "Assignee"));
    }

    @Test
    void findRow_byTitle() {
        WebElement row = engine.findRow(driver, "Title", "Export fails");
        assertNotNull(row);
        assertEquals("TKT-003", engine.getCellValue(driver, row, "Ticket ID"));
    }

    @Test
    void findRow_notFound_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> engine.findRow(driver, "Ticket ID", "TKT-999"));
        assertTrue(ex.getMessage().contains("Row not found"));
    }

    @Test
    void findRowByCriteria_multiColumn() {
        WebElement row = engine.findRowByCriteria(driver, Map.of(
                "Assignee", "Sarah Parker",
                "Status", "Open"
        ));
        assertNotNull(row);
        assertEquals("TKT-001", engine.getCellValue(driver, row, "Ticket ID"));
    }

    @Test
    void findRowByCriteria_duplicateAssignee() {
        WebElement row = engine.findRowByCriteria(driver, Map.of(
                "Assignee", "James Wilson",
                "Priority", "Critical"
        ));
        assertNotNull(row);
        assertEquals("TKT-005", engine.getCellValue(driver, row, "Ticket ID"));
    }

    @Test
    void rowExists_true() {
        assertTrue(engine.rowExists(driver, "Title", "API timeout"));
    }

    @Test
    void rowExists_false() {
        assertFalse(engine.rowExists(driver, "Title", "Nonexistent"));
    }

    @Test
    void getRowCount() {
        assertEquals(5, engine.getRowCount(driver));
    }

    // === Angular Material-Specific: Chip Extraction ===

    @Test
    void getCellValue_extractsFromMatChip() {
        WebElement row = engine.findRow(driver, "Ticket ID", "TKT-001");
        assertEquals("Critical", engine.getCellValue(driver, row, "Priority"));
    }

    @Test
    void getCellValue_extractsHighPriority() {
        WebElement row = engine.findRow(driver, "Ticket ID", "TKT-002");
        assertEquals("High", engine.getCellValue(driver, row, "Priority"));
    }

    @Test
    void getCellValue_extractsMediumPriority() {
        WebElement row = engine.findRow(driver, "Ticket ID", "TKT-003");
        assertEquals("Medium", engine.getCellValue(driver, row, "Priority"));
    }

    // === Column Mapping ===

    @Test
    void columnMapping_allColumnsResolved() {
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Ticket ID"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Title"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Assignee"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Priority"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Status"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Actions"));
    }

    @Test
    void columnMapping_correctOrder() {
        assertEquals(0, engine.getColumnIndex(driver, "Ticket ID"));
        assertEquals(1, engine.getColumnIndex(driver, "Title"));
        assertEquals(2, engine.getColumnIndex(driver, "Assignee"));
        assertEquals(3, engine.getColumnIndex(driver, "Priority"));
        assertEquals(4, engine.getColumnIndex(driver, "Status"));
        assertEquals(5, engine.getColumnIndex(driver, "Actions"));
    }

    // === Pagination ===

    @Test
    void paginatorInterface_implemented() {
        assertTrue(engine instanceof TablePaginator);
    }

    @Test
    void hasNextPage_true() {
        assertTrue(((TablePaginator) engine).hasNextPage(driver));
    }

    // === Stale Recovery ===

    @Test
    void staleRecovery_afterRerender() {
        WebElement row = engine.findRow(driver, "Ticket ID", "TKT-003");
        assertNotNull(row);

        ((JavascriptExecutor) driver).executeScript(
                "var tbody = document.querySelector('tbody');" +
                "var html = tbody.innerHTML;" +
                "tbody.innerHTML = '';" +
                "tbody.innerHTML = html;");

        WebElement freshTable = driver.findElement(By.id("angularMaterialTableFixture"));
        TableEngine freshEngine = new AngularMaterialTableEngine(freshTable, driver);
        WebElement refound = freshEngine.findRow(driver, "Ticket ID", "TKT-003");
        assertNotNull(refound);
        assertEquals("Export fails", freshEngine.getCellValue(driver, refound, "Title"));
    }

    // === Performance ===

    @Test
    void performance_findRow_under3Seconds() {
        long start = System.currentTimeMillis();
        engine.findRow(driver, "Ticket ID", "TKT-005");
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 3000, "findRow took " + elapsed + "ms, expected < 3000ms");
    }

    @Test
    void performance_getRowCount_under1Second() {
        long start = System.currentTimeMillis();
        engine.getRowCount(driver);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 1000, "getRowCount took " + elapsed + "ms, expected < 1000ms");
    }

    // === Backward Compatibility ===

    @Test
    void gridHandler_backwardCompat_getColumnIndex() {
        assertEquals(2, engine.getColumnIndex(driver, "Assignee"));
    }

    @Test
    void gridHandler_backwardCompat_getCellValueByIndex() {
        assertEquals("TKT-001", engine.getCellValue(driver, "Ticket ID", 0));
    }

    @Test
    void gridHandler_backwardCompat_getRow() {
        assertNotNull(engine.getRow(driver, 2));
    }
}
