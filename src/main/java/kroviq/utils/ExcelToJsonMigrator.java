package kroviq.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

// ===== ONE-TIME MIGRATION TOOL — NOT USED IN FRAMEWORK RUNTIME =====
//
// Converts Excel test data sheets (.xlsx) to flat JSON files.
// Run manually when migrating data. Not called by any framework code.
// No runtime references — never loaded by TestDataManager or any hook.
//
// Uses ExcelDataLoader for core parsing logic (single source of truth).
//
// --- Usage ---
//   mvn exec:java -Dexec.mainClass=kroviq.utils.ExcelToJsonMigrator \
//       "-Dexec.args=--excel <path> --outdir <path>" -Dexec.classpathScope=compile
//
//   CLI options:
//     (no args)         Convert ALL sheets using default relative paths.
//     <sheetName>       Convert only the named sheet.
//     --excel <path>    Source Excel file (default: TestDatastore/Testdata.xlsx).
//     --outdir <path>   Output directory for JSON (default: TestDatastore/json).
//     --list            List sheet names and row counts only; no conversion.
//
// --- Output structure ---
//   One JSON file per sheet. Each file contains:
//     { "_module": "<moduleName>", "<TestCaseID>": { ... }, ... }
//   Single-row TCs → flat object.  Multi-row TCs → JSON array (iterations).
//
// --- Key behaviours ---
//   Fill-down:  Rows with data but an empty TestCaseID cell inherit the
//               TestCaseID from the nearest preceding non-empty row.
//
//   Page-suffix collision guard:  Sheet names ending in "Page" have the
//               suffix stripped for the JSON filename *only if* no other
//               sheet already uses the stripped name.
//
//   FORMULA cells:  Evaluated as STRING first, then NUMERIC fallback.
//
// --- Changelog ---
//   v1.0  Initial version — basic sheet-to-JSON, single TestCaseID per row.
//   v1.1  Fill-down, Page-suffix collision, CLI args, FORMULA support.
//   v1.2  Refactored to use ExcelDataLoader (shared parsing logic).
// ====================================================================
public class ExcelToJsonMigrator {

    private static final String DEFAULT_EXCEL = "TestDatastore/Testdata.xlsx";
    private static final String DEFAULT_JSON  = "TestDatastore/json";

    public static void main(String[] args) throws Exception {
        String excelPath = DEFAULT_EXCEL;
        String jsonPath  = DEFAULT_JSON;
        String targetSheet = null;
        boolean listOnly = false;

        for (int a = 0; a < args.length; a++) {
            switch (args[a]) {
                case "--excel":  excelPath = args[++a]; break;
                case "--outdir": jsonPath  = args[++a]; break;
                case "--list":   listOnly  = true;      break;
                default:         targetSheet = args[a];  break;
            }
        }

        File excelFile = new File(excelPath);
        if (!excelFile.exists()) {
            System.err.println("Excel file not found: " + excelFile.getAbsolutePath());
            System.exit(1);
        }

        if (listOnly) {
            listSheets(excelFile);
            return;
        }

        File jsonDir = new File(jsonPath);
        if (!jsonDir.exists()) jsonDir.mkdirs();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        int converted = 0;

        // Use shared parser for full-file parsing when no target sheet filter
        if (targetSheet == null) {
            List<ExcelDataLoader.ModuleData> modules = ExcelDataLoader.parseExcelFile(excelFile);
            for (ExcelDataLoader.ModuleData module : modules) {
                File outFile = writeModuleToJson(mapper, jsonDir, module);
                int iterCount = countIterations(module);
                String iterInfo = iterCount > 0 ? ", " + iterCount + " iterations" : "";
                System.out.printf("OK: %s → %s (%d TCs%s)%n",
                        module.moduleName, outFile.getName(), module.testCaseRows.size(), iterInfo);
                converted++;
            }
        } else {
            // Single-sheet mode: parse only the target sheet
            try (FileInputStream fis = new FileInputStream(excelFile);
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Set<String> rawSheetNames = new LinkedHashSet<>();
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    rawSheetNames.add(workbook.getSheetAt(i).getSheetName());
                }

                Sheet sheet = workbook.getSheet(targetSheet);
                if (sheet == null) {
                    System.err.println("Sheet not found: " + targetSheet);
                    System.exit(1);
                }

                ExcelDataLoader.ModuleData module = ExcelDataLoader.parseSheet(sheet, rawSheetNames);
                if (module == null) {
                    System.out.println("SKIP: " + targetSheet + " (empty or no valid rows)");
                } else {
                    File outFile = writeModuleToJson(mapper, jsonDir, module);
                    int iterCount = countIterations(module);
                    String iterInfo = iterCount > 0 ? ", " + iterCount + " iterations" : "";
                    System.out.printf("OK: %s → %s (%d TCs%s)%n",
                            module.moduleName, outFile.getName(), module.testCaseRows.size(), iterInfo);
                    converted++;
                }
            }
        }

        System.out.printf("%nDone. %d sheet(s) converted to %s%n", converted, jsonDir.getAbsolutePath());
    }

    private static File writeModuleToJson(ObjectMapper mapper, File jsonDir, ExcelDataLoader.ModuleData module) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("_module", module.moduleName);

        for (Map.Entry<String, List<Map<String, String>>> entry : module.testCaseRows.entrySet()) {
            String tcId = entry.getKey();
            List<Map<String, String>> rows = entry.getValue();

            if (rows.size() == 1) {
                ObjectNode tcNode = mapper.createObjectNode();
                for (Map.Entry<String, String> field : rows.get(0).entrySet()) {
                    tcNode.put(field.getKey(), field.getValue());
                }
                root.set(tcId, tcNode);
            } else {
                ArrayNode arr = mapper.createArrayNode();
                for (Map<String, String> row : rows) {
                    ObjectNode iterNode = mapper.createObjectNode();
                    for (Map.Entry<String, String> field : row.entrySet()) {
                        iterNode.put(field.getKey(), field.getValue());
                    }
                    arr.add(iterNode);
                }
                root.set(tcId, arr);
            }
        }

        File outFile = new File(jsonDir, module.moduleName + ".json");
        mapper.writeValue(outFile, root);
        return outFile;
    }

    private static int countIterations(ExcelDataLoader.ModuleData module) {
        int count = 0;
        for (List<Map<String, String>> rows : module.testCaseRows.values()) {
            if (rows.size() > 1) count += rows.size();
        }
        return count;
    }

    private static void listSheets(File excelFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            System.out.println("Sheets in: " + excelFile.getAbsolutePath());
            System.out.println("Total: " + workbook.getNumberOfSheets());
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                System.out.printf("  [%d] %s (rows: %d)%n", i, sheet.getSheetName(), sheet.getLastRowNum());
            }
        }
    }
}
