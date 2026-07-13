package kroviq.utils;

import java.io.File;
import java.util.*;

public class ConventionValidator {
    
    private static final String JSON_DIR = "TestDatastore/json";
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private Set<String> jsonModules;
    
    public ValidationResult validate() {
        errors.clear();
        warnings.clear();
        
        // Load data sources
        loadJsonModules();
        
        // Get all discovered modules
        Map<String, ModuleRegistry.ModuleInfo> modules = ModuleRegistry.getInstance().getModules();
        
        if (modules.isEmpty()) {
            errors.add("No modules discovered in testscripts/ directory");
            return new ValidationResult(false, errors, warnings);
        }
        
        // Validate each module
        for (ModuleRegistry.ModuleInfo module : modules.values()) {
            validateModule(module);
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    private void validateModule(ModuleRegistry.ModuleInfo module) {
        String moduleName = module.name;
        
        // Rule 1: Constants class must exist
        if (module.constantsClass == null) {
            errors.add(String.format(
                "[FAIL] Module '%s': Missing constants class 'kroviq.constants.%sConstants'", 
                moduleName, moduleName
            ));
        } else {
            // Rule 2: Constants class must have at least one element
            if (module.constantCount == 0) {
                errors.add(String.format(
                    "[FAIL] Module '%s': Constants class '%sConstants' has no public static String fields", 
                    moduleName, moduleName
                ));
            }
        }
        
        // Rule 3: Data source must exist (JSON file)
        boolean hasJson = jsonModules != null && (jsonModules.contains(moduleName)
                || jsonModules.contains(moduleName.replace("Page", "")));
        if (!hasJson) {
            warnings.add(String.format(
                "[WARN]  Module '%s': No JSON data source found (no file '%s.json' in %s)",
                moduleName, moduleName, JSON_DIR
            ));
        }
        
        // Optional: Page class warning
        if (module.pageClass == null) {
            warnings.add(String.format(
                "[WARN]  Module '%s': No page class found (kroviq.pages.%sPage) - using generic wrapper", 
                moduleName, moduleName
            ));
        }
    }
    
    private void loadJsonModules() {
        jsonModules = new HashSet<>();
        File jsonDir = new File(JSON_DIR);
        if (!jsonDir.exists() || !jsonDir.isDirectory()) return;
        File[] files = jsonDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            jsonModules.add(f.getName().replace(".json", ""));
        }
    }

    public static class ValidationResult {
        public final boolean success;
        public final List<String> errors;
        public final List<String> warnings;
        
        public ValidationResult(boolean success, List<String> errors, List<String> warnings) {
            this.success = success;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }
    }
}
