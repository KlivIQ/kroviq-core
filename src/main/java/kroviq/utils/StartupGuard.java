package kroviq.utils;

import java.util.Map;

public class StartupGuard {
    
    private static boolean validated = false;
    private static final Object lock = new Object();
    
    public static void validateFramework() {
        synchronized (lock) {
            if (validated) return;
            
            System.out.println("\n" + "=".repeat(120));
            System.out.println("[START] Kroviq Framework Startup - Convention Validation");
            System.out.println("=".repeat(120));
            
            // Step 1: Discover modules
            System.out.println("\n[DIR] Discovering modules from testscripts/...");
            ModuleRegistry registry = ModuleRegistry.getInstance();
            registry.initialize();
            
            Map<String, ModuleRegistry.ModuleInfo> modules = registry.getModules();
            System.out.println("   Found " + modules.size() + " module(s)\n");
            
            // Step 2: Validate conventions
            System.out.println("[CHECK] Validating naming conventions...\n");
            ConventionValidator validator = new ConventionValidator();
            ConventionValidator.ValidationResult result = validator.validate();
            
            // Step 3: Display results
            if (!result.success) {
                System.err.println("[FAIL] CONVENTION VALIDATION FAILED\n");
                System.err.println("Errors:");
                for (String error : result.errors) {
                    System.err.println("   " + error);
                }
                System.err.println("\n" + "=".repeat(120));
                throw new RuntimeException("Framework startup failed due to convention violations. Fix the errors above.");
            }
            
            // Display warnings
            if (!result.warnings.isEmpty()) {
                System.out.println("Warnings:");
                for (String warning : result.warnings) {
                    System.out.println("   " + warning);
                }
                System.out.println();
            }
            
            // Step 4: Display module registry
            System.out.println("[OK] CONVENTION VALIDATION PASSED\n");
            System.out.println("[INFO] Registered Modules:");
            System.out.println("-".repeat(120));
            System.out.printf("%-20s %-15s %-15s %-15s %-10s%n", 
                "Module", "Constants", "Page Class", "Data Source", "Elements");
            System.out.println("-".repeat(120));
            
            for (ModuleRegistry.ModuleInfo module : modules.values()) {
                // Check JSON data source
                java.io.File jsonFile = new java.io.File("TestDatastore/json/" + module.name + ".json");
                String dataSource = jsonFile.exists() ? "[JSON]" : "[N]";
                
                System.out.printf("%-20s %-15s %-15s %-15s %-10d%n",
                    module.name,
                    module.constantsClass != null ? "[Y]" : "[N]",
                    module.pageClass != null ? "[Y]" : "[N]",
                    dataSource,
                    module.constantCount
                );
            }
            
            System.out.println("-".repeat(120));
            System.out.println("\n[READY] Framework ready for test execution");
            System.out.println("=".repeat(120) + "\n");
            
            validated = true;
        }
    }
    
    public static boolean isValidated() {
        return validated;
    }
    
    public static void reset() {
        synchronized (lock) {
            validated = false;
        }
    }
}
