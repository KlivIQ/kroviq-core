package kroviq.utils;

import kroviq.utils.ActionType;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SmartLocator {
    
    private static final int MAX_ALTERNATES = 5;
    private static final Pattern ID_PATTERN = Pattern.compile("@id='([^']+)'");
    private static final Pattern CLASS_PATTERN = Pattern.compile("@class='([^']+)'");
    private static final Pattern TEXT_PATTERN = Pattern.compile("text\\(\\)='([^']+)'");
    
    public static List<String> generateAlternateLocators(String primaryLocator, ActionType actionType, String elementName) {
        if (primaryLocator == null || actionType == null) {
            return Collections.emptyList();
        }
        
        List<String> alternates = new ArrayList<>();
        
        switch (actionType) {
            case CLICK:
                alternates.addAll(generateClickAlternates(primaryLocator, elementName));
                break;
            case INPUT:
                alternates.addAll(generateInputAlternates(primaryLocator, elementName));
                break;
            case DROPDOWN:
                alternates.addAll(generateDropdownAlternates(primaryLocator, elementName));
                break;
            case DATE_PICKER:
                alternates.addAll(generateDatePickerAlternates(primaryLocator, elementName));
                break;
            case TOGGLE:
                alternates.addAll(generateToggleAlternates(primaryLocator, elementName));
                break;
            default:
                alternates.addAll(generateGenericAlternates(primaryLocator, elementName));
        }
        
        return alternates.subList(0, Math.min(alternates.size(), MAX_ALTERNATES));
    }
    
    private static List<String> generateClickAlternates(String primary, String elementName) {
        List<String> alternates = new ArrayList<>();
        
        String id = extractId(primary);
        if (id != null) {
            String fragment = getIdFragment(id);
            // ID-based alternates
            alternates.add("//button[contains(@id,'" + fragment + "')]");
            alternates.add("//*[@role='button'][contains(@id,'" + fragment + "')]");
            alternates.add("//a[contains(@id,'" + fragment + "')]");
            
            // Class-based from ID pattern
            if (fragment.contains("Btn") || fragment.contains("Button")) {
                alternates.add("//button[contains(@class,'" + fragment.toLowerCase() + "')]");
            }
        }
        
        if (elementName != null) {
            String displayText = getDisplayText(elementName);
            // Text-based alternates
            alternates.add("//button[.//span[normalize-space()='" + displayText + "']]");
            alternates.add("//*[@role='button'][.//span[normalize-space()='" + displayText + "']]");
            alternates.add("//button[contains(@class,'ant-btn')][.//span[normalize-space()='" + displayText + "']]");
            alternates.add("//a[normalize-space()='" + displayText + "']");
            
            // AntD button patterns
            alternates.add("//button[contains(@class,'ant-btn-primary')][contains(text(),'" + displayText + "')]");
            alternates.add("//span[contains(@class,'ant-btn')][contains(text(),'" + displayText + "')]");
        }
        
        return alternates;
    }
    
    private static List<String> generateInputAlternates(String primary, String elementName) {
        List<String> alternates = new ArrayList<>();
        
        String id = extractId(primary);
        if (id != null) {
            String fragment = getIdFragment(id);
            // ID-based alternates
            alternates.add("//input[contains(@id,'" + fragment + "')]");
            alternates.add("//textarea[contains(@id,'" + fragment + "')]");
            
            // Name attribute from ID
            alternates.add("//input[@name='" + fragment.toLowerCase() + "']");
            alternates.add("//input[@name='" + fragment + "']");
        }
        
        if (elementName != null) {
            String name = elementName.toLowerCase();
            String displayText = getDisplayText(elementName);
            
            // Placeholder-based alternates
            alternates.add("//input[contains(@placeholder,'" + name + "')]");
            alternates.add("//input[contains(@placeholder,'" + displayText + "')]");
            
            // Label-based alternates
            alternates.add("//label[.//span[contains(normalize-space(),'" + displayText + "')]]/following-sibling::*//input");
            alternates.add("//label[contains(normalize-space(),'" + displayText + "')]/following-sibling::input");
            
            // AntD input patterns
            alternates.add("//input[contains(@class,'ant-input')][contains(@placeholder,'" + name + "')]");
            alternates.add("//div[contains(@class,'ant-input-wrapper')]//input[contains(@placeholder,'" + name + "')]");
            
            // Form item context
            alternates.add("//div[contains(@class,'ant-form-item')][.//label[contains(text(),'" + displayText + "')]]//input");
        }
        
        return alternates;
    }
    
    private static List<String> generateDropdownAlternates(String primary, String elementName) {
        List<String> alternates = new ArrayList<>();
        
        String id = extractId(primary);
        if (id != null) {
            String fragment = getIdFragment(id);
            // AntD select patterns by ID
            alternates.add("//div[contains(@id,'" + fragment + "')]//div[contains(@class,'ant-select-selector')]");
            alternates.add("//div[contains(@class,'ant-select')][contains(@id,'" + fragment + "')]");
            alternates.add("//div[contains(@id,'" + fragment + "')]//span[contains(@class,'ant-select-selection-item')]");
            
            // Multiple select patterns
            alternates.add("//div[contains(@id,'" + fragment + "')][contains(@class,'ant-select-multiple')]");
        }
        
        if (elementName != null) {
            String displayText = getDisplayText(elementName);
            
            // Label-based AntD select patterns
            alternates.add("//div[contains(@class,'ant-select')][.//span[contains(text(),'" + displayText + "')]]");
            alternates.add("//div[contains(@class,'ant-form-item')][.//label[contains(text(),'" + displayText + "')]]//div[contains(@class,'ant-select')]");
            
            // Role and aria patterns
            alternates.add("//*[@role='combobox'][contains(@aria-label,'" + displayText + "')]");
            alternates.add("//*[@role='button'][contains(@aria-label,'" + displayText + "')]");
            
            // AntD dropdown trigger patterns
            alternates.add("//div[contains(@class,'ant-select-selector')][contains(@title,'" + displayText + "')]");
            alternates.add("//span[contains(@class,'ant-select-selection-search')]/input[contains(@aria-label,'" + displayText + "')]");
            
            // Cascader patterns (AntD cascading select)
            alternates.add("//div[contains(@class,'ant-cascader')][.//input[contains(@placeholder,'" + displayText + "')]]");
        }
        
        return alternates;
    }
    
    private static List<String> generateDatePickerAlternates(String primary, String elementName) {
        List<String> alternates = new ArrayList<>();
        
        String id = extractId(primary);
        if (id != null) {
            String fragment = getIdFragment(id);
            // AntD picker wrapper patterns
            alternates.add("//div[contains(@id,'" + fragment + "')]//div[contains(@class,'ant-picker')]");
            alternates.add("//div[contains(@class,'ant-picker')][contains(@id,'" + fragment + "')]");
            
            // Input within picker
            alternates.add("//div[contains(@id,'" + fragment + "')]//input[contains(@class,'ant-picker-input')]");
            alternates.add("//input[contains(@id,'" + fragment + "')][contains(@class,'ant-picker-input')]");
            
            // Date range picker patterns
            alternates.add("//div[contains(@id,'" + fragment + "')][contains(@class,'ant-picker-range')]");
        }
        
        if (elementName != null) {
            String displayText = getDisplayText(elementName);
            boolean isDateField = elementName.toLowerCase().contains("date") || 
                                elementName.toLowerCase().contains("effective") ||
                                elementName.toLowerCase().contains("from") ||
                                elementName.toLowerCase().contains("to");
            
            if (isDateField) {
                // Label-based picker patterns
                alternates.add("//label[.//span[normalize-space()='" + displayText + "']]/following-sibling::*//div[contains(@class,'ant-picker')]");
                alternates.add("//div[contains(@class,'ant-form-item')][.//label[contains(text(),'" + displayText + "')]]//div[contains(@class,'ant-picker')]");
                
                // Picker state patterns
                alternates.add("//div[contains(@class,'ant-picker')][contains(@class,'ant-picker-focused')]");
                alternates.add("//div[contains(@class,'ant-picker')][contains(@class,'ant-picker-status-error')]");
                
                // Time picker patterns
                alternates.add("//div[contains(@class,'ant-picker')][contains(@class,'ant-picker-time')]");
                
                // Placeholder-based input patterns
                alternates.add("//input[contains(@placeholder,'" + displayText + "')][contains(@class,'ant-picker-input')]");
            }
        }
        
        return alternates;
    }
    
    private static List<String> generateToggleAlternates(String primary, String elementName) {
        List<String> alternates = new ArrayList<>();
        
        String id = extractId(primary);
        if (id != null) {
            String fragment = getIdFragment(id);
            // AntD switch patterns by ID
            alternates.add("//div[contains(@id,'" + fragment + "')]//button[contains(@class,'ant-switch')]");
            alternates.add("//button[contains(@class,'ant-switch')][contains(@id,'" + fragment + "')]");
            alternates.add("//button[@role='switch'][contains(@id,'" + fragment + "')]");
            
            // Switch handle patterns
            alternates.add("//div[contains(@id,'" + fragment + "')]//div[contains(@class,'ant-switch-handle')]");
        }
        
        if (elementName != null) {
            String displayText = getDisplayText(elementName);
            
            // Role and aria-based patterns
            alternates.add("//button[@role='switch'][contains(@aria-label,'" + displayText + "')]");
            alternates.add("//button[contains(@class,'ant-switch')][contains(@aria-label,'" + displayText + "')]");
            
            // Label-based patterns
            alternates.add("//label[.//span[normalize-space()='" + displayText + "']]/following-sibling::*//button[contains(@class,'ant-switch')]");
            alternates.add("//div[contains(@class,'ant-form-item')][.//label[contains(text(),'" + displayText + "')]]//button[contains(@class,'ant-switch')]");
            
            // State-based patterns
            alternates.add("//button[contains(@class,'ant-switch')][contains(@class,'ant-switch-checked')]");
            alternates.add("//button[contains(@class,'ant-switch')][not(contains(@class,'ant-switch-checked'))]");
            
            // Generic switch patterns
            alternates.add("//div[contains(@class,'ant-switch')]");
            alternates.add("//*[@role='switch']");
        }
        
        return alternates;
    }
    
    private static List<String> generateGenericAlternates(String primary, String elementName) {
        List<String> alternates = new ArrayList<>();
        
        String id = extractId(primary);
        if (id != null) {
            String fragment = getIdFragment(id);
            alternates.add("//*[contains(@id,'" + fragment + "')]");
        }
        
        String className = extractClass(primary);
        if (className != null) {
            alternates.add("//*[contains(@class,'" + className + "')]");
        }
        
        return alternates;
    }
    
    private static String extractId(String xpath) {
        Matcher matcher = ID_PATTERN.matcher(xpath);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private static String extractClass(String xpath) {
        Matcher matcher = CLASS_PATTERN.matcher(xpath);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private static String getIdFragment(String id) {
        if (id.contains("_")) {
            String[] parts = id.split("_");
            return parts[parts.length - 1];
        }
        return id;
    }
    
    private static String getDisplayText(String elementName) {
        if (elementName == null) return "";
        
        return elementName.replaceAll("([a-z])([A-Z])", "$1 $2")
                         .replace("button", "")
                         .replace("field", "")
                         .replace("dropdown", "")
                         .replace("toggle", "")
                         .replace("date", "")
                         .replace("picker", "")
                         .replace("effective", "Effective")
                         .replace("from", "From")
                         .replace("to", "To")
                         .replaceAll("\\s+", " ")
                         .trim();
    }
}