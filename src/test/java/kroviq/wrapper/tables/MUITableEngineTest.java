package kroviq.wrapper.tables;

import kroviq.wrapper.core.TableEngine;
import kroviq.wrapper.core.TablePaginator;
import kroviq.wrapper.mui.MUITableEngine;
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
class MUITableEngineTest {

    private WebDriver driver;
    private TableEngine engine;
    private String fixtureUrl;

    @BeforeAll
    void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox");
        driver = new ChromeDriver(options);

        File fixture = new File("src/test/resources/tables/mui-datagrid-fixture.html");
        fixtureUrl = "file:///" + fixture.getAbsolutePath().replace("\\", "/");
        driver.get(fixtureUrl);

        WebElement table = driver.findElement(By.id("muiDataGridFixture"));
        engine = new MUITableEngine(table, driver);
    }

    @AfterAll
    void teardown() {
        if (driver != null) driver.quit();
    }

    // === Happy Path Tests ===

    @Test
    void findRow_byInvoiceId() {
        WebElement row = engine.findRow(driver, "Invoice ID", "INV-002");
        assertNotNull(row);
        assertEquals("TechStart LLC", engine.getCellValue(driver, row, "Client"));
    }

    @Test
    void findRow_byClient() {
        WebElement row = engine.findRow(driver, "Client", "GlobalTech Inc");
        assertNotNull(row);
        assertEquals("INV-004", engine.getCellValue(driver, row, "Invoice ID"));
    }

    @Test
    void findRow_notFound_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> engine.findRow(driver, "Invoice ID", "INV-999"));
        assertTrue(ex.getMessage().contains("Row not found"));
    }

    @Test
    void findRowByCriteria_multiColumn() {
        WebElement row = engine.findRowByCriteria(driver, Map.of(
                "Client", "Acme Corp",
                "Project", "API Integration"
        ));
        assertNotNull(row);
        assertEquals("9500.00", engine.getCellValue(driver, row, "Amount"));
    }

    @Test
    void findRowByCriteria_duplicateClient() {
        WebElement row = engine.findRowByCriteria(driver, Map.of(
                "Client", "Acme Corp",
                "Project", "Website Redesign"
        ));
        assertNotNull(row);
        assertEquals("INV-001", engine.getCellValue(driver, row, "Invoice ID"));
    }

    @Test
    void rowExists_true() {
        assertTrue(engine.rowExists(driver, "Client", "StartupXYZ"));
    }

    @Test
    void rowExists_false() {
        assertFalse(engine.rowExists(driver, "Client", "Nobody Inc"));
    }

    @Test
    void getRowCount() {
        assertEquals(5, engine.getRowCount(driver));
    }

    // === MUI-Specific: Chip Extraction ===

    @Test
    void getCellValue_extractsFromMuiChip_paid() {
        WebElement row = engine.findRow(driver, "Invoice ID", "INV-001");
        assertEquals("Paid", engine.getCellValue(driver, row, "Status"));
    }

    @Test
    void getCellValue_extractsFromMuiChip_pending() {
        WebElement row = engine.findRow(driver, "Invoice ID", "INV-002");
        assertEquals("Pending", engine.getCellValue(driver, row, "Status"));
    }

    @Test
    void getCellValue_extractsFromMuiChip_overdue() {
        WebElement row = engine.findRow(driver, "Invoice ID", "INV-004");
        assertEquals("Overdue", engine.getCellValue(driver, row, "Status"));
    }

    // === Column Mapping ===

    @Test
    void columnMapping_allColumnsResolved() {
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Invoice ID"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Client"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Project"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Amount"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Status"));
        assertDoesNotThrow(() -> engine.getColumnIndex(driver, "Actions"));
    }

    @Test
    void columnMapping_correctOrder() {
        assertEquals(0, engine.getColumnIndex(driver, "Invoice ID"));
        assertEquals(1, engine.getColumnIndex(driver, "Client"));
        assertEquals(2, engine.getColumnIndex(driver, "Project"));
        assertEquals(3, engine.getColumnIndex(driver, "Amount"));
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
        WebElement row = engine.findRow(driver, "Invoice ID", "INV-003");
        assertNotNull(row);

        ((JavascriptExecutor) driver).executeScript(
                "var container = document.querySelector('.MuiDataGrid-virtualScrollerContent');" +
                "var html = container.innerHTML;" +
                "container.innerHTML = '';" +
                "container.innerHTML = html;");

        WebElement freshTable = driver.findElement(By.id("muiDataGridFixture"));
        TableEngine freshEngine = new MUITableEngine(freshTable, driver);
        WebElement refound = freshEngine.findRow(driver, "Invoice ID", "INV-003");
        assertNotNull(refound);
        assertEquals("Acme Corp", freshEngine.getCellValue(driver, refound, "Client"));
    }

    // === Performance ===

    @Test
    void performance_findRow_under3Seconds() {
        long start = System.currentTimeMillis();
        engine.findRow(driver, "Invoice ID", "INV-005");
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
        assertEquals(3, engine.getColumnIndex(driver, "Amount"));
    }

    @Test
    void gridHandler_backwardCompat_getCellValueByIndex() {
        assertEquals("INV-001", engine.getCellValue(driver, "Invoice ID", 0));
    }

    @Test
    void gridHandler_backwardCompat_getRow() {
        assertNotNull(engine.getRow(driver, 2));
    }
}
