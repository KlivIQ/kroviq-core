package kroviq.wrapper.integration;

import io.github.bonigarcia.wdm.WebDriverManager;
import kroviq.wrapper.core.AutoCompleteHandler;
import kroviq.wrapper.core.DialogHandler;
import kroviq.wrapper.core.DropdownHandler;
import kroviq.wrapper.core.GridHandler;
import kroviq.wrapper.factory.ComponentHandlerFactory;
import kroviq.wrapper.factory.UIFrameworkDetector;
import kroviq.wrapper.factory.UIFrameworkDetector.UIFramework;
import kroviq.wrapper.primeng.PrimeNGAutoCompleteHandler;
import kroviq.wrapper.primeng.PrimeNGDialogHandler;
import kroviq.wrapper.primeng.PrimeNGDropdownHandler;
import kroviq.wrapper.primeng.PrimeNGTableHandler;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PrimeNGLocalIntegrationTest {

    private static WebDriver driver;
    private static String fixtureUrl;
    private static final List<String> results = new ArrayList<>();
    private static int passed = 0;
    private static int failed = 0;

    @BeforeAll
    static void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");
        driver = new ChromeDriver(options);

        File fixture = new File("src/test/resources/primeng/primeng-fixture.html");
        fixtureUrl = "file:///" + fixture.getAbsolutePath().replace("\\", "/");
        driver.get(fixtureUrl);

        log("=== PrimeNG Local Integration Test Started ===");
        log("Loaded fixture: " + fixtureUrl);
        UIFrameworkDetector.clearCache();
    }

    @AfterAll
    static void tearDown() {
        log("\n=== SUMMARY ===");
        log("Passed: " + passed);
        log("Failed: " + failed);
        log("Total:  " + (passed + failed));
        if (failed == 0) log("Result: ALL PASS ✓");
        log("===============");
        results.forEach(System.out::println);

        if (driver != null) driver.quit();
        assertEquals(0, failed, failed + " test(s) failed");
    }

    // ==================== DETECTION TESTS ====================

    @Test @Order(1)
    void test01_DetectorIdentifiesPrimeNG_Dropdown() {
        try {
            WebElement element = driver.findElement(By.cssSelector("p-dropdown"));
            UIFramework detected = UIFrameworkDetector.detect(element);
            log("[Detector_p-dropdown] tag: " + element.getTagName() + " | class: " + element.getAttribute("class") + " | detected: " + detected);
            assertEquals(UIFramework.PRIMENG, detected);
            recordPass("Detector_p-dropdown");
        } catch (Exception e) { recordFail("Detector_p-dropdown", e); }
    }

    @Test @Order(2)
    void test02_DetectorIdentifiesPrimeNG_Table() {
        try {
            UIFrameworkDetector.clearCache();
            WebElement element = driver.findElement(By.cssSelector("p-table"));
            UIFramework detected = UIFrameworkDetector.detect(element);
            log("[Detector_p-table] tag: " + element.getTagName() + " | class: " + element.getAttribute("class") + " | detected: " + detected);
            assertEquals(UIFramework.PRIMENG, detected);
            recordPass("Detector_p-table");
        } catch (Exception e) { recordFail("Detector_p-table", e); }
    }

    @Test @Order(3)
    void test03_DetectorIdentifiesPrimeNG_Autocomplete() {
        try {
            UIFrameworkDetector.clearCache();
            WebElement element = driver.findElement(By.cssSelector("p-autocomplete"));
            UIFramework detected = UIFrameworkDetector.detect(element);
            log("[Detector_p-autocomplete] tag: " + element.getTagName() + " | class: " + element.getAttribute("class") + " | detected: " + detected);
            assertEquals(UIFramework.PRIMENG, detected);
            recordPass("Detector_p-autocomplete");
        } catch (Exception e) { recordFail("Detector_p-autocomplete", e); }
    }

    // ==================== FACTORY ROUTING TESTS ====================

    @Test @Order(4)
    void test04_FactoryReturnsPrimeNGDropdownHandler() {
        try {
            UIFrameworkDetector.clearCache();
            WebElement element = driver.findElement(By.cssSelector("p-dropdown"));
            DropdownHandler handler = ComponentHandlerFactory.getDropdownHandler(element, driver);
            log("[Factory_DropdownHandler] Handler: " + handler.getClass().getSimpleName());
            assertInstanceOf(PrimeNGDropdownHandler.class, handler);
            recordPass("Factory_DropdownHandler");
        } catch (Exception e) { recordFail("Factory_DropdownHandler", e); }
    }

    @Test @Order(5)
    void test05_FactoryReturnsPrimeNGAutoCompleteHandler() {
        try {
            UIFrameworkDetector.clearCache();
            WebElement element = driver.findElement(By.cssSelector("p-autocomplete"));
            AutoCompleteHandler handler = ComponentHandlerFactory.getAutoCompleteHandler(element);
            log("[Factory_AutoCompleteHandler] Handler: " + handler.getClass().getSimpleName());
            assertInstanceOf(PrimeNGAutoCompleteHandler.class, handler);
            recordPass("Factory_AutoCompleteHandler");
        } catch (Exception e) { recordFail("Factory_AutoCompleteHandler", e); }
    }

    @Test @Order(6)
    void test06_FactoryReturnsPrimeNGTableHandler() {
        try {
            UIFrameworkDetector.clearCache();
            WebElement element = driver.findElement(By.cssSelector("p-table"));
            GridHandler handler = ComponentHandlerFactory.getGridHandler(element, driver);
            log("[Factory_TableHandler] Handler: " + handler.getClass().getSimpleName());
            assertInstanceOf(PrimeNGTableHandler.class, handler);
            recordPass("Factory_TableHandler");
        } catch (Exception e) { recordFail("Factory_TableHandler", e); }
    }

    // ==================== DROPDOWN TESTS ====================

    @Test @Order(7)
    void test07_DropdownGetOptions() {
        try {
            driver.get(fixtureUrl);
            UIFrameworkDetector.clearCache();
            WebElement trigger = driver.findElement(By.cssSelector("#country-dropdown"));
            DropdownHandler handler = ComponentHandlerFactory.getDropdownHandler(trigger, driver);
            List<String> options = handler.getOptions(driver, trigger);
            log("[Dropdown_GetOptions] Options: " + options);
            assertTrue(options.contains("India"));
            assertTrue(options.contains("Germany"));
            assertEquals(5, options.size());
            recordPass("Dropdown_GetOptions");
        } catch (Exception e) { recordFail("Dropdown_GetOptions", e); }
    }

    @Test @Order(8)
    void test08_DropdownSelect() {
        try {
            driver.get(fixtureUrl);
            UIFrameworkDetector.clearCache();
            WebElement trigger = driver.findElement(By.cssSelector("#country-dropdown"));
            DropdownHandler handler = ComponentHandlerFactory.getDropdownHandler(trigger, driver);
            handler.select(driver, trigger, "Germany");
            String label = driver.findElement(By.id("dropdown-label")).getText();
            log("[Dropdown_Select] Selected: 'Germany' | Displayed: '" + label + "'");
            assertEquals("Germany", label);
            recordPass("Dropdown_Select");
        } catch (Exception e) { recordFail("Dropdown_Select", e); }
    }

    @Test @Order(9)
    void test09_DropdownSearchableFilter() {
        try {
            driver.get(fixtureUrl);
            UIFrameworkDetector.clearCache();
            WebElement trigger = driver.findElement(By.cssSelector("#searchable-dropdown"));
            DropdownHandler handler = ComponentHandlerFactory.getDropdownHandler(trigger, driver);
            handler.select(driver, trigger, "Tokyo");
            String label = driver.findElement(By.id("searchable-dropdown-label")).getText();
            log("[Dropdown_Searchable] Selected: 'Tokyo' | Displayed: '" + label + "'");
            assertEquals("Tokyo", label);
            recordPass("Dropdown_Searchable");
        } catch (Exception e) { recordFail("Dropdown_Searchable", e); }
    }

    // ==================== AUTOCOMPLETE TESTS ====================

    @Test @Order(10)
    void test10_AutocompleteGetSuggestions() {
        try {
            driver.get(fixtureUrl);
            UIFrameworkDetector.clearCache();
            WebElement input = driver.findElement(By.cssSelector("#language-autocomplete"));
            AutoCompleteHandler handler = ComponentHandlerFactory.getAutoCompleteHandler(input);
            List<String> suggestions = handler.getSuggestions(driver, input, "Java");
            log("[Autocomplete_GetSuggestions] Suggestions for 'Java': " + suggestions);
            assertTrue(suggestions.contains("Java"));
            assertTrue(suggestions.contains("JavaScript"));
            recordPass("Autocomplete_GetSuggestions");
        } catch (Exception e) { recordFail("Autocomplete_GetSuggestions", e); }
    }

    @Test @Order(11)
    void test11_AutocompleteSearchAndSelect() {
        try {
            driver.get(fixtureUrl);
            UIFrameworkDetector.clearCache();
            WebElement input = driver.findElement(By.cssSelector("#language-autocomplete"));
            AutoCompleteHandler handler = ComponentHandlerFactory.getAutoCompleteHandler(input);
            handler.searchAndSelect(driver, input, "Py", "Python");
            String value = driver.findElement(By.id("autocomplete-input")).getAttribute("value");
            log("[Autocomplete_SearchAndSelect] Input value after select: '" + value + "'");
            assertEquals("Python", value);
            recordPass("Autocomplete_SearchAndSelect");
        } catch (Exception e) { recordFail("Autocomplete_SearchAndSelect", e); }
    }

    // ==================== DIALOG TESTS ====================

    @Test @Order(12)
    void test12_DialogNotOpenInitially() {
        try {
            driver.get(fixtureUrl);
            UIFrameworkDetector.clearCache();
            WebElement dialogElement = driver.findElement(By.cssSelector("p-dialog"));
            DialogHandler handler = ComponentHandlerFactory.getDialogHandler(dialogElement);
            boolean isOpen = handler.isOpen(driver);
            log("[Dialog_Not_Open_Initially] Dialog open: " + isOpen);
            assertFalse(isOpen);
            recordPass("Dialog_Not_Open_Initially");
        } catch (Exception e) { recordFail("Dialog_Not_Open_Initially", e); }
    }

    @Test @Order(13)
    void test13_DialogOpenDetect() {
        try {
            driver.get(fixtureUrl);
            driver.findElement(By.id("open-dialog-btn")).click();
            Thread.sleep(300);
            UIFrameworkDetector.clearCache();
            WebElement dialogElement = driver.findElement(By.cssSelector("p-dialog"));
            DialogHandler handler = ComponentHandlerFactory.getDialogHandler(dialogElement);
            boolean isOpen = handler.isOpen(driver);
            log("[Dialog_Open_Detect] Dialog open after click: " + isOpen);
            assertTrue(isOpen);
            recordPass("Dialog_Open_Detect");
        } catch (Exception e) { recordFail("Dialog_Open_Detect", e); }
    }

    @Test @Order(14)
    void test14_DialogGetContent() {
        try {
            driver.get(fixtureUrl);
            driver.findElement(By.id("open-dialog-btn")).click();
            Thread.sleep(300);
            UIFrameworkDetector.clearCache();
            WebElement dialogElement = driver.findElement(By.cssSelector("p-dialog"));
            DialogHandler handler = ComponentHandlerFactory.getDialogHandler(dialogElement);
            WebElement content = handler.getDialogContent(driver);
            String text = content.getText();
            log("[Dialog_GetContent] Content: '" + text + "'");
            assertTrue(text.contains("Are you sure"));
            recordPass("Dialog_GetContent");
        } catch (Exception e) { recordFail("Dialog_GetContent", e); }
    }

    @Test @Order(15)
    void test15_DialogClickAction() {
        try {
            driver.get(fixtureUrl);
            driver.findElement(By.id("open-dialog-btn")).click();
            Thread.sleep(300);
            UIFrameworkDetector.clearCache();
            WebElement dialogElement = driver.findElement(By.cssSelector("p-dialog"));
            DialogHandler handler = ComponentHandlerFactory.getDialogHandler(dialogElement);
            handler.clickAction(driver, "Confirm");
            Thread.sleep(300);
            boolean isOpen = handler.isOpen(driver);
            String result = driver.findElement(By.id("result")).getText();
            log("[Dialog_ClickAction] Dialog open after Confirm: " + isOpen);
            log("[Dialog_ClickAction] Result text: '" + result + "'");
            assertFalse(isOpen);
            assertEquals("Action confirmed!", result);
            recordPass("Dialog_ClickAction");
        } catch (Exception e) { recordFail("Dialog_ClickAction", e); }
    }

    @Test @Order(16)
    void test16_DialogCloseEscape() {
        try {
            driver.get(fixtureUrl);
            driver.findElement(By.id("open-dialog-btn")).click();
            Thread.sleep(300);
            UIFrameworkDetector.clearCache();
            WebElement dialogElement = driver.findElement(By.cssSelector("p-dialog"));
            DialogHandler handler = ComponentHandlerFactory.getDialogHandler(dialogElement);
            assertTrue(handler.isOpen(driver));
            handler.close(driver);
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> !handler.isOpen(d));
            log("[Dialog_Close_Escape] Dialog open after close: " + handler.isOpen(driver));
            assertFalse(handler.isOpen(driver));
            recordPass("Dialog_Close_Escape");
        } catch (Exception e) { recordFail("Dialog_Close_Escape", e); }
    }

    // ==================== TABLE TESTS ====================

    @Test @Order(17)
    void test17_TableColumnLookup() {
        try {
            driver.get(fixtureUrl);
            UIFrameworkDetector.clearCache();
            WebElement table = driver.findElement(By.cssSelector("p-table"));
            GridHandler handler = ComponentHandlerFactory.getGridHandler(table, driver);
            int idIdx = handler.getColumnIndex(driver, "Id");
            int nameIdx = handler.getColumnIndex(driver, "Name");
            int countryIdx = handler.getColumnIndex(driver, "Country");
            int companyIdx = handler.getColumnIndex(driver, "Company");
            log("[Table_Column_Lookup] Id=" + idIdx + " Name=" + nameIdx + " Country=" + countryIdx + " Company=" + companyIdx);
            assertEquals(0, idIdx);
            assertEquals(1, nameIdx);
            assertEquals(2, countryIdx);
            assertEquals(3, companyIdx);
            recordPass("Table_Column_Lookup");
        } catch (Exception e) { recordFail("Table_Column_Lookup", e); }
    }

    @Test @Order(18)
    void test18_TableCellRead() {
        try {
            driver.get(fixtureUrl);
            UIFrameworkDetector.clearCache();
            WebElement table = driver.findElement(By.cssSelector("p-table"));
            GridHandler handler = ComponentHandlerFactory.getGridHandler(table, driver);
            String id = handler.getCellValue(driver, "Id", 0);
            String name = handler.getCellValue(driver, "Name", 0);
            String country = handler.getCellValue(driver, "Country", 2);
            log("[Table_Cell_Read] Cell[Id,0]='" + id + "' Cell[Name,0]='" + name + "' Cell[Country,2]='" + country + "'");
            assertEquals("1", id);
            assertEquals("James Butt", name);
            assertEquals("Panama", country);
            recordPass("Table_Cell_Read");
        } catch (Exception e) { recordFail("Table_Cell_Read", e); }
    }

    @Test @Order(19)
    void test19_TableCellClick() {
        try {
            driver.get(fixtureUrl);
            UIFrameworkDetector.clearCache();
            WebElement table = driver.findElement(By.cssSelector("p-table"));
            GridHandler handler = ComponentHandlerFactory.getGridHandler(table, driver);
            handler.clickCell(driver, "Name", 1);
            log("[Table_Cell_Click] Clicked cell[Name, row 1] successfully");
            recordPass("Table_Cell_Click");
        } catch (Exception e) { recordFail("Table_Cell_Click", e); }
    }

    @Test @Order(20)
    void test20_TableRowSelection() {
        try {
            driver.get(fixtureUrl);
            UIFrameworkDetector.clearCache();
            WebElement table = driver.findElement(By.cssSelector("p-table"));
            GridHandler handler = ComponentHandlerFactory.getGridHandler(table, driver);
            handler.selectRow(driver, 2);
            WebElement row = handler.getRow(driver, 2);
            String rowClass = row.getAttribute("class");
            log("[Table_Row_Selection] Row 2 class after select: '" + rowClass + "'");
            assertTrue(rowClass.contains("p-highlight"));
            recordPass("Table_Row_Selection");
        } catch (Exception e) { recordFail("Table_Row_Selection", e); }
    }

    @Test @Order(21)
    void test21_TablePagination() {
        try {
            driver.get(fixtureUrl);
            UIFrameworkDetector.clearCache();
            WebElement table = driver.findElement(By.cssSelector("p-table"));
            PrimeNGTableHandler handler = (PrimeNGTableHandler) ComponentHandlerFactory.getGridHandler(table, driver);

            // Page 1: first row should be "James Butt"
            String page1Name = handler.getCellValue(driver, "Name", 0);
            log("[Table_Pagination] Page 1, row 0 Name: '" + page1Name + "'");
            assertEquals("James Butt", page1Name);

            // Go to page 2
            handler.nextPage(driver);
            String page2Name = handler.getCellValue(driver, "Name", 0);
            log("[Table_Pagination] Page 2, row 0 Name: '" + page2Name + "'");
            assertEquals("Minna Amigon", page2Name);

            // Go to page 3
            handler.goToPage(driver, 3);
            String page3Name = handler.getCellValue(driver, "Name", 0);
            log("[Table_Pagination] Page 3, row 0 Name: '" + page3Name + "'");
            assertNotNull(page3Name);
            assertFalse(page3Name.isEmpty());
            // Verify it's different from page 1 and page 2
            assertNotEquals(page1Name, page3Name);
            assertNotEquals(page2Name, page3Name);

            recordPass("Table_Pagination");
        } catch (Exception e) { recordFail("Table_Pagination", e); }
    }

    @Test @Order(22)
    void test22_TableGetRow() {
        try {
            driver.get(fixtureUrl);
            UIFrameworkDetector.clearCache();
            WebElement table = driver.findElement(By.cssSelector("p-table"));
            GridHandler handler = ComponentHandlerFactory.getGridHandler(table, driver);
            WebElement row0 = handler.getRow(driver, 0);
            WebElement row4 = handler.getRow(driver, 4);
            log("[Table_GetRow] Row 0 tag: " + row0.getTagName() + " | Row 4 tag: " + row4.getTagName());
            assertEquals("tr", row0.getTagName());
            assertEquals("tr", row4.getTagName());
            recordPass("Table_GetRow");
        } catch (Exception e) { recordFail("Table_GetRow", e); }
    }

    // ==================== HELPERS ====================

    private void recordPass(String testName) {
        passed++;
        log("[PASS] " + testName);
    }

    private void recordFail(String testName, Exception e) {
        failed++;
        String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
        if (msg.length() > 200) msg = msg.substring(0, 200);
        log("[FAIL] " + testName + "  " + msg);
        fail(testName + " failed: " + e.getMessage());
    }

    private static void log(String msg) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        String line = "[" + timestamp + "] " + msg;
        System.out.println(line);
        results.add(line);
    }
}
