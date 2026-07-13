package kroviq.utils;

import org.junit.jupiter.api.*;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Data Source Parity Validation Test.
 *
 * Proves that JSON mode and Excel mode produce IDENTICAL runtime behavior
 * using a project's actual TestDatastore (Testdata.xlsx + json/ folder).
 *
 * This test does NOT require a running application or WebDriver.
 * It validates the data loading layer in isolation.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataSourceParityTest {

    private static final String TEST_DATASTORE = "../test-project/TestDatastore";
    private static final String JSON_DIR = TEST_DATASTORE + "/json";
    private static final String GENERATED_EXCEL = TEST_DATASTORE + "/Testdata_generated.xlsx";

    private static Map<String, Map<String, String>> jsonTestData;
    private static Map<String, List<Map<String, String>>> jsonIterationData;
    private static Set<String> jsonModules;

    private static Map<String, Map<String, String>> excelTestData;
    private static Map<String, List<Map<String, String>>> excelIterationData;
    private static Set<String> excelModules;

    private static long jsonLoadTimeMs;
    private static long excelLoadTimeMs;

    @BeforeAll
    static void loadBothSources() {
        // Skip if test data not available
        Assumptions.assumeTrue(new File(JSON_DIR).exists(), "Test JSON dir not found — skipping parity test");

        // Step 1: Generate fresh Excel from current JSON
        File jsonDir = new File(JSON_DIR);
        File[] jsonFiles = jsonDir.listFiles((dir, name) -> name.endsWith(".json"));
        Assumptions.assumeTrue(jsonFiles != null && jsonFiles.length > 0, "No JSON files found");

        try {
            JsonToExcelMigrator.generateExcel(jsonFiles, GENERATED_EXCEL, null);
        } catch (Exception e) {
            fail("Failed to generate Excel from JSON: " + e.getMessage());
        }
        Assumptions.assumeTrue(new File(GENERATED_EXCEL).exists(), "Generated Excel not created");

        // Step 2: Load JSON
        long jsonStart = System.currentTimeMillis();
        LoadResult jsonResult = loadFromJson(jsonDir);
        jsonLoadTimeMs = System.currentTimeMillis() - jsonStart;
        jsonTestData = jsonResult.testData;
        jsonIterationData = jsonResult.iterationData;
        jsonModules = jsonResult.modules;

        // Step 3: Load generated Excel
        long excelStart = System.currentTimeMillis();
        LoadResult excelResult = loadFromExcel(new File(GENERATED_EXCEL));
        excelLoadTimeMs = System.currentTimeMillis() - excelStart;
        excelTestData = excelResult.testData;
        excelIterationData = excelResult.iterationData;
        excelModules = excelResult.modules;

        System.out.println("\n" + "=".repeat(100));
        System.out.println("DATA SOURCE PARITY VALIDATION");
        System.out.println("=".repeat(100));
        System.out.printf("JSON: %d modules | %d TCs | %dms%n", jsonModules.size(), jsonTestData.size(), jsonLoadTimeMs);
        System.out.printf("Excel: %d modules | %d TCs | %dms%n", excelModules.size(), excelTestData.size(), excelLoadTimeMs);
        System.out.println("=".repeat(100) + "\n");
    }

    // ========== 1. TD RESOLUTION PARITY ==========

    @Test
    @Order(1)
    @DisplayName("1. TD Resolution — Login.TC_LOGIN_001 fields match")
    void tdResolution_LoginFields() {
        String key = "Login.TC_LOGIN_001";
        Map<String, String> jsonData = jsonTestData.get(key);
        Map<String, String> excelData = excelTestData.get(key);

        assertNotNull(jsonData, "JSON missing key: " + key);
        assertNotNull(excelData, "Excel missing key: " + key);

        assertEquals(jsonData.get("Username"), excelData.get("Username"),
                "TD_Username mismatch");
        assertEquals(jsonData.get("Password"), excelData.get("Password"),
                "TD_Password mismatch");

        System.out.println("[PASS] TD Resolution — Login fields identical");
        System.out.printf("  JSON: Username=%s, Password=%s%n", jsonData.get("Username"), jsonData.get("Password"));
        System.out.printf("  Excel: Username=%s, Password=%s%n", excelData.get("Username"), excelData.get("Password"));
    }

    @Test
    @Order(2)
    @DisplayName("1b. TD Resolution — ProductConfig flat TC fields match")
    void tdResolution_ProductConfigFields() {
        String key = "ProductConfig.TC_PRODUCT_CONFIG_PRO_COMMON_001";
        Map<String, String> jsonData = jsonTestData.get(key);
        Map<String, String> excelData = excelTestData.get(key);

        assertNotNull(jsonData, "JSON missing key: " + key);
        assertNotNull(excelData, "Excel missing key: " + key);

        // Compare all fields
        for (String field : jsonData.keySet()) {
            String jsonVal = jsonData.get(field);
            String excelVal = excelData.get(field);
            assertEquals(jsonVal, excelVal,
                    "Field '" + field + "' mismatch in " + key);
        }

        System.out.printf("[PASS] TD Resolution — %s: %d fields all match%n", key, jsonData.size());
    }

    // ========== 2. ITERATION HANDLING ==========

    @Test
    @Order(3)
    @DisplayName("2. Iteration Count — multi-row TCs have same iteration count")
    void iterationCount_Match() {
        int mismatches = 0;
        int checked = 0;

        for (String key : jsonIterationData.keySet()) {
            List<Map<String, String>> jsonIters = jsonIterationData.get(key);
            List<Map<String, String>> excelIters = excelIterationData.get(key);

            if (jsonIters.size() > 1) {
                checked++;
                if (excelIters == null) {
                    System.err.printf("  [MISS] %s: JSON has %d iterations, Excel has NONE%n", key, jsonIters.size());
                    mismatches++;
                } else if (jsonIters.size() != excelIters.size()) {
                    System.err.printf("  [MISMATCH] %s: JSON=%d, Excel=%d%n", key, jsonIters.size(), excelIters.size());
                    mismatches++;
                }
            }
        }

        System.out.printf("[%s] Iteration Count — checked %d multi-row TCs, %d mismatches%n",
                mismatches == 0 ? "PASS" : "FAIL", checked, mismatches);
        assertEquals(0, mismatches, "Iteration count mismatches found");
    }

    @Test
    @Order(4)
    @DisplayName("2b. Iteration Values — Rating.TC_Rating_Charge_001 iterations match")
    void iterationValues_RatingCharge() {
        String key = "Rating.TC_Rating_Charge_001";
        List<Map<String, String>> jsonIters = jsonIterationData.get(key);
        List<Map<String, String>> excelIters = excelIterationData.get(key);

        assertNotNull(jsonIters, "JSON missing iterations for: " + key);
        assertNotNull(excelIters, "Excel missing iterations for: " + key);
        assertEquals(jsonIters.size(), excelIters.size(),
                "Iteration count mismatch for " + key);

        for (int i = 0; i < jsonIters.size(); i++) {
            Map<String, String> jsonIter = jsonIters.get(i);
            Map<String, String> excelIter = excelIters.get(i);

            for (String field : jsonIter.keySet()) {
                assertEquals(jsonIter.get(field), excelIter.get(field),
                        String.format("Iteration %d, field '%s' mismatch in %s", i, field, key));
            }
        }

        System.out.printf("[PASS] Iteration Values — %s: %d iterations, all fields match%n", key, jsonIters.size());
    }

    @Test
    @Order(5)
    @DisplayName("2c. Iteration Order — first iteration populates base testData")
    void iterationOrder_FirstIterationIsBase() {
        for (String key : jsonIterationData.keySet()) {
            List<Map<String, String>> jsonIters = jsonIterationData.get(key);
            if (jsonIters.size() > 1) {
                Map<String, String> jsonBase = jsonTestData.get(key);
                Map<String, String> excelBase = excelTestData.get(key);

                assertNotNull(jsonBase, "JSON base data missing for iterated TC: " + key);
                assertNotNull(excelBase, "Excel base data missing for iterated TC: " + key);

                // Base should equal first iteration
                assertEquals(jsonIters.get(0), jsonBase,
                        "JSON base != first iteration for " + key);

                List<Map<String, String>> excelIters = excelIterationData.get(key);
                if (excelIters != null && !excelIters.isEmpty()) {
                    assertEquals(excelIters.get(0), excelBase,
                            "Excel base != first iteration for " + key);
                }
            }
        }

        System.out.println("[PASS] Iteration Order — base testData = first iteration in both modes");
    }

    // ========== 3. FILL-DOWN BEHAVIOR ==========

    @Test
    @Order(6)
    @DisplayName("3. Fill-down — multi-row TCs from Excel have correct grouping")
    void fillDown_MultiRowGrouping() {
        // If Excel has iteration data for the same keys as JSON, fill-down worked
        int iteratedTCs = 0;
        int matchedTCs = 0;

        for (String key : jsonIterationData.keySet()) {
            if (jsonIterationData.get(key).size() > 1) {
                iteratedTCs++;
                if (excelIterationData.containsKey(key) && excelIterationData.get(key).size() > 1) {
                    matchedTCs++;
                }
            }
        }

        System.out.printf("[%s] Fill-down — %d iterated TCs in JSON, %d matched in Excel%n",
                iteratedTCs == matchedTCs ? "PASS" : "FAIL", iteratedTCs, matchedTCs);
        assertEquals(iteratedTCs, matchedTCs,
                "Fill-down mismatch: some multi-row TCs not grouped correctly in Excel");
    }

    // ========== 4. CROSS-MODULE LOOKUP ==========

    @Test
    @Order(7)
    @DisplayName("4. Cross-module — all JSON modules present in Excel")
    void crossModule_AllModulesPresent() {
        Set<String> missingInExcel = new HashSet<>(jsonModules);
        missingInExcel.removeAll(excelModules);

        Set<String> extraInExcel = new HashSet<>(excelModules);
        extraInExcel.removeAll(jsonModules);

        System.out.printf("  JSON modules: %s%n", jsonModules);
        System.out.printf("  Excel modules: %s%n", excelModules);
        if (!missingInExcel.isEmpty()) {
            System.out.printf("  Missing in Excel: %s%n", missingInExcel);
        }
        if (!extraInExcel.isEmpty()) {
            System.out.printf("  Extra in Excel: %s%n", extraInExcel);
        }

        assertEquals(jsonModules, excelModules,
                "Module set mismatch between JSON and Excel");
        System.out.printf("[PASS] Cross-module — %d modules identical in both sources%n", jsonModules.size());
    }

    @Test
    @Order(8)
    @DisplayName("4b. Cross-module — getDataFromModule simulation")
    void crossModule_DataFromModuleSimulation() {
        // Simulate cross-module lookup: access Login data while "in" ProductConfig
        String loginKey = "Login.TC_LOGIN_001";

        Map<String, String> jsonLogin = jsonTestData.get(loginKey);
        Map<String, String> excelLogin = excelTestData.get(loginKey);

        assertNotNull(jsonLogin, "JSON cross-module lookup failed for " + loginKey);
        assertNotNull(excelLogin, "Excel cross-module lookup failed for " + loginKey);
        assertEquals(jsonLogin, excelLogin, "Cross-module data mismatch for " + loginKey);

        System.out.println("[PASS] Cross-module — getDataFromModule produces identical results");
    }

    // ========== 5. STRICT VALIDATION ==========

    @Test
    @Order(9)
    @DisplayName("5. Strict validation — missing TC returns null in both")
    void strictValidation_MissingTC() {
        String missingKey = "Login.TC_NONEXISTENT_999";

        Map<String, String> jsonResult = jsonTestData.get(missingKey);
        Map<String, String> excelResult = excelTestData.get(missingKey);

        assertNull(jsonResult, "JSON should return null for missing TC");
        assertNull(excelResult, "Excel should return null for missing TC");

        System.out.println("[PASS] Strict validation — missing TC returns null in both modes");
    }

    @Test
    @Order(10)
    @DisplayName("5b. Strict validation — missing field returns null in both")
    void strictValidation_MissingField() {
        String key = "Login.TC_LOGIN_001";

        Map<String, String> jsonData = jsonTestData.get(key);
        Map<String, String> excelData = excelTestData.get(key);

        assertNull(jsonData.get("NonExistentField_XYZ"),
                "JSON should return null for missing field");
        assertNull(excelData.get("NonExistentField_XYZ"),
                "Excel should return null for missing field");

        System.out.println("[PASS] Strict validation — missing field returns null in both modes");
    }

    // ========== 6. LOGGING / STARTUP PARITY ==========

    @Test
    @Order(11)
    @DisplayName("6. Startup — module count matches")
    void startup_ModuleCountMatches() {
        assertEquals(jsonModules.size(), excelModules.size(),
                "Module count mismatch");
        System.out.printf("[PASS] Startup — module count: JSON=%d, Excel=%d%n",
                jsonModules.size(), excelModules.size());
    }

    @Test
    @Order(12)
    @DisplayName("6b. Startup — TC count matches")
    void startup_TcCountMatches() {
        assertEquals(jsonTestData.size(), excelTestData.size(),
                "TC count mismatch: JSON=" + jsonTestData.size() + " Excel=" + excelTestData.size());
        System.out.printf("[PASS] Startup — TC count: JSON=%d, Excel=%d%n",
                jsonTestData.size(), excelTestData.size());
    }

    // ========== 7. PERFORMANCE ==========

    @Test
    @Order(13)
    @DisplayName("7. Performance — Excel load time < 5 seconds (absolute threshold)")
    void performance_ExcelNotAbsurdlySlower() {
        double ratio = (double) excelLoadTimeMs / Math.max(jsonLoadTimeMs, 1);

        System.out.printf("[%s] Performance — JSON=%dms, Excel=%dms, Ratio=%.2fx%n",
                excelLoadTimeMs < 5000 ? "PASS" : "FAIL", jsonLoadTimeMs, excelLoadTimeMs, ratio);

        // Absolute threshold: Excel must load in under 5 seconds for any reasonable dataset
        assertTrue(excelLoadTimeMs < 5000,
                "Excel load time exceeds 5s: " + excelLoadTimeMs + "ms");
    }

    // ========== 8. FULL DATA PARITY ==========

    @Test
    @Order(14)
    @DisplayName("8. Full parity — every JSON key+value exists identically in Excel")
    void fullParity_AllKeysAndValues() {
        int totalFields = 0;
        int mismatches = 0;
        List<String> mismatchDetails = new ArrayList<>();

        for (String key : jsonTestData.keySet()) {
            Map<String, String> jsonData = jsonTestData.get(key);
            Map<String, String> excelData = excelTestData.get(key);

            if (excelData == null) {
                mismatches++;
                mismatchDetails.add("MISSING_KEY: " + key);
                continue;
            }

            for (Map.Entry<String, String> entry : jsonData.entrySet()) {
                totalFields++;
                String excelVal = excelData.get(entry.getKey());
                if (!Objects.equals(entry.getValue(), excelVal)) {
                    mismatches++;
                    if (mismatchDetails.size() < 20) {
                        mismatchDetails.add(String.format("%s.%s: JSON='%s' Excel='%s'",
                                key, entry.getKey(), entry.getValue(), excelVal));
                    }
                }
            }
        }

        if (!mismatchDetails.isEmpty()) {
            System.out.println("  Mismatches (first 20):");
            mismatchDetails.forEach(d -> System.out.println("    " + d));
        }

        System.out.printf("[%s] Full parity — %d fields checked, %d mismatches%n",
                mismatches == 0 ? "PASS" : "FAIL", totalFields, mismatches);
        assertEquals(0, mismatches, "Data parity violations found");
    }

    // ========== FINAL SUMMARY ==========

    @AfterAll
    static void printSummary() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("PARITY VALIDATION SUMMARY");
        System.out.println("=".repeat(100));
        System.out.printf("| %-25s | %6s | %6s | %s%n", "Validation Area", "JSON", "Excel", "Result");
        System.out.println("-".repeat(100));
        System.out.printf("| %-25s | %6d | %6d | %s%n", "Modules", jsonModules.size(), excelModules.size(),
                jsonModules.size() == excelModules.size() ? "✅" : "❌");
        System.out.printf("| %-25s | %6d | %6d | %s%n", "Test Cases", jsonTestData.size(), excelTestData.size(),
                jsonTestData.size() == excelTestData.size() ? "✅" : "❌");
        System.out.printf("| %-25s | %4dms | %4dms | %s%n", "Load Time",
                jsonLoadTimeMs, excelLoadTimeMs,
                excelLoadTimeMs < jsonLoadTimeMs * 2 ? "✅" : "⚠️");
        System.out.println("=".repeat(100) + "\n");
    }

    // ========== HELPER: Load from JSON (mirrors TestDataManager logic) ==========

    private static LoadResult loadFromJson(File jsonDir) {
        LoadResult result = new LoadResult();
        File[] jsonFiles = jsonDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null) return result;

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        for (File file : jsonFiles) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(file);
                if (root == null || root.isEmpty()) continue;

                String fileName = file.getName().replace(".json", "");
                String moduleName = root.has("_module") ? root.get("_module").asText() : fileName;
                result.modules.add(moduleName);

                Iterator<String> fieldNames = root.fieldNames();
                while (fieldNames.hasNext()) {
                    String tcId = fieldNames.next();
                    if (tcId.startsWith("_")) continue;

                    com.fasterxml.jackson.databind.JsonNode tcNode = root.get(tcId);
                    String key = moduleName + "." + tcId;

                    if (tcNode.isArray()) {
                        List<Map<String, String>> iterations = new ArrayList<>();
                        for (int i = 0; i < tcNode.size(); i++) {
                            com.fasterxml.jackson.databind.JsonNode iterNode = tcNode.get(i);
                            if (iterNode.isObject()) {
                                Map<String, String> flat = flattenNode(iterNode);
                                iterations.add(flat);
                            }
                        }
                        if (!iterations.isEmpty()) {
                            result.iterationData.put(key, iterations);
                            result.testData.put(key, iterations.get(0));
                        }
                    } else if (tcNode.isObject()) {
                        Map<String, String> flat = flattenNode(tcNode);
                        result.testData.put(key, flat);
                        List<Map<String, String>> single = new ArrayList<>();
                        single.add(flat);
                        result.iterationData.put(key, single);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load JSON: " + file.getName() + " — " + e.getMessage());
            }
        }
        return result;
    }

    private static Map<String, String> flattenNode(com.fasterxml.jackson.databind.JsonNode obj) {
        Map<String, String> result = new LinkedHashMap<>();
        Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = obj.fields();
        while (fields.hasNext()) {
            Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
            if (entry.getKey().startsWith("_")) continue;
            if (entry.getValue().isValueNode()) {
                String val = entry.getValue().isNull() ? "" :
                        entry.getValue().isTextual() ? entry.getValue().asText() :
                                entry.getValue().isIntegralNumber() ? String.valueOf(entry.getValue().asLong()) :
                                        entry.getValue().isNumber() ? String.valueOf(entry.getValue().asDouble()) :
                                                entry.getValue().asText();
                result.put(entry.getKey(), val);
            }
        }
        return result;
    }

    // ========== HELPER: Load from Excel (uses ExcelDataLoader) ==========

    private static LoadResult loadFromExcel(File excelFile) {
        LoadResult result = new LoadResult();
        List<ExcelDataLoader.ModuleData> modules = ExcelDataLoader.parseExcelFile(excelFile);

        for (ExcelDataLoader.ModuleData module : modules) {
            result.modules.add(module.moduleName);

            for (Map.Entry<String, List<Map<String, String>>> entry : module.testCaseRows.entrySet()) {
                String key = module.moduleName + "." + entry.getKey();
                List<Map<String, String>> rows = entry.getValue();

                result.iterationData.put(key, rows);
                result.testData.put(key, rows.get(0));
            }
        }
        return result;
    }

    private static class LoadResult {
        Map<String, Map<String, String>> testData = new LinkedHashMap<>();
        Map<String, List<Map<String, String>>> iterationData = new LinkedHashMap<>();
        Set<String> modules = new LinkedHashSet<>();
    }
}
