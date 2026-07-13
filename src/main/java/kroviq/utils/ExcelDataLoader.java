package kroviq.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

/**
 * Shared Excel parsing logic used by both TestDataManager (runtime) and ExcelToJsonMigrator (CLI).
 * Single source of truth — do NOT duplicate this logic elsewhere.
 */
public class ExcelDataLoader {

    /**
     * Parsed result for a single Excel sheet (module).
     */
    public static class ModuleData {
        public final String moduleName;
        public final Map<String, List<Map<String, String>>> testCaseRows;

        public ModuleData(String moduleName, Map<String, List<Map<String, String>>> testCaseRows) {
            this.moduleName = moduleName;
            this.testCaseRows = testCaseRows;
        }
    }

    /**
     * Parse all sheets from an Excel file into ModuleData list.
     * Applies: header extraction, TestCaseID grouping, fill-down, Page-suffix stripping.
     */
    public static List<ModuleData> parseExcelFile(File excelFile) {
        if (excelFile == null || !excelFile.exists()) {
            throw new FatalFrameworkException("[ExcelDataLoader] File not found: " +
                    (excelFile != null ? excelFile.getAbsolutePath() : "null"));
        }

        List<ModuleData> results = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Set<String> rawSheetNames = new LinkedHashSet<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                rawSheetNames.add(workbook.getSheetAt(i).getSheetName());
            }

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                ModuleData moduleData = parseSheet(sheet, rawSheetNames);
                if (moduleData != null) {
                    results.add(moduleData);
                }
            }
        } catch (FatalFrameworkException e) {
            throw e;
        } catch (Exception e) {
            throw new FatalFrameworkException("[ExcelDataLoader] Failed to read Excel file '" +
                    excelFile.getAbsolutePath() + "': " + e.getMessage(), e);
        }

        return results;
    }

    /**
     * Parse a single sheet into ModuleData. Returns null if sheet is empty/invalid.
     */
    static ModuleData parseSheet(Sheet sheet, Set<String> allSheetNames) {
        String sheetName = sheet.getSheetName();

        if (sheet.getLastRowNum() < 1) return null;

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return null;

        Map<Integer, String> headers = extractHeaders(headerRow);
        if (headers.isEmpty()) return null;

        // Resolve module name: strip "Page" suffix only if no collision
        String moduleName = sheetName;
        if (sheetName.endsWith("Page")) {
            String stripped = sheetName.substring(0, sheetName.length() - 4);
            if (!allSheetNames.contains(stripped)) {
                moduleName = stripped;
            }
        }

        Map<String, List<Map<String, String>>> tcRows = groupRowsByTestCaseId(sheet, headers);
        if (tcRows.isEmpty()) return null;

        return new ModuleData(moduleName, tcRows);
    }

    /**
     * Extract column headers from row 0.
     */
    private static Map<Integer, String> extractHeaders(Row headerRow) {
        Map<Integer, String> headers = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            if (cell != null) {
                String val = getCellValueAsString(cell).trim();
                if (!val.isEmpty()) {
                    headers.put(cell.getColumnIndex(), val);
                }
            }
        }
        return headers;
    }

    /**
     * Group data rows by TestCaseID with fill-down support.
     */
    private static Map<String, List<Map<String, String>>> groupRowsByTestCaseId(Sheet sheet, Map<Integer, String> headers) {
        Map<String, List<Map<String, String>>> tcRows = new LinkedHashMap<>();
        String lastTestCaseId = null;

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            Map<String, String> rowData = new LinkedHashMap<>();
            String testCaseId = null;

            for (Map.Entry<Integer, String> h : headers.entrySet()) {
                Cell cell = row.getCell(h.getKey());
                String value = getCellValueAsString(cell);
                String colName = h.getValue();

                if ("TestCaseID".equals(colName)) {
                    testCaseId = value;
                } else if (!value.isEmpty()) {
                    rowData.put(colName, value);
                }
            }

            // Fill-down: empty TestCaseID with data → belongs to previous TC
            if ((testCaseId == null || testCaseId.isEmpty()) && !rowData.isEmpty()) {
                testCaseId = lastTestCaseId;
            }
            if (testCaseId != null && !testCaseId.isEmpty()) {
                lastTestCaseId = testCaseId;
            }

            if (testCaseId != null && !testCaseId.isEmpty() && !rowData.isEmpty()) {
                tcRows.computeIfAbsent(testCaseId, k -> new ArrayList<>()).add(rowData);
            }
        }

        return tcRows;
    }

    /**
     * Convert a cell value to String. Handles STRING, NUMERIC, BOOLEAN, FORMULA types.
     * This is the single source of truth — used by both runtime loading and migration.
     */
    public static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue(); }
                catch (Exception e) {
                    try { return String.valueOf((long) cell.getNumericCellValue()); }
                    catch (Exception e2) { return ""; }
                }
            default:
                return "";
        }
    }
}
