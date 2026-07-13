package kroviq.wrapper.desktop;

import kroviq.wrapper.desktop.table.DesktopRow;
import kroviq.wrapper.desktop.table.DesktopTable;
import kroviq.wrapper.desktop.table.GenericDesktopTable;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GenericDesktopTableTest {

    private RemoteWebDriver mockDriver;
    private WebElement mockGridRoot;
    private DesktopTable table;

    // Test data: 3 columns, 5 rows
    private static final String[] COLUMNS = {"ID", "Name", "Status"};
    private static final String[][] DATA = {
        {"EMP-001", "Alice Brown", "Active"},
        {"EMP-002", "Bob Wilson", "Pending"},
        {"EMP-003", "Charlie Davis", "Active"},
        {"EMP-004", "Diana Evans", "Inactive"},
        {"EMP-005", "Edward Fox", "Active"}
    };

    @BeforeEach
    void setup() {
        mockDriver = mock(RemoteWebDriver.class);
        mockGridRoot = mock(WebElement.class);

        // Mock header cells
        List<WebElement> headerCells = new ArrayList<>();
        for (String col : COLUMNS) {
            WebElement header = mockCell(col);
            headerCells.add(header);
        }
        when(mockGridRoot.findElements(By.xpath(".//*[@LocalizedControlType='column header']")))
            .thenReturn(headerCells);

        // Mock data rows (all visible — no scrolling needed for basic tests)
        List<WebElement> rows = new ArrayList<>();
        for (String[] rowData : DATA) {
            WebElement row = mockRow(rowData);
            rows.add(row);
        }
        when(mockGridRoot.findElements(By.xpath(".//*[@LocalizedControlType='data item']")))
            .thenReturn(rows);

        table = new GenericDesktopTable(mockGridRoot, mockDriver);
    }

    @Test
    @Order(1)
    void findRow_byColumnValue_returnsRow() {
        DesktopRow row = table.findRow("Name", "Alice Brown");
        assertNotNull(row);
    }

    @Test
    @Order(2)
    void findRow_caseInsensitive_returnsRow() {
        DesktopRow row = table.findRow("Name", "alice brown");
        assertNotNull(row);
    }

    @Test
    @Order(3)
    void findRow_notFound_throwsException() {
        assertThrows(RuntimeException.class, () -> table.findRow("Name", "Nobody"));
    }

    @Test
    @Order(4)
    void getCellValue_returnsCorrectValue() {
        DesktopRow row = table.findRow("ID", "EMP-003");
        String name = table.getCellValue(row, "Name");
        assertEquals("Charlie Davis", name);
    }

    @Test
    @Order(5)
    void getCellValue_statusColumn() {
        DesktopRow row = table.findRow("Name", "Bob Wilson");
        String status = table.getCellValue(row, "Status");
        assertEquals("Pending", status);
    }

    @Test
    @Order(6)
    void getCellValue_invalidColumn_throwsException() {
        DesktopRow row = table.findRow("ID", "EMP-001");
        assertThrows(RuntimeException.class, () -> table.getCellValue(row, "NonExistent"));
    }

    @Test
    @Order(7)
    void getRowCount_returnsVisibleCount() {
        int count = table.getRowCount();
        assertEquals(5, count);
    }

    @Test
    @Order(8)
    void rowExists_true() {
        assertTrue(table.rowExists("ID", "EMP-004"));
    }

    @Test
    @Order(9)
    void rowExists_false() {
        assertFalse(table.rowExists("ID", "EMP-999"));
    }

    @Test
    @Order(10)
    void findRowByCriteria_singleColumn() {
        Map<String, String> criteria = Map.of("Status", "Pending");
        DesktopRow row = table.findRowByCriteria(criteria);
        assertNotNull(row);
        assertEquals("Bob Wilson", table.getCellValue(row, "Name"));
    }

    @Test
    @Order(11)
    void findRowByCriteria_multiColumn() {
        Map<String, String> criteria = new LinkedHashMap<>();
        criteria.put("Status", "Active");
        criteria.put("Name", "Charlie Davis");
        DesktopRow row = table.findRowByCriteria(criteria);
        assertNotNull(row);
        assertEquals("EMP-003", table.getCellValue(row, "ID"));
    }

    @Test
    @Order(12)
    void findRowByCriteria_notFound_throwsException() {
        Map<String, String> criteria = Map.of("Status", "Active", "Name", "Nobody");
        assertThrows(RuntimeException.class, () -> table.findRowByCriteria(criteria));
    }

    @Test
    @Order(13)
    void scrollToFind_discoversRowAfterScroll() {
        // Setup: first call returns only first 2 rows, after scroll returns all 5
        WebElement mockGrid = mock(WebElement.class);

        List<WebElement> headerCells = new ArrayList<>();
        for (String col : COLUMNS) {
            headerCells.add(mockCell(col));
        }
        when(mockGrid.findElements(By.xpath(".//*[@LocalizedControlType='column header']")))
            .thenReturn(headerCells);

        // First visible: rows 0-1, after scroll: rows 0-4
        List<WebElement> firstPage = List.of(mockRow(DATA[0]), mockRow(DATA[1]));
        List<WebElement> secondPage = List.of(
            mockRow(DATA[0]), mockRow(DATA[1]), mockRow(DATA[2]),
            mockRow(DATA[3]), mockRow(DATA[4])
        );

        when(mockGrid.findElements(By.xpath(".//*[@LocalizedControlType='data item']")))
            .thenReturn(firstPage)
            .thenReturn(secondPage);

        // sendKeys for scroll — just verify it's called
        doNothing().when(mockGrid).sendKeys(any(CharSequence.class));

        DesktopTable scrollTable = new GenericDesktopTable(mockGrid, mockDriver);
        DesktopRow row = scrollTable.findRow("Name", "Edward Fox");
        assertNotNull(row);
        assertEquals("EMP-005", scrollTable.getCellValue(row, "ID"));

        // Verify scroll was triggered
        verify(mockGrid, atLeastOnce()).sendKeys(Keys.PAGE_DOWN);
    }

    @Test
    @Order(14)
    void scrollToFind_stopsWhenNoNewRows() {
        WebElement mockGrid = mock(WebElement.class);

        List<WebElement> headerCells = new ArrayList<>();
        for (String col : COLUMNS) {
            headerCells.add(mockCell(col));
        }
        when(mockGrid.findElements(By.xpath(".//*[@LocalizedControlType='column header']")))
            .thenReturn(headerCells);

        // Always returns same rows — simulates bottom of grid
        List<WebElement> sameRows = List.of(mockRow(DATA[0]), mockRow(DATA[1]));
        when(mockGrid.findElements(By.xpath(".//*[@LocalizedControlType='data item']")))
            .thenReturn(sameRows);

        doNothing().when(mockGrid).sendKeys(any(CharSequence.class));

        DesktopTable scrollTable = new GenericDesktopTable(mockGrid, mockDriver);
        assertFalse(scrollTable.rowExists("Name", "Nobody"));
    }

    // --- Mock helpers ---

    private WebElement mockRow(String[] rowData) {
        WebElement row = mock(WebElement.class);
        // Mock Name attribute for row hash computation
        when(row.getAttribute("Name")).thenReturn(String.join(" ", rowData));

        List<WebElement> cells = new ArrayList<>();
        for (String cellValue : rowData) {
            cells.add(mockCell(cellValue));
        }
        when(row.findElements(By.xpath(".//*[@LocalizedControlType='text']")))
            .thenReturn(cells);

        return row;
    }

    private WebElement mockCell(String text) {
        WebElement cell = mock(WebElement.class);
        when(cell.getAttribute("Value.Value")).thenReturn(null);
        when(cell.getAttribute("Name")).thenReturn(text);
        when(cell.getText()).thenReturn(text);
        return cell;
    }
}
