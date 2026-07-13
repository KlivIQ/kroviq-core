package kroviq.utils;

import org.openqa.selenium.WebDriver;
import kroviq.wrapper.GenericWrapper;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Constructor;

public class PageFactory {
    
    private static final Map<String, Class<?>> PAGE_REGISTRY = new HashMap<>();
    private static final Map<String, Object> INSTANCE_CACHE = new HashMap<>();
    
    static {
        ModuleRegistry.getInstance().initialize();
        for (ModuleRegistry.ModuleInfo module : ModuleRegistry.getInstance().getModules().values()) {
            if (module.pageClass != null) {
                registerPage(module.name.toLowerCase(), module.pageClass);
                registerPage(module.name.toLowerCase() + "page", module.pageClass);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T getPage(String pageName, Class<T> expectedType) {
        String normalizedName = pageName.toLowerCase();
        
        if (INSTANCE_CACHE.containsKey(normalizedName)) {
            Object cachedInstance = INSTANCE_CACHE.get(normalizedName);
            if (expectedType.isInstance(cachedInstance)) {
                return (T) cachedInstance;
            }
        }
        
        Class<?> pageClass = PAGE_REGISTRY.get(normalizedName);
        if (pageClass == null) {
            throw new RuntimeException("Page not registered: " + pageName);
        }
        
        if (!expectedType.isAssignableFrom(pageClass)) {
            throw new RuntimeException("Page type mismatch for: " + pageName + 
                ". Expected: " + expectedType.getSimpleName() + 
                ", Found: " + pageClass.getSimpleName());
        }
        
        try {
            Constructor<?> constructor = pageClass.getConstructor(WebDriver.class);
            T instance = (T) constructor.newInstance(GenericWrapper.getDriver());
            INSTANCE_CACHE.put(normalizedName, instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create page instance: " + pageName, e);
        }
    }
    
    public static Object getPage(String pageName) {
        String currentModule = TestContext.getCurrentModule();
        String normalizedPage = pageName.toLowerCase().replace("page", "");
        
        if (!normalizedPage.equalsIgnoreCase(currentModule)) {
            throw new RuntimeException(
                "Module isolation violation: Attempting to get page '" + pageName + 
                "' but current module is '" + currentModule + 
                "'. Expected page: '" + currentModule + "Page'"
            );
        }
        
        return getPage(pageName, Object.class);
    }
    
    public static void clearCache() {
        INSTANCE_CACHE.clear();
    }
    
    public static void registerPage(String pageName, Class<?> pageClass) {
        PAGE_REGISTRY.put(pageName.toLowerCase(), pageClass);
    }
}
