package kroviq.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.util.*;

public class TestDataManager {
    private static final Logger logger = LogManager.getLogger(TestDataManager.class);
    private static final TestDataManager instance = new TestDataManager();

    /*
     * LOOKUP ORDER CONTRACT (DO NOT CHANGE):
     *   1. moduleKey  = currentModule + "." + testCaseId   (primary — source of truth)
     *   2. pageKey    = page + "." + testCaseId            (page-name fallback)
     *   3. legacyKey  = page + ".TC_" + PAGE_UPPER + "_001" (legacy — BLOCKED in strict mode)
     *
     * Data source (JSON or Excel) is determined by RunManager.json "testDataSource".
     * Both sources populate identical runtime structures (testData, iterationData).
     * If someone reorders the lookup, data will be silently bypassed.
     */
    private Map<String, Map<String, String>> testData;
    private Map<String, List<Map<String, String>>> iterationData;
    private Map<String, String> dataSource;
    private Set<String> jsonLoadedModules;
    private static final ThreadLocal<Map<String, String>> runtimeData = ThreadLocal.withInitial(HashMap::new);
    private static final String JSON_DIR = "TestDatastore/json";
    private static final String EXCEL_SINGLE_FILE = "TestDatastore/Testdata.xlsx";
    private static final String EXCEL_DIR = "TestDatastore/excel";
    private String resolvedDataSource = "json";

    private TestDataManager() {
        this.testData = new HashMap<>();
        this.iterationData = new HashMap<>();
        this.dataSource = new HashMap<>();
        this.jsonLoadedModules = new HashSet<>();
        loadTestData();
    }

    public static TestDataManager get() {
        return instance;
    }

    public void reloadTestData() {
        testData.clear();
        iterationData.clear();
        dataSource.clear();
        jsonLoadedModules.clear();
        loadTestData();
        logger.info("[TestData] Test data reloaded successfully");
    }

    // ========== JSON LOADING ==========

    private void loadTestData() {
        long startTime = System.currentTimeMillis();

        configureLogLevel();

        resolvedDataSource = resolveDataSource();

        if ("excel".equals(resolvedDataSource)) {
            loadFromExcel();
        } else {
            loadAllJsonFiles();
        }

        long elapsed = System.currentTimeMillis() - startTime;

        boolean strict = isStrictMode();
        logger.info("[TestDataManager] Source: {}", resolvedDataSource.toUpperCase());
        if ("excel".equals(resolvedDataSource)) {
            logger.info("[TestDataManager] Mode: {}", resolveExcelPath());
        }
        logger.info("[TestDataManager] Modules: {}", jsonLoadedModules.size());
        logger.info("[TestDataManager] TestCases: {}", testData.size());
        logger.info("[TestDataManager] LoadTime: {}ms", elapsed);
        logger.info("[TestData] Strict mode: {}", strict ? "ENABLED" : "DISABLED");
        logger.debug("[TestData] Module list: {}", jsonLoadedModules);
    }

    private String resolveExcelPath() {
        File singleFile = new File(EXCEL_SINGLE_FILE);
        if (singleFile.exists()) return singleFile.getAbsolutePath();
        File excelDir = new File(EXCEL_DIR);
        if (excelDir.exists() && excelDir.isDirectory()) return excelDir.getAbsolutePath() + "/*.xlsx";
        return "(not resolved)";
    }

    private String resolveDataSource() {
        try {
            return RunManager.getTestDataSource();
        } catch (Exception e) {
            logger.debug("[TestData] RunManager not ready, defaulting to 'json'");
            return "json";
        }
    }

    // ========== EXCEL LOADING ==========

    private void loadFromExcel() {
        File resolvedFile = resolveExcelFile();
        if (resolvedFile == null) return; // Already loaded via directory mode

        logger.info("[TestData] Excel mode: {}", resolvedFile.getAbsolutePath());

        List<ExcelDataLoader.ModuleData> modules = ExcelDataLoader.parseExcelFile(resolvedFile);

        for (ExcelDataLoader.ModuleData module : modules) {
            jsonLoadedModules.add(module.moduleName);
            int tcCount = 0;
            int totalIterations = 0;

            for (Map.Entry<String, List<Map<String, String>>> entry : module.testCaseRows.entrySet()) {
                String tcId = entry.getKey();
                List<Map<String, String>> rows = entry.getValue();
                String key = module.moduleName + "." + tcId;

                if (rows.size() == 1) {
                    testData.put(key, rows.get(0));
                    List<Map<String, String>> singleIteration = new ArrayList<>();
                    singleIteration.add(rows.get(0));
                    iterationData.put(key, singleIteration);
                } else {
                    iterationData.put(key, rows);
                    testData.put(key, rows.get(0));
                    totalIterations += rows.size();
                }
                dataSource.put(key, "EXCEL");
                tcCount++;
            }

            logger.info("[TestData] Module '{}' loaded from EXCEL ({} test cases{})",
                    module.moduleName, tcCount,
                    totalIterations > tcCount ? ", " + totalIterations + " total iterations" : "");
        }
    }

    private File resolveExcelFile() {
        // Priority 1: Single file TestDatastore/Testdata.xlsx
        File singleFile = new File(EXCEL_SINGLE_FILE);
        if (singleFile.exists()) return singleFile;

        // Priority 2: Directory TestDatastore/excel/*.xlsx
        File excelDir = new File(EXCEL_DIR);
        if (excelDir.exists() && excelDir.isDirectory()) {
            File[] xlsxFiles = excelDir.listFiles((dir, name) -> name.endsWith(".xlsx"));
            if (xlsxFiles != null && xlsxFiles.length > 0) {
                // Load all xlsx files from directory
                loadFromExcelDirectory(xlsxFiles);
                return null; // Signal: already loaded via directory mode
            }
        }

        // Priority 3: Neither found — throw clear error
        throw new FatalFrameworkException(
                "[TestData] testDataSource='excel' but no Excel files found. " +
                "Checked: 1) " + new File(EXCEL_SINGLE_FILE).getAbsolutePath() +
                " 2) " + new File(EXCEL_DIR).getAbsolutePath() + "/*.xlsx");
    }

    private void loadFromExcelDirectory(File[] xlsxFiles) {
        logger.info("[TestData] Excel mode: directory {} ({} files)", EXCEL_DIR, xlsxFiles.length);
        for (File file : xlsxFiles) {
            try {
                List<ExcelDataLoader.ModuleData> modules = ExcelDataLoader.parseExcelFile(file);
                for (ExcelDataLoader.ModuleData module : modules) {
                    jsonLoadedModules.add(module.moduleName);
                    int tcCount = 0;

                    for (Map.Entry<String, List<Map<String, String>>> entry : module.testCaseRows.entrySet()) {
                        String tcId = entry.getKey();
                        List<Map<String, String>> rows = entry.getValue();
                        String key = module.moduleName + "." + tcId;

                        if (rows.size() == 1) {
                            testData.put(key, rows.get(0));
                            List<Map<String, String>> singleIteration = new ArrayList<>();
                            singleIteration.add(rows.get(0));
                            iterationData.put(key, singleIteration);
                        } else {
                            iterationData.put(key, rows);
                            testData.put(key, rows.get(0));
                        }
                        dataSource.put(key, "EXCEL");
                        tcCount++;
                    }

                    logger.info("[TestData] Module '{}' loaded from EXCEL file '{}' ({} test cases)",
                            module.moduleName, file.getName(), tcCount);
                }
            } catch (Exception e) {
                if (isStrictMode()) {
                    throw new FatalFrameworkException("[TestData] Strict mode: Failed to load Excel file '" + file.getName() + "': " + e.getMessage(), e);
                }
                logger.error("[TestData] Failed to load Excel file '{}': {}", file.getName(), e.getMessage());
            }
        }
    }

    private void configureLogLevel() {
        try {
            String level = RunManager.getTestDataLogLevel();
            if ("DEBUG".equalsIgnoreCase(level)) {
                org.apache.logging.log4j.core.config.Configurator.setLevel(logger.getName(), org.apache.logging.log4j.Level.DEBUG);
                logger.info("[TestData] Log level set to DEBUG via RunManager config");
            }
        } catch (Exception e) {
            // RunManager not ready or config missing — keep default level
        }
    }

    private int loadAllJsonFiles() {
        File jsonDir = new File(JSON_DIR);
        if (!jsonDir.exists() || !jsonDir.isDirectory()) {
            logger.debug("[TestData] No JSON directory found at '{}', skipping JSON loading", JSON_DIR);
            return 0;
        }

        File[] jsonFiles = jsonDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            logger.debug("[TestData] No JSON files found in '{}'", JSON_DIR);
            return 0;
        }

        int loaded = 0;
        for (File file : jsonFiles) {
            try {
                loadFromJsonFile(file);
                loaded++;
            } catch (Exception e) {
                if (isStrictMode()) {
                    throw new RuntimeException("[TestData] Strict mode: Failed to load JSON file '" + file.getName() + "': " + e.getMessage(), e);
                }
                logger.error("[TestData] Failed to load JSON file '{}': {} — module will have NO data", file.getName(), e.getMessage());
                String failedModule = file.getName().replace(".json", "");
                jsonLoadedModules.add(failedModule);
            }
        }
        return loaded;
    }

    private void loadFromJsonFile(File jsonFile) {
        String fileName = jsonFile.getName().replace(".json", "");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try {
            root = mapper.readTree(jsonFile);
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            String msg = String.format("[TestData] Invalid JSON syntax in '%s' at line %d, column %d: %s",
                    jsonFile.getName(), e.getLocation().getLineNr(), e.getLocation().getColumnNr(), e.getOriginalMessage());
            if (isStrictMode()) {
                throw new RuntimeException(msg, e);
            }
            logger.error(msg);
            return;
        } catch (Exception e) {
            String msg = String.format("[TestData] Cannot read JSON file '%s': %s", jsonFile.getName(), e.getMessage());
            if (isStrictMode()) {
                throw new RuntimeException(msg, e);
            }
            logger.error(msg);
            return;
        }

        if (root == null || root.isEmpty()) {
            String msg = String.format("[TestData] JSON file '%s' is empty or null", jsonFile.getName());
            if (isStrictMode()) {
                throw new RuntimeException(msg);
            }
            logger.error(msg);
            return;
        }

        String moduleName = root.has("_module") ? root.get("_module").asText() : fileName;

        // Validate: _module value must match filename
        if (!moduleName.equals(fileName)) {
            logger.warn("[TestData] Module name mismatch: file='{}' but _module='{}' — using _module value '{}'",
                    jsonFile.getName(), moduleName, moduleName);
        }

        jsonLoadedModules.add(moduleName);
        int tcCount = 0;
        int totalIterations = 0;

        Iterator<String> fieldNames = root.fieldNames();
        while (fieldNames.hasNext()) {
            String tcId = fieldNames.next();
            if (tcId.startsWith("_")) continue;

            JsonNode tcNode = root.get(tcId);
            String key = moduleName + "." + tcId;

            if (tcNode.isArray()) {
                List<Map<String, String>> iterations = new ArrayList<>();
                for (int i = 0; i < tcNode.size(); i++) {
                    JsonNode iterNode = tcNode.get(i);
                    if (iterNode.isObject()) {
                        Map<String, String> flat = flattenJsonObject(iterNode, moduleName, tcId);
                        iterations.add(flat);
                    }
                }
                if (!iterations.isEmpty()) {
                    iterationData.put(key, iterations);
                    testData.put(key, iterations.get(0));
                    dataSource.put(key, "JSON");
                    tcCount++;
                    totalIterations += iterations.size();
                    logger.info("[TestData] Iterations detected: Module={} | TC={} | Count={}", moduleName, tcId, iterations.size());
                }
            } else if (tcNode.isObject()) {
                Map<String, String> flat = flattenJsonObject(tcNode, moduleName, tcId);
                if (flat.isEmpty() && isStrictMode()) {
                    throw new RuntimeException(String.format("[TestData] Strict mode: TC '%s' in module '%s' has zero fields after flattening", tcId, moduleName));
                }
                testData.put(key, flat);
                List<Map<String, String>> singleIteration = new ArrayList<>();
                singleIteration.add(flat);
                iterationData.put(key, singleIteration);
                dataSource.put(key, "JSON");
                tcCount++;
                logger.debug("[TestData] Module={} | TC={} | Fields={} | Source=JSON", moduleName, tcId, flat.size());
            }
        }

        logger.info("[TestData] Module '{}' loaded from JSON source ({} test cases{})",
                moduleName, tcCount,
                totalIterations > tcCount ? ", " + totalIterations + " total iterations" : "");
    }

    // ========== NAMESPACE-AWARE FLATTENING ==========

    private Map<String, String> flattenJsonObject(JsonNode obj, String moduleName, String tcId) {
        Map<String, String> result = new LinkedHashMap<>();
        int collisionCount = 0;

        // Pass 1: root-level primitives
        Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String fieldKey = entry.getKey();
            JsonNode value = entry.getValue();

            if (fieldKey.startsWith("_")) continue;

            if (value.isValueNode()) {
                result.put(fieldKey, getNodeValueAsString(value));
            }
        }

        // Pass 2: nested objects — namespace.key (always) + flat shorthand (if unclaimed)
        fields = obj.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String namespace = entry.getKey();
            JsonNode value = entry.getValue();

            if (namespace.startsWith("_")) continue;
            if (!value.isObject()) continue;

            Iterator<Map.Entry<String, JsonNode>> nestedFields = value.fields();
            while (nestedFields.hasNext()) {
                Map.Entry<String, JsonNode> nestedEntry = nestedFields.next();
                String leafKey = nestedEntry.getKey();
                JsonNode leafValue = nestedEntry.getValue();

                if (leafKey.startsWith("_")) continue;
                if (!leafValue.isValueNode()) continue;

                String stringValue = getNodeValueAsString(leafValue);

                // Always add namespaced key
                String namespacedKey = namespace + "." + leafKey;
                result.put(namespacedKey, stringValue);

                // Flat shorthand — only if not already claimed
                if (result.containsKey(leafKey)) {
                    String existingValue = result.get(leafKey);
                    if (!existingValue.equals(stringValue)) {
                        collisionCount++;
                        logger.warn("[TestData] COLLISION | Module={} | TC={} | Key={} | ExistingValue={} | NestedValue={} (existing wins)",
                                moduleName, tcId, leafKey, existingValue, stringValue);
                    }
                } else {
                    result.put(leafKey, stringValue);
                }
            }
        }

        if (collisionCount > 0) {
            logger.warn("[TestData] TC={} had {} key collision(s) — use namespaced keys (e.g., section.FieldName) to access shadowed values", tcId, collisionCount);
        }

        return result;
    }

    private String getNodeValueAsString(JsonNode node) {
        if (node == null || node.isNull()) return "";
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) {
            if (node.isIntegralNumber()) return String.valueOf(node.asLong());
            return String.valueOf(node.asDouble());
        }
        if (node.isBoolean()) return String.valueOf(node.asBoolean());
        return node.asText();
    }

    // ========== DATA ACCESS ==========

    public Map<String, String> getData(String page, String testCaseId) {
        String currentModule = TestContext.getCurrentModule();
        String normalizedPage = page.replace("Page", "");

        if (!normalizedPage.equalsIgnoreCase(currentModule)) {
            throw new RuntimeException(
                "Module isolation violation: Attempting to read test data from module '" + page +
                "' but current module is '" + currentModule +
                "'. Expected module: '" + currentModule + "'"
            );
        }

        // Validate module is known
        if (!jsonLoadedModules.contains(currentModule)) {
            boolean foundViaPage = jsonLoadedModules.contains(page);
            if (!foundViaPage) {
                logger.warn("[TestData] Module '{}' not found in loaded JSON modules: {}",
                        currentModule, jsonLoadedModules);
            }
        }

        String cid = correlationId();
        int currentIteration = kroviq.utils.IterationExecutor.getCurrentIteration(currentModule, testCaseId);
        if (currentIteration >= 0) {
            int totalIterations = getIterationCount(currentModule, testCaseId);
            Map<String, String> iterData = getIteration(currentModule, testCaseId, currentIteration);
            if (iterData != null && !iterData.isEmpty()) {
                logger.debug("[TestData][{}] Module={} | TC={} | Iteration={}/{} | Source={} | Fields={}",
                        cid, currentModule, testCaseId, currentIteration + 1, totalIterations,
                        getSource(currentModule + "." + testCaseId), iterData.size());
                return iterData;
            }
        }

        // ── LOOKUP ORDER (DO NOT REORDER — see contract comment at class top) ──
        // 1. moduleKey (JSON = source of truth) → 2. pageKey (page-name fallback) → 3. legacyKey (BLOCKED in strict)

        // 1. Module name key (JSON convention: Login.TC_LOGIN_001)
        String moduleKey = currentModule + "." + testCaseId;
        String key = moduleKey;
        Map<String, String> data = testData.get(moduleKey);

        // 2. Page name key (page-name fallback: LoginPage.TC_LOGIN_001)
        if (data == null) {
            String pageKey = page + "." + testCaseId;
            data = testData.get(pageKey);
            if (data != null) {
                if (!pageKey.equals(moduleKey)) {
                    logger.debug("[TestData][{}] FALLBACK | Requested={} → Using={}", cid, moduleKey, pageKey);
                }
                key = pageKey;
            }
        }

        // 3. Legacy fallback — BLOCKED in strict mode (wrong data + passing tests = worst scenario)
        if (data == null) {
            String legacyKey = page + ".TC_" + page.toUpperCase() + "_001";
            data = testData.get(legacyKey);
            if (data != null) {
                if (isStrictMode()) {
                    throw new RuntimeException(String.format(
                        "[TestData][STRICT] LEGACY_FALLBACK_BLOCKED | Requested TC '%s' not found, " +
                        "but legacy key '%s' exists. This is dangerous — wrong data would be returned silently. " +
                        "Fix the test data or the test case tag.",
                        testCaseId, legacyKey));
                }
                logger.warn("[TestData][{}] LEGACY_FALLBACK_USED | Requested={} → Using={} — THIS IS UNSAFE, fix your test data",
                        cid, moduleKey, legacyKey);
                key = legacyKey;
            }
        }

        if (data == null) {
            if (isStrictMode()) {
                throw new RuntimeException(String.format(
                    "[TestData][STRICT] Missing data | Module=%s | TC=%s | " +
                    "Looked up keys: ['%s', '%s.%s']. JSON modules: %s",
                    currentModule, testCaseId, page + "." + testCaseId, currentModule, testCaseId,
                    jsonLoadedModules));
            }
            logger.warn("[TestData][{}] MISS | Module={} | TC={} | No data found", cid, currentModule, testCaseId);
            data = new HashMap<>();
        } else if (data.isEmpty()) {
            // Data key exists but map is empty — broken data, not optional
            if (isStrictMode()) {
                throw new RuntimeException(String.format(
                    "[TestData][STRICT] Empty data | Module=%s | TC=%s | Source=%s | " +
                    "Key '%s' exists but contains zero fields. Check TestDatastore/json/%s.json",
                    currentModule, testCaseId, getSource(key), key, currentModule));
            }
            logger.error("[TestData][{}] EMPTY_DATA | Module={} | TC={} | Source={} — key exists but zero fields",
                    cid, currentModule, testCaseId, getSource(key));
        } else {
            logger.debug("[TestData][{}] Module={} | TC={} | Source={} | Fields={} | Iteration=none",
                    cid, currentModule, testCaseId, getSource(key), data.size());
        }

        return data;
    }

    public String getTestDataValue(String page, String testCaseId, String fieldName) {
        Map<String, String> data = getData(page, testCaseId);
        String value = data.get(fieldName);
        String cid = correlationId();

        if (value == null || value.isEmpty()) {
            if (isStrictMode()) {
                String currentModule = TestContext.getCurrentModule();
                throw new RuntimeException(String.format(
                    "[TestData][STRICT] Missing data | Module=%s | TC=%s | Field=%s | Available=%s",
                    currentModule, testCaseId, fieldName, data.keySet()));
            }
            String currentModule = "unknown";
            try { currentModule = TestContext.getCurrentModule(); } catch (Exception ignored) {}
            logger.warn("[TestData][{}] MISS | Module={} | TC={} | Field={}", cid, currentModule, testCaseId, fieldName);
            value = "";
        } else {
            String source = resolveSource(page, testCaseId);
            logger.debug("[TestData][{}] Module={} | TC={} | Field={} | Value={} | Source={}",
                    cid, page.replace("Page", ""), testCaseId, fieldName, value, source);
        }

        return value;
    }

    public boolean validateTestData(String page, String testCaseId, String[] requiredFields) {
        Map<String, String> data = getData(page, testCaseId);
        boolean isValid = true;
        for (String field : requiredFields) {
            String value = data.get(field);
            if (value == null || value.trim().isEmpty()) {
                logger.warn("[TestData] Missing required field: {} for {}.{}", field, page, testCaseId);
                isValid = false;
            }
        }
        return isValid;
    }

    public List<Map<String, String>> getIterations(String page, String testCaseId) {
        String currentModule = TestContext.getCurrentModule();
        String normalizedPage = page.replace("Page", "");

        if (!normalizedPage.equalsIgnoreCase(currentModule)) {
            throw new RuntimeException(
                "Module isolation violation: Attempting to read iterations from module '" + page +
                "' but current module is '" + currentModule +
                "'. Expected module: '" + currentModule + "'"
            );
        }

        String key = currentModule + "." + testCaseId;
        List<Map<String, String>> iterations = iterationData.getOrDefault(key, new ArrayList<>());
        if (iterations.size() > 1) {
            logger.debug("[TestData][{}] Module={} | TC={} | IterationCount={} | Source={}", correlationId(), currentModule, testCaseId, iterations.size(), getSource(key));
        }
        return iterations;
    }

    public Map<String, String> getIteration(String page, String testCaseId, int iteration) {
        List<Map<String, String>> iterations = getIterations(page, testCaseId);
        String cid = correlationId();
        if (iteration >= 0 && iteration < iterations.size()) {
            Map<String, String> iterData = iterations.get(iteration);
            String currentModule = TestContext.getCurrentModule();
            logger.debug("[TestData][{}] Module={} | TC={} | Iteration={}/{} | Source={} | Fields={}",
                    cid, currentModule, testCaseId, iteration + 1, iterations.size(),
                    getSource(currentModule + "." + testCaseId), iterData.size());
            return iterData;
        }
        // Out of bounds — strict mode should not silently fall back to default data
        if (isStrictMode()) {
            String currentModule = TestContext.getCurrentModule();
            throw new RuntimeException(String.format(
                "[TestData][STRICT] Missing data | Module=%s | TC=%s | Iteration=%d requested but only %d available",
                currentModule, testCaseId, iteration + 1, iterations.size()));
        }
        logger.warn("[TestData][{}] Iteration {} out of bounds (total={}) for TC={}, falling back to default",
                cid, iteration + 1, iterations.size(), testCaseId);
        return getData(page, testCaseId);
    }

    public int getIterationCount(String page, String testCaseId) {
        return getIterations(page, testCaseId).size();
    }

    /**
     * Retrieves test data from a specific module (cross-module lookup).
     * Use this when a step needs data from a module other than the current one.
     */
    public Map<String, String> getDataFromModule(String moduleName, String testCaseId) {
        String key = moduleName + "." + testCaseId;
        String cid = correlationId();
        String callingModule = "unknown";
        try { callingModule = TestContext.getCurrentModule(); } catch (Exception ignored) {}

        Map<String, String> data = testData.get(key);
        if (data == null) {
            if (isStrictMode()) {
                throw new RuntimeException(String.format(
                    "[TestData][STRICT] Missing data | CrossModule | From=%s → To=%s | TC=%s | " +
                    "JSON modules: %s",
                    callingModule, moduleName, testCaseId, jsonLoadedModules));
            }
            logger.warn("[TestData][{}] MISS | CrossModule | From={} → To={} | TC={}", cid, callingModule, moduleName, testCaseId);
            return new HashMap<>();
        }
        if (data.isEmpty()) {
            if (isStrictMode()) {
                throw new RuntimeException(String.format(
                    "[TestData][STRICT] Empty data | CrossModule | From=%s → To=%s | TC=%s | Source=%s",
                    callingModule, moduleName, testCaseId, getSource(key)));
            }
            logger.error("[TestData][{}] EMPTY_DATA | CrossModule | From={} → To={} | TC={} | Source={}",
                    cid, callingModule, moduleName, testCaseId, getSource(key));
        }
        logger.debug("[TestData][{}] CrossModule | From={} → To={} | TC={} | Fields={} | Source={}",
                cid, callingModule, moduleName, testCaseId, data.size(), getSource(key));
        return data;
    }

    /**
     * @deprecated Use {@link #getDataFromModule(String, String)} instead. This method is retained
     *             for backward compatibility with existing step definitions that use "sheet" terminology.
     */
    @Deprecated
    public Map<String, String> getDataFromSheet(String sheetName, String testCaseId) {
        return getDataFromModule(sheetName, testCaseId);
    }

    public String resolveReference(String reference) {
        if (!reference.contains(".")) return reference;

        String[] parts = reference.split("\\.", 2);
        if (parts.length != 2) return reference;

        String tcId = parts[0];
        String fieldName = parts[1];

        for (String key : testData.keySet()) {
            if (key.endsWith("." + tcId)) {
                Map<String, String> data = testData.get(key);
                String value = data.get(fieldName);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }

        return reference;
    }

    // ========== RUNTIME DATA ==========

    public void setRuntimeValue(String key, String value) {
        runtimeData.get().put(key, value);
    }

    public String getRuntimeValue(String key) {
        return runtimeData.get().get(key);
    }

    public void clearRuntimeData() {
        Map<String, String> threadMap = runtimeData.get();
        if (threadMap != null) {
            threadMap.clear();
        }
        runtimeData.remove();
    }

    // ========== HELPERS ==========

    private String getSource(String key) {
        return dataSource.getOrDefault(key, "UNKNOWN");
    }

    private String resolveSource(String page, String testCaseId) {
        String source = getSource(page + "." + testCaseId);
        if ("UNKNOWN".equals(source)) {
            try {
                source = getSource(TestContext.getCurrentModule() + "." + testCaseId);
            } catch (Exception ignored) {}
        }
        return source;
    }

    private String correlationId() {
        try {
            String tcId = TestContext.getCurrentTestCaseId();
            return tcId != null ? tcId : "no-tc";
        } catch (Exception e) {
            return "no-tc";
        }
    }

    private boolean isStrictMode() {
        try {
            return RunManager.isStrictDataValidation();
        } catch (Exception e) {
            return false;
        }
    }
}
