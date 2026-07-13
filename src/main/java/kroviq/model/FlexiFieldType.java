package kroviq.model;

public enum FlexiFieldType {

    DROPDOWN("antd-dropdown-component-main-wrapper"),
    TEXTBOX("antd-input-component-main-wrapper"),
    NUMBER("antd-number-component-main-wrapper"),
    RADIO("antd-radio-component-main-wrapper"),
    CHECKBOX("antd-checkbox-component-main-wrapper"),
    TOGGLE("antd-toggle-component-main-wrapper"),
    DATE_PICKER("antd-datepicker-component-main-wrapper"),
    UNKNOWN("");

    private final String cssClassMarker;

    FlexiFieldType(String cssClassMarker) {
        this.cssClassMarker = cssClassMarker;
    }

    public String getCssClassMarker() {
        return cssClassMarker;
    }

    public static FlexiFieldType fromCssClass(String classAttribute) {
        if (classAttribute == null || classAttribute.isEmpty()) {
            return UNKNOWN;
        }
        for (FlexiFieldType type : values()) {
            if (!type.cssClassMarker.isEmpty() && classAttribute.contains(type.cssClassMarker)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
