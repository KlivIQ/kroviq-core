package kroviq.utils;

import java.lang.reflect.Field;
import kroviq.wrapper.desktop.core.DesktopLocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConstantsResolver {
    
    private static final Logger logger = LogManager.getLogger(ConstantsResolver.class);
    
    public static class ElementInfo {
        private final String xpath;
        private final ActionType actionType;
        
        public ElementInfo(String xpath, ActionType actionType) {
            this.xpath = xpath;
            this.actionType = actionType;
        }
        
        public String getXpath() {
            return xpath;
        }
        
        public ActionType getActionType() {
            return actionType;
        }
    }

    public static class DesktopElementInfo {
        private final DesktopLocator locator;
        private final ActionType actionType;

        public DesktopElementInfo(DesktopLocator locator, ActionType actionType) {
            this.locator = locator;
            this.actionType = actionType;
        }

        public DesktopLocator getLocator() { return locator; }

        public ActionType getActionType() { return actionType; }
    }
    
    public static ElementInfo resolve(String pageName, String constantName) {
        if (pageName == null || constantName == null) {
            throw new IllegalArgumentException("Page name and constant name cannot be null");
        }
        
        String currentModule = TestContext.getCurrentModule();
        String normalizedPage = pageName.replace(" ", "").replace("Page", "");
        
        if (!normalizedPage.equalsIgnoreCase(currentModule)) {
            throw new RuntimeException(
                "Module isolation violation: Attempting to resolve element '" + constantName + 
                "' from page '" + pageName + "' but current module is '" + currentModule + 
                "'. Expected page: '" + currentModule + "Page'"
            );
        }
        
        Class<?> constantsClass = getConstantsClass(pageName);
        if (constantsClass == null) {
            throw new RuntimeException("No Constants class found for page: " + pageName + " in module: " + currentModule);
        }
        
        ElementInfo result = resolveFromClass(constantsClass, constantName);
        if (result != null) {
            return result;
        }
        
        try {
            Class<?> commonClass = Class.forName("kroviq.constants.CommonConstants");
            if (!commonClass.equals(constantsClass)) {
                result = resolveFromClass(commonClass, constantName);
                if (result != null) {
                    logger.debug("Resolved '{}' from CommonConstants (fallback)", constantName);
                    return result;
                }
            }
        } catch (ClassNotFoundException e) {
            logger.debug("CommonConstants class not found, skipping fallback");
        }
        
        throw new RuntimeException("Element '" + constantName + "' not found in " + constantsClass.getSimpleName() + 
            " or CommonConstants for module '" + currentModule + "'");
    }

    public static DesktopElementInfo resolveDesktop(String pageName, String constantName) {
        if (pageName == null || constantName == null) {
            throw new IllegalArgumentException("Page name and constant name cannot be null");
        }

        String currentModule = TestContext.getCurrentModule();
        String normalizedPage = pageName.replace(" ", "").replace("Page", "");

        if (!normalizedPage.equalsIgnoreCase(currentModule)) {
            throw new RuntimeException(
                "Module isolation violation: Attempting to resolve desktop element '" + constantName +
                "' from page '" + pageName + "' but current module is '" + currentModule + "'");
        }

        Class<?> constantsClass = getConstantsClass(pageName);
        if (constantsClass == null) {
            throw new RuntimeException("No Constants class found for page: " + pageName + " in module: " + currentModule);
        }

        DesktopElementInfo result = resolveDesktopFromClass(constantsClass, constantName);
        if (result != null) {
            return result;
        }

        try {
            Class<?> commonClass = Class.forName("kroviq.constants.CommonConstants");
            if (!commonClass.equals(constantsClass)) {
                result = resolveDesktopFromClass(commonClass, constantName);
                if (result != null) {
                    logger.debug("Resolved desktop '{}' from CommonConstants (fallback)", constantName);
                    return result;
                }
            }
        } catch (ClassNotFoundException e) {
            logger.debug("CommonConstants class not found, skipping fallback");
        }

        throw new RuntimeException("Desktop element '" + constantName + "' not found in " +
            constantsClass.getSimpleName() + " or CommonConstants for module '" + currentModule + "'");
    }

    public static DesktopLocator resolveDesktopLocator(String pageName, String constantName) {
        return resolveDesktop(pageName, constantName).getLocator();
    }

    private static DesktopElementInfo resolveDesktopFromClass(Class<?> clazz, String constantName) {
        try {
            Field field = clazz.getDeclaredField(constantName);
            field.setAccessible(true);
            Object value = field.get(null);

            if (!(value instanceof String locatorString)) {
                return null;
            }

            if (!DesktopLocator.isDesktopLocator(locatorString)) {
                throw new RuntimeException("Constant '" + constantName + "' value '" + locatorString +
                    "' is not a valid desktop locator (expected 'strategy:value' format)");
            }

            DesktopLocator locator = DesktopLocator.parse(locatorString);
            ActionType actionType = inferActionType(constantName);

            logger.debug("Resolved desktop {}.{} -> {}, ActionType: {}",
                clazz.getSimpleName(), constantName, locator, actionType);

            return new DesktopElementInfo(locator, actionType);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
    
    private static ElementInfo resolveFromClass(Class<?> clazz, String constantName) {
        try {
            Field field = clazz.getDeclaredField(constantName);
            field.setAccessible(true);
            Object value = field.get(null);
            
            if (!(value instanceof String)) {
                return null;
            }
            
            String xpath = (String) value;
            ActionType actionType = inferActionType(constantName);
            
            logger.debug("Resolved {}.{} -> XPath: {}, ActionType: {}", 
                clazz.getSimpleName(), constantName, xpath, actionType);
            
            return new ElementInfo(xpath, actionType);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
    
    private static Class<?> getConstantsClass(String pageName) {
        String normalized = pageName.replace(" ", "").replace("Page", "");
        
        ModuleRegistry.ModuleInfo module = ModuleRegistry.getInstance().getModule(normalized);
        if (module != null && module.constantsClass != null) {
            return module.constantsClass;
        }
        
        for (ModuleRegistry.ModuleInfo m : ModuleRegistry.getInstance().getModules().values()) {
            if (m.name.equalsIgnoreCase(normalized)) {
                return m.constantsClass;
            }
        }
        
        logger.warn("No Constants class found for page '{}'", pageName);
        return null;
    }
    
    private static ActionType inferActionType(String constantName) {
        String upper = constantName.toUpperCase();
        
        // 1. CHECKBOX -- most specific, always clicked
        if (upper.contains("CHECKBOX") || upper.contains("CHK")) {
            return ActionType.CLICK;
        }
        
        // 2. TOGGLE / SWITCH
        if (upper.contains("TOGGLE") || upper.contains("SWITCH")) {
            return ActionType.TOGGLE;
        }
        
        // 3. DROPDOWN (including DRPDWN abbreviation)
        if (upper.contains("DROPDOWN") || upper.contains("DRPDWN") || upper.contains("SELECT")) {
            return ActionType.DROPDOWN;
        }
        
        // 4. UPLOAD
        if (upper.contains("UPLOAD") || upper.contains("FILE_INPUT") || upper.contains("ATTACH")) {
            return ActionType.UPLOAD;
        }
        
        // 5. DATE_PICKER
        if ((upper.contains("DATE") || upper.contains("PICKER")) && 
            !upper.endsWith("_INPUT") && !upper.endsWith("_FIELD")) {
            return ActionType.DATE_PICKER;
        }
        
        // 5. INPUT
        if (upper.contains("INPUT") || upper.contains("FIELD") || upper.contains("SEARCH") || 
            upper.contains("CODE") || upper.contains("NAME") || upper.contains("DESCRIPTION")) {
            return ActionType.INPUT;
        }
        
        // 6. CLICK
        if (upper.contains("BUTTON") || upper.contains("BTN") || upper.contains("ICON") || 
            upper.contains("LINK") || upper.contains("ARROW") || upper.contains("EDIT") || 
            upper.contains("DELETE") || upper.contains("SAVE") || upper.contains("CLOSE") || 
            upper.contains("CANCEL") || upper.contains("ADD") || upper.contains("CLEAR")) {
            return ActionType.CLICK;
        }
        
        // 7. VERIFICATION
        if (upper.contains("DASHBOARD") || upper.contains("PAGE") || upper.contains("TITLE") || 
            upper.contains("ERROR") || upper.contains("MSG") || upper.contains("MESSAGE") ||
            upper.contains("HEADER") || upper.contains("SECTION")) {
            return ActionType.VERIFICATION;
        }
        
        logger.debug("Could not infer ActionType for '{}', defaulting to CLICK", constantName);
        return ActionType.CLICK;
    }
}
