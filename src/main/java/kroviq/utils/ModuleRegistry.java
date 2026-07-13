package kroviq.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ModuleRegistry {
    
    private static ModuleRegistry instance;
    private final Map<String, ModuleInfo> modules = new LinkedHashMap<>();
    private boolean initialized = false;
    
    public static class ModuleInfo {
        public final String name;
        public final String featurePath;
        public final Class<?> constantsClass;
        public final Class<?> pageClass;
        public final int constantCount;
        
        public ModuleInfo(String name, String featurePath, Class<?> constantsClass, 
                         Class<?> pageClass, int constantCount) {
            this.name = name;
            this.featurePath = featurePath;
            this.constantsClass = constantsClass;
            this.pageClass = pageClass;
            this.constantCount = constantCount;
        }
    }
    
    private ModuleRegistry() {}
    
    public static ModuleRegistry getInstance() {
        if (instance == null) {
            instance = new ModuleRegistry();
        }
        return instance;
    }
    
    public void initialize() {
        if (initialized) return;
        
        modules.clear();
        discoverModules();
        initialized = true;
    }
    
    private void discoverModules() {
        Path testScriptsPath = Paths.get("src/test/resources/features");
        if (!Files.exists(testScriptsPath)) {
            testScriptsPath = Paths.get("target/classes/features");
        }
        
        if (!Files.exists(testScriptsPath)) {
            System.err.println("[WARN]  features directory not found");
            return;
        }
        
        File testScriptsDir = testScriptsPath.toFile();
        File[] moduleDirs = testScriptsDir.listFiles(File::isDirectory);
        
        if (moduleDirs == null || moduleDirs.length == 0) {
            System.err.println("[WARN]  No module directories found in features/");
            return;
        }
        
        for (File moduleDir : moduleDirs) {
            String moduleName = moduleDir.getName();
            
            // Check for feature file
            File featureFile = new File(moduleDir, moduleName + ".feature");
            if (!featureFile.exists()) {
                System.err.println("[WARN]  Module '" + moduleName + "' missing feature file: " + moduleName + ".feature");
                continue;
            }
            
            // Load constants class
            Class<?> constantsClass = loadClass("kroviq.constants." + moduleName + "Constants");
            
            // Load page class (optional)
            Class<?> pageClass = loadClass("kroviq.pages." + moduleName + "Page");
            
            // Count constants
            int constantCount = constantsClass != null ? countConstants(constantsClass) : 0;
            
            ModuleInfo info = new ModuleInfo(
                moduleName,
                featureFile.getAbsolutePath(),
                constantsClass,
                pageClass,
                constantCount
            );
            
            modules.put(moduleName, info);
        }
    }
    
    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    private int countConstants(Class<?> clazz) {
        return (int) Arrays.stream(clazz.getDeclaredFields())
            .filter(f -> java.lang.reflect.Modifier.isStatic(f.getModifiers()))
            .filter(f -> java.lang.reflect.Modifier.isPublic(f.getModifiers()))
            .filter(f -> f.getType() == String.class)
            .count();
    }
    
    public Map<String, ModuleInfo> getModules() {
        return Collections.unmodifiableMap(modules);
    }
    
    public ModuleInfo getModule(String name) {
        return modules.get(name);
    }
    
    public boolean isInitialized() {
        return initialized;
    }
}
