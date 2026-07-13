package kroviq.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RunManager {
    
    private static final Logger logger = LogManager.getLogger(RunManager.class);
    
    private static final String CONFIG_FILE = "RunManager.json";
    
    private static JsonNode config;
    private static boolean initialized = false;
    
    public static void initialize() {
        if (initialized) return;
        initializeForce();
    }

    public static void reinitialize() {
        initialized = false;
        config = null;
        initializeForce();
    }

    private static void initializeForce() {
        File configFile = new File(CONFIG_FILE);
        
        if (!configFile.exists()) {
            throw new RuntimeException("RunManager.json not found at: " + configFile.getAbsolutePath() + ". Cannot proceed.");
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            config = mapper.readTree(configFile);
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            throw new RuntimeException("RunManager.json has invalid JSON syntax at line " 
                + e.getLocation().getLineNr() + ", column " + e.getLocation().getColumnNr() 
                + ": " + e.getOriginalMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse RunManager.json: " + e.getMessage(), e);
        }
        
        validateConfiguration();
        initialized = true;
        logger.info("RunManager.json loaded successfully");
    }
    
    private static void validateConfiguration() {
        if (config == null) {
            throw new RuntimeException("RunManager.json parsed to null. File may be empty.");
        }
        
        // REST-only mode does not require environmentURL
        String mode = config.has("executionMode") ? config.get("executionMode").asText().trim().toLowerCase() : "web";
        if (!"rest".equals(mode)) {
            if (!config.has("environmentURL") || config.get("environmentURL").asText().trim().isEmpty()) {
                throw new RuntimeException("Missing or empty 'environmentURL' in RunManager.json. Cannot proceed without a valid URL.");
            }
            logger.info("Configuration validation passed -- environmentURL: {}", config.get("environmentURL").asText());
        } else {
            logger.info("Configuration validation passed -- REST mode (no environmentURL required)");
        }
    }
    
    public static String getEnvironmentURL() {
        initialize();
        
        try {
            if (config != null && config.has("environmentURL")) {
                String url = config.get("environmentURL").asText().trim();
                if (!url.isEmpty()) {
                    return url;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading environmentURL from RunManager.json: " + e.getMessage(), e);
        }
        
        throw new RuntimeException("environmentURL is missing or empty in RunManager.json. Cannot proceed without a valid URL.");
    }
    
    public static String getIncludeTags() {
        initialize();
        
        try {
            if (config != null && config.has("includeTags")) {
                JsonNode node = config.get("includeTags");
                if (node.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < node.size(); i++) {
                        if (i > 0) sb.append(" or ");
                        sb.append(node.get(i).asText().trim());
                    }
                    return sb.toString();
                }
                return node.asText().trim();
            }
        } catch (Exception e) {
            logger.error("Error reading includeTags: {}", e.getMessage());
        }
        
        return "";
    }
    
    public static String getExcludeTags() {
        initialize();
        
        try {
            if (config != null && config.has("excludeTags")) {
                JsonNode node = config.get("excludeTags");
                if (node.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < node.size(); i++) {
                        if (i > 0) sb.append(" or ");
                        sb.append(node.get(i).asText().trim());
                    }
                    return sb.toString();
                }
                return node.asText().trim();
            }
        } catch (Exception e) {
            logger.error("Error reading excludeTags: {}", e.getMessage());
        }
        
        return "";
    }
    
    public static String getTagExpression() {
        initialize();
        
        String includeTags = getIncludeTags();
        String excludeTags = getExcludeTags();
        
        logger.info("RunManager - Include Tags: '{}'", includeTags);
        logger.info("RunManager - Exclude Tags: '{}'", excludeTags);
        
        StringBuilder expression = new StringBuilder();
        
        if (!includeTags.isEmpty()) {
            expression.append(includeTags);
        }
        
        if (!excludeTags.isEmpty()) {
            if (expression.length() > 0) {
                expression.append(" and ");
            }
            expression.append("not (").append(excludeTags).append(")");
        }
        
        String result = expression.toString();
        if (!result.isEmpty()) {
            logger.info("Final Tag Expression: {}", result);
        } else {
            logger.warn("No tag expression configured");
        }
        
        return result;
    }
    
    public static boolean isParallelExecutionEnabled() {
        initialize();
        
        if (config != null && config.has("parallelExecution")) {
            return config.get("parallelExecution").asBoolean();
        }
        return false;
    }
    
    public static int getThreadCount() {
        initialize();
        
        if (config != null && config.has("threads")) {
            return config.get("threads").asInt();
        }
        return 1;
    }
    
    public static String getProjectName() {
        initialize();
        
        if (config != null && config.has("projectName")) {
            return config.get("projectName").asText();
        }
        return "Kroviq Project";
    }
    
    public static String getBrowser() {
        initialize();
        
        // System property takes priority (set by TestRunner for multi-browser runs)
        String sysProp = System.getProperty("browser");
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            return sysProp.trim().toLowerCase();
        }
        
        if (config != null && config.has("browser")) {
            JsonNode browserNode = config.get("browser");
            if (browserNode.isArray() && browserNode.size() > 0) {
                return browserNode.get(0).asText().trim().toLowerCase();
            }
            return browserNode.asText().trim().toLowerCase();
        }
        return "chrome";
    }
    
    public static java.util.List<String> getBrowserList() {
        initialize();
        
        java.util.List<String> browsers = new java.util.ArrayList<>();
        
        if (config != null && config.has("browser")) {
            JsonNode browserNode = config.get("browser");
            if (browserNode.isArray()) {
                for (JsonNode b : browserNode) {
                    String val = b.asText().trim().toLowerCase();
                    if (!val.isEmpty()) browsers.add(val);
                }
            } else {
                String val = browserNode.asText().trim().toLowerCase();
                if (!val.isEmpty()) browsers.add(val);
            }
        }
        
        if (browsers.isEmpty()) browsers.add("chrome");
        return browsers;
    }
    
    /**
     * Extract all @TC_* tags from includeTags expression for validation
     */
    public static Set<String> getAllowedTestCaseTags() {
        String includeTags = getIncludeTags();
        Set<String> allowed = new HashSet<>();
        
        if (includeTags.isEmpty()) {
            return allowed; // Empty = all scenarios allowed
        }
        
        // Extract @TC_* patterns from tag expression
        Pattern pattern = Pattern.compile("@TC_[A-Z_0-9]+");
        Matcher matcher = pattern.matcher(includeTags);
        while (matcher.find()) {
            allowed.add(matcher.group());
        }
        
        return allowed;
    }
    
    /**
     * Derive module names from @TC_* tags in includeTags for discovery-time filtering
     * Example: @TC_LOGIN_001 -> Login, @TC_PRODUCT_CONFIG_001 -> ProductConfig
     */
    public static Set<String> getAllowedModulesFromIncludeTags() {
        String includeTags = getIncludeTags();
        Set<String> modules = new LinkedHashSet<>();
        
        if (includeTags.isEmpty()) {
            return modules; // Empty = scan all
        }
        
        // Extract @TC_* patterns: @TC_PREFIX_NUMBER
        Pattern pattern = Pattern.compile("@TC_([A-Z_]+)_\\d+");
        Matcher matcher = pattern.matcher(includeTags);
        
        while (matcher.find()) {
            String tagPrefix = matcher.group(1); // e.g., "LOGIN" or "PRODUCT_CONFIG"
            String moduleName = convertTagToModuleName(tagPrefix);
            modules.add(moduleName);
        }
        
        return modules;
    }
    
    /**
     * Convert TAG_PREFIX format to ModuleName format
     * LOGIN -> Login, PRODUCT_CONFIG -> ProductConfig
     */
    private static String convertTagToModuleName(String tagPrefix) {
        String[] parts = tagPrefix.split("_");
        StringBuilder moduleName = new StringBuilder();
        
        for (String part : parts) {
            if (part.length() > 0) {
                moduleName.append(Character.toUpperCase(part.charAt(0)));
                moduleName.append(part.substring(1).toLowerCase());
            }
        }
        
        return moduleName.toString();
    }
    
    public static String getTestDataLogLevel() {
        initialize();

        if (config != null && config.has("testDataLogLevel")) {
            return config.get("testDataLogLevel").asText().trim().toUpperCase();
        }
        return "INFO";
    }

    public static boolean isStrictDataValidation() {
        initialize();
        
        if (config != null && config.has("strictDataValidation")) {
            return config.get("strictDataValidation").asBoolean();
        }
        return false;
    }
    
    public static boolean isFailFastEnabled() {
        initialize();
        
        if (config != null && config.has("failFastEnabled")) {
            return config.get("failFastEnabled").asBoolean();
        }
        return false;
    }
    
    public static boolean isCaptureScreenshotsOnPass() {
        initialize();
        
        if (config != null && config.has("reporting")) {
            JsonNode reporting = config.get("reporting");
            if (reporting.has("captureScreenshotsOnPass")) {
                return reporting.get("captureScreenshotsOnPass").asBoolean();
            }
        }
        return false;
    }
    
    public static JsonNode getQMetryConfig() {
        initialize();
        return config != null ? config.get("qmetry") : null;
    }

    public static boolean isCaptureScreenshotsOnFailure() {
        initialize();
        
        if (config != null && config.has("reporting")) {
            JsonNode reporting = config.get("reporting");
            if (reporting.has("captureScreenshotsOnFailure")) {
                return reporting.get("captureScreenshotsOnFailure").asBoolean();
            }
        }
        return true;
    }
    
    public static String getTestDataSource() {
        initialize();
        
        if (config != null && config.has("testDataSource")) {
            String value = config.get("testDataSource").asText().trim().toLowerCase();
            if ("excel".equals(value)) return "excel";
            if ("json".equals(value)) return "json";
            if (!value.isEmpty()) {
                logger.warn("[RunManager] Invalid testDataSource '{}' — falling back to 'json'", value);
            }
        }
        return "json";
    }

    public static String getExecutionMode() {
        initialize();
        if (config != null && config.has("executionMode")) {
            String value = config.get("executionMode").asText().trim().toLowerCase();
            return switch (value) {
                case "rest", "desktop", "hybrid" -> value;
                default -> "web";
            };
        }
        return "web";
    }

    public static boolean isDesktopMode() {
        return "desktop".equals(getExecutionMode());
    }

    public static boolean isRestMode() {
        return "rest".equals(getExecutionMode());
    }

    public static boolean isHybridMode() {
        return "hybrid".equals(getExecutionMode());
    }

    public static boolean isBrowserMode() {
        String mode = getExecutionMode();
        return "web".equals(mode) || "hybrid".equals(mode);
    }

    public static String getApiBaseUrl() {
        initialize();
        if (config != null && config.has("apiBaseUrl")) {
            String url = config.get("apiBaseUrl").asText().trim();
            if (!url.isEmpty()) return url;
        }
        return null;
    }

    public static int getApiTimeoutSeconds() {
        initialize();
        if (config != null && config.has("apiConfig")) {
            JsonNode apiConfig = config.get("apiConfig");
            if (apiConfig.has("timeoutSeconds")) {
                return apiConfig.get("timeoutSeconds").asInt(30);
            }
        }
        return 30;
    }

    public static boolean isApiVerboseLogging() {
        initialize();
        if (config != null && config.has("apiConfig")) {
            JsonNode apiConfig = config.get("apiConfig");
            if (apiConfig.has("verboseLogging")) {
                return apiConfig.get("verboseLogging").asBoolean(false);
            }
        }
        return false;
    }

    public static String getDesktopApplicationPath() {
        initialize();
        if (config != null && config.has("desktopConfig")) {
            JsonNode dc = config.get("desktopConfig");
            if (dc.has("applicationPath")) {
                return dc.get("applicationPath").asText().trim();
            }
        }
        return "";
    }

    public static int getDesktopLaunchTimeout() {
        initialize();
        if (config != null && config.has("desktopConfig")) {
            JsonNode dc = config.get("desktopConfig");
            if (dc.has("launchTimeoutSeconds")) {
                return dc.get("launchTimeoutSeconds").asInt(30);
            }
        }
        return 30;
    }

    public static boolean isDesktopReuseSession() {
        initialize();
        if (config != null && config.has("desktopConfig")) {
            JsonNode dc = config.get("desktopConfig");
            if (dc.has("reuseSession")) {
                return dc.get("reuseSession").asBoolean(true);
            }
        }
        return true;
    }

    public static boolean isRcaEnabled() {
        initialize();
        if (config != null && config.has("rcaEnabled")) {
            return config.get("rcaEnabled").asBoolean(true);
        }
        return true;
    }

    public static boolean isDefectWriterEnabled() {
        initialize();
        if (config != null && config.has("defectWriterEnabled")) {
            return config.get("defectWriterEnabled").asBoolean(true);
        }
        return isRcaEnabled();
    }
}
