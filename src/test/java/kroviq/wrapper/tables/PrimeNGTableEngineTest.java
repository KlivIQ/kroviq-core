package kroviq.wrapper.tables;

import kroviq.wrapper.core.TableEngine;
import kroviq.wrapper.core.TablePaginator;
import kroviq.wrapper.primeng.PrimeNGTableEngine;
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
class PrimeNGTableEngineTest {

    private WebDriver driver;
    private TableEngine engine;
    private String fixtureUrl;

    @BeforeAll
    void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox");
        driver = new ChromeDriver(options);

        File fixture = new File("src/test/resources/tables/primeng-table-fixture.html");
        fixtureUrl = "file:///" + fixture.getAbsolutePath().replace("\\", "/");
        driver.get(fixtureUrl);

        WebElement table = driver.findElement(By.id("primeNGTableFixture"));
        engine = new PrimeNGTableEngine(table, driver);
    }

    @AfterAll
    void teardown() {
        if (driver != null) driver.quit();
    }

    // === Happy Path Tests ===

    @Test
    void findRow_byEmployeeId() {
        WebElement row = engine.findRow(driver, "Employee ID", "EMP-002");
        assertNotNull(row);
        assertEquals("Jane Doe", engine.getCellValue(driver, row, "Full Name"));
    }

    @Test
    void findRow_byName() {
        WebElement row = engine.findRow(driver, "Full Name", "Bob Wilson");
        assertNotNull(row);
        assertEquals("EMP-003", engine.getCellValue(driver, row, "Employee ID"));
    }

    @Test
    void findRow_notFound_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> engine.findRow(driver, "Employee ID", "EMP-999"));
        assertTrue(ex.getMessage().contains("Row not found"));
    }

    @Test
    void findRowByCriteria_multiColumn() {
        WebElement row = engine.findRowByCriteria(driver, Map.of(
                "Department", "Engineering",
                "Full Name", "Charlie Davis"
        ));
        assertNotNull(row);
        assertEquals("95000", engine.getCellValue(driver, row, "Salary"));
    }

    @Test
    void rowExists_true() {
        assertTrue(engine.rowExists(driver, "Full Name", "Alice Brown"));
    }

    @Test
    void rowExists_false() {
        assertFalse(engine.rowExists(driver, "Full Name", "Nobody"));
    }

    @Test
    void getRowCount() {
        assertEquals(5, engine.getRowCount(driver));
    }

    // === PrimeNG-Specific: Tag Extraction ===

    @Test
    void getCellValue_extractsFromPTag() {
        WebElement row = engine.findRow(driver, "Employee ID", "EMP-001");
        assertEquals("Active", engine.getCellValue(driver, row, "Status"));
    }

    @Test
    void getCellValue_extractsOnLeave() {
        WebElement row = engine.findRow(driver, "Employee ID", "EMP-002");
        assertEquals("On Leave", engine.getCellValue(driver, row, "Status"));
    }

    @Test
    void getCellValue_extractsTerminated() {
        WebElement row = engine.findRow(driver, "Employee ID", "EMP-004");
        assertEquals("Terminated", engine.getCellValue(driver, row, "Status"));
    }

    // === Column Mapping ===

    @Test
    void columnMapping_allColumnsResolved() {
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Employee ID"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Full Name"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Department"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Status"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Salary"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Actions"));
    }

    @Test
    void columnMapping_correctOrder() {
        assertEquals(0, engine.getColumnIndex(driver, "Employee ID"));
        assertEquals(1, engine.getColumnIndex(driver, "Full Name"));
        assertEquals(2, engine.getColumnIndex(driver, "Department"));
        assertEquals(3, engine.getColumnIndex(driver, "Status"));
        assertEquals(4, engine.getColumnIndex(driver, "Salary"));
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
        WebElement row = engine.findRow(driver, "Employee ID", "EMP-003");
        assertNotNull(row);

        ((JavascriptExecutor) driver).executeScript(
                "var tbody = document.querySelector('.p-datatable-tbody');" +
                "var html = tbody.innerHTML;" +
                "tbody.innerHTML = '';" +
                "tbody.innerHTML = html;");

        WebElement freshTable = driver.findElement(By.id("primeNGTableFixture"));
        TableEngine freshEngine = new PrimeNGTableEngine(freshTable, driver);
        WebElement refound = freshEngine.findRow(driver, "Employee ID", "EMP-003");
        assertNotNull(refound);
        assertEquals("Bob Wilson", freshEngine.getCellValue(driver, refound, "Full Name"));
    }

    // === Performance ===

    @Test
    void performance_findRow_under3Seconds() {
        long start = System.currentTimeMillis();
        engine.findRow(driver, "Employee ID", "EMP-005");
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
        assertEquals(4, engine.getColumnIndex(driver, "Salary"));
    }

    @Test
    void gridHandler_backwardCompat_getCellValueByIndex() {
        assertEquals("EMP-001", engine.getCellValue(driver, "Employee ID", 0));
    }

    @Test
    void gridHandler_backwardCompat_getRow() {
        assertNotNull(engine.getRow(driver, 2));
    }
}
