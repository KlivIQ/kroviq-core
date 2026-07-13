package kroviq.reporting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Converts technical step descriptions into business-readable messages.
 * Falls back to original description if enrichment fails.
 */
public class ReadableMessageBuilder {
    private static final Logger logger = LogManager.getLogger(ReadableMessageBuilder.class);
    
    /**
     * Main entry point - builds readable message from step context.
     * Returns original stepDescription if enrichment fails.
     */
    public static String buildReadableMessage(
        String stepDescription,
        String elementName,
        String pageName,
        String resolvedValue
    ) {
        try {
            if (stepDescription == null || stepDescription.isEmpty()) {
                return stepDescription;
            }
            
            // Detect action type from step description
            String lowerDesc = stepDescription.toLowerCase();
            
            if (lowerDesc.contains("click")) {
                return formatClickMessage(elementName != null ? elementName : extractElement(stepDescription));
            } else if (lowerDesc.contains("enter") || lowerDesc.contains("input")) {
                String field = elementName != null ? elementName : extractField(stepDescription);
                String value = resolvedValue != null ? resolvedValue : extractValue(stepDescription);
                return formatInputMessage(field, value);
            } else if (lowerDesc.contains("select") && lowerDesc.contains("from")) {
                String dropdown = elementName != null ? elementName : extractDropdown(stepDescription);
                String option = resolvedValue != null ? resolvedValue : extractOption(stepDescription);
                return formatDropdownMessage(dropdown, option);
            } else if (lowerDesc.contains("set") && (lowerDesc.contains("date") || lowerDesc.contains("to"))) {
                if (lowerDesc.contains("date")) {
                    String field = elementName != null ? elementName : extractField(stepDescription);
                    String date = resolvedValue != null ? resolvedValue : extractValue(stepDescription);
                    return formatDateMessage(field, date);
                } else if (lowerDesc.contains("toggle") || lowerDesc.contains("true") || lowerDesc.contains("false")) {
                    String toggle = elementName != null ? elementName : extractField(stepDescription);
                    String state = resolvedValue != null ? resolvedValue : extractValue(stepDescription);
                    return formatToggleMessage(toggle, state);
                }
            } else if (lowerDesc.contains("validate") || lowerDesc.contains("verify")) {
                return formatValidationMessage(stepDescription);
            }
            
            // Fallback: return original
            return stepDescription;
            
        } catch (Exception e) {
            logger.debug("Message enrichment failed, using original: {}", e.getMessage());
            return stepDescription;
        }
    }
    
    /**
     * Builds clean failure message without technical details.
     * Technical errors are logged separately by the caller.
     */
    public static String buildFailureMessage(
        String stepDescription,
        String elementName,
        String pageName,
        Exception exception
    ) {
        try {
            if (stepDescription == null || stepDescription.isEmpty()) {
                return "Step execution failed";
            }
            
            // Extract action type from step description
            String lowerDesc = stepDescription.toLowerCase();
            String action = "perform action on";
            
            if (lowerDesc.contains("click")) {
                action = "click";
            } else if (lowerDesc.contains("enter") || lowerDesc.contains("input")) {
                action = "enter text in";
            } else if (lowerDesc.contains("select")) {
                action = "select from";
            } else if (lowerDesc.contains("set") && lowerDesc.contains("date")) {
                action = "set date for";
            } else if (lowerDesc.contains("toggle") || lowerDesc.contains("set")) {
                action = "toggle";
            } else if (lowerDesc.contains("validate") || lowerDesc.contains("verify")) {
                action = "validate";
            }
            
            // Build clean message using element name if available
            String fieldName = elementName != null ? formatFieldName(elementName) : extractElement(stepDescription);
            if (fieldName == null || fieldName.isEmpty()) {
                fieldName = "element";
            } else {
                fieldName = formatFieldName(fieldName);
            }
            
            return String.format("Failed to %s %s", action, fieldName);
            
        } catch (Exception e) {
            logger.debug("Failure message building failed: {}", e.getMessage());
            return "Step execution failed";
        }
    }
    
    private static String formatClickMessage(String element) {
        if (element == null || element.isEmpty()) return "Clicked element";
        return String.format("Clicked '%s'", formatFieldName(element));
    }
    
    private static String formatInputMessage(String field, String value) {
        if (field == null || field.isEmpty()) return "Entered text";
        
        // Hide sensitive data patterns
        if (field.toLowerCase().contains("password")) {
            return String.format("Entered password in %s field", formatFieldName(field));
        }
        
        // Check if value is TD_ key or empty
        if (value == null || value.isEmpty() || value.startsWith("TD_")) {
            return String.format("Entered text in %s field", formatFieldName(field));
        }
        
        return String.format("Entered '%s' in %s field", value, formatFieldName(field));
    }
    
    private static String formatDropdownMessage(String dropdown, String option) {
        if (dropdown == null || dropdown.isEmpty()) return "Selected dropdown option";
        
        // Check if option is TD_ key or empty
        if (option == null || option.isEmpty() || option.startsWith("TD_")) {
            return String.format("Selected option from %s dropdown", formatFieldName(dropdown));
        }
        
        return String.format("Selected '%s' from %s dropdown", option, formatFieldName(dropdown));
    }
    
    private static String formatToggleMessage(String toggle, String state) {
        if (toggle == null || toggle.isEmpty()) return "Toggled switch";
        
        boolean isOn = "true".equalsIgnoreCase(state) || "yes".equalsIgnoreCase(state) || "on".equalsIgnoreCase(state);
        return String.format("Set %s to %s", formatFieldName(toggle), isOn ? "ON" : "OFF");
    }
    
    private static String formatDateMessage(String field, String date) {
        if (field == null || field.isEmpty()) return "Set date";
        if (date == null || date.isEmpty()) return String.format("Set %s date", formatFieldName(field));
        return String.format("Set %s to %s", formatFieldName(field), date);
    }
    
    private static String formatValidationMessage(String stepDescription) {
        // Keep validation messages mostly as-is, just clean up formatting
        return stepDescription.replace("'", "'").replace("\"", "");
    }
    
    /**
     * Converts technical field names to readable format.
     * Example: "UW_POLICY_START_DATE" -> "Policy Start Date"
     */
    private static String formatFieldName(String technicalName) {
        if (technicalName == null || technicalName.isEmpty()) {
            return technicalName;
        }
        
        // Remove common prefixes
        String cleaned = technicalName
            .replaceFirst("^(UW_|TD_|BTN_|TXT_|DDL_|CHK_)", "")
            .replace("_", " ")
            .toLowerCase();
        
        // Capitalize first letter of each word
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : cleaned.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    // Extraction helpers - parse from step description
    
    private static String extractElement(String stepDescription) {
        return extractQuoted(stepDescription, 0);
    }
    
    private static String extractField(String stepDescription) {
        return extractQuoted(stepDescription, 1);
    }
    
    private static String extractValue(String stepDescription) {
        return extractQuoted(stepDescription, 0);
    }
    
    private static String extractOption(String stepDescription) {
        return extractQuoted(stepDescription, 0);
    }
    
    private static String extractDropdown(String stepDescription) {
        return extractQuoted(stepDescription, 1);
    }
    
    /**
     * Extracts text within quotes from step description.
     * @param index 0 for first quoted text, 1 for second, etc.
     */
    private static String extractQuoted(String text, int index) {
        if (text == null) return "";
        
        int count = 0;
        int start = -1;
        boolean inQuote = false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '\'' || c == '"') && (i == 0 || text.charAt(i - 1) != '\\')) {
                if (!inQuote) {
                    if (count == index) {
                        start = i + 1;
                    }
                    inQuote = true;
                } else {
                    if (count == index) {
                        return text.substring(start, i);
                    }
                    count++;
                    inQuote = false;
                }
            }
        }
        
        return "";
    }
}
