package kroviq.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

// ===== BIDIRECTIONAL MIGRATION TOOL — JSON → Excel =====
//
// Converts JSON test data files back to Excel format.
// Preserves the exact Excel contract used by ExcelDataLoader:
//   - 1 sheet per module (sheet name = module name)
//   - Row 0 = headers (TestCaseID + all field names)
//   - Single-row TCs → one row with TestCaseID
//   - Multi-row TCs (iterations) → TestCaseID on first row, blank on subsequent rows (fill-down)
//
// Round-trip guarantee:
//   JSON → Excel → JSON must produce identical data.
//   Excel → JSON → Excel must produce identical data.
//
// --- Usage ---
//   mvn exec:java -Dexec.mainClass=kroviq.utils.JsonToExcelMigrator \
//       "-Dexec.args=--jsondir <path> --output <path>" -Dexec.classpathScope=compile
//
//   CLI options:
//     (no args)           Convert all JSON files using default paths.
//     --jsondir <path>    Source JSON directory (default: TestDatastore/json).
//     --output <path>     Output Excel file (default: TestDatastore/Testdata_generated.xlsx).
//     <moduleName>        Convert only the named module.
//
// ====================================================================
public class JsonToExcelMigrator {

    private static final String DEFAULT_JSON_DIR = "TestDatastore/json";
    private static final String DEFAULT_OUTPUT = "TestDatastore/Testdata_generated.xlsx";

    public static void main(String[] args) throws Exception {
        String jsonDir = DEFAULT_JSON_DIR;
        String outputPath = DEFAULT_OUTPUT;
        String targetModule = null;

        for (int a = 0; a < args.length; a++) {
            switch (args[a]) {
                case "--jsondir": jsonDir = args[++a]; break;
                case "--output": outputPath = args[++a]; break;
                default: targetModule = args[a]; break;
            }
        }

        File jsonDirFile = new File(jsonDir);
        if (!jsonDirFile.exists() || !jsonDirFile.isDirectory()) {
            System.err.println("JSON directory not found: " + jsonDirFile.getAbsolutePath());
            System.exit(1);
        }

        File[] jsonFiles = jsonDirFile.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.err.println("No JSON files found in: " + jsonDirFile.getAbsolutePath());
            System.exit(1);
        }

        generateExcel(jsonFiles, outputPath, targetModule);
    }

    /**
     * Generate Excel workbook from JSON files.
     * Public API for programmatic use (e.g., from tests).
     */
    public static void generateExcel(File[] jsonFiles, String outputPath, String targetModule) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        try (Workbook workbook = new XSSFWorkbook()) {
            int converted = 0;

            for (File jsonFile : jsonFiles) {
                String moduleName = jsonFile.getName().replace(".json", "");
                if (targetModule != null && !targetModule.equals(moduleName)) continue;

                JsonNode root = mapper.readTree(jsonFile);
                if (root == null || root.isEmpty()) {
                    System.out.println("SKIP: " + jsonFile.getName() + " (empty)");
                    continue;
                }

                // Use _module field if present, otherwise filename
                String sheetName = root.has("_module") ? root.get("_module").asText() : moduleName;

                // Collect all TCs and their data
                List<TcEntry> entries = new ArrayList<>();
                Set<String> allFields = new LinkedHashSet<>();

                Iterator<String> fieldNames = root.fieldNames();
                while (fieldNames.hasNext()) {
                    String tcId = fieldNames.next();
                    if (tcId.startsWith("_")) continue;

                    JsonNode tcNode = root.get(tcId);

                    if (tcNode.isArray()) {
                        // Iterations — multiple rows
                        for (int i = 0; i < tcNode.size(); i++) {
                            JsonNode iterNode = tcNode.get(i);
                            if (iterNode.isObject()) {
                                Map<String, String> rowData = flattenToMap(iterNode);
                                allFields.addAll(rowData.keySet());
                                entries.add(new TcEntry(tcId, rowData, i > 0));
                            }
                        }
                    } else if (tcNode.isObject()) {
                        // Single row
                        Map<String, String> rowData = flattenToMap(tcNode);
                        allFields.addAll(rowData.keySet());
                        entries.add(new TcEntry(tcId, rowData, false));
                    }
                }

                if (entries.isEmpty()) {
                    System.out.println("SKIP: " + sheetName + " (no valid TCs)");
                    continue;
                }

                // Create sheet
                Sheet sheet = workbook.createSheet(sheetName);

                // Write header row: TestCaseID + all fields
                List<String> headers = new ArrayList<>();
                headers.add("TestCaseID");
                headers.addAll(allFields);

                Row headerRow = sheet.createRow(0);
                for (int col = 0; col < headers.size(); col++) {
                    headerRow.createCell(col).setCellValue(headers.get(col));
                }

                // Write data rows
                int rowIdx = 1;
                for (TcEntry entry : entries) {
                    Row row = sheet.createRow(rowIdx++);

                    // TestCaseID: blank for fill-down rows (continuation of same TC)
                    if (!entry.isContinuation) {
                        row.createCell(0).setCellValue(entry.tcId);
                    }

                    // Field values
                    for (int col = 1; col < headers.size(); col++) {
                        String fieldName = headers.get(col);
                        String value = entry.data.get(fieldName);
                        if (value != null && !value.isEmpty()) {
                            row.createCell(col).setCellValue(value);
                        }
                    }
                }

                int iterCount = (int) entries.stream().filter(e -> e.isContinuation).count();
                String iterInfo = iterCount > 0 ? ", " + (iterCount + countDistinctIteratedTCs(entries)) + " iteration rows" : "";
                System.out.printf("OK: %s → sheet '%s' (%d TCs, %d rows%s)%n",
                        jsonFile.getName(), sheetName, countDistinctTCs(entries), entries.size(), iterInfo);
                converted++;
            }

            if (converted == 0) {
                System.err.println("No modules converted.");
                System.exit(1);
            }

            // Write workbook
            File outputFile = new File(outputPath);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }

            System.out.printf("%nDone. %d module(s) written to %s%n", converted, outputFile.getAbsolutePath());
        }
    }

    private static Map<String, String> flattenToMap(JsonNode obj) {
        Map<String, String> result = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (entry.getKey().startsWith("_")) continue;
            JsonNode value = entry.getValue();
            if (value.isValueNode()) {
                String strVal = nodeToString(value);
                if (!strVal.isEmpty()) {
                    result.put(entry.getKey(), strVal);
                }
            }
            // Note: nested objects in JSON are NOT written to Excel (Excel is flat).
            // Only root-level primitives are preserved in round-trip.
        }
        return result;
    }

    private static String nodeToString(JsonNode node) {
        if (node == null || node.isNull()) return "";
        if (node.isTextual()) return node.asText();
        if (node.isIntegralNumber()) return String.valueOf(node.asLong());
        if (node.isNumber()) return String.valueOf(node.asDouble());
        if (node.isBoolean()) return String.valueOf(node.asBoolean());
        return node.asText();
    }

    private static int countDistinctTCs(List<TcEntry> entries) {
        return (int) entries.stream().filter(e -> !e.isContinuation).count();
    }

    private static int countDistinctIteratedTCs(List<TcEntry> entries) {
        Set<String> iterated = new HashSet<>();
        for (TcEntry e : entries) {
            if (e.isContinuation) iterated.add(e.tcId);
        }
        return iterated.size();
    }

    private static class TcEntry {
        final String tcId;
        final Map<String, String> data;
        final boolean isContinuation; // true = fill-down row (blank TestCaseID)

        TcEntry(String tcId, Map<String, String> data, boolean isContinuation) {
            this.tcId = tcId;
            this.data = data;
            this.isContinuation = isContinuation;
        }
    }
}
