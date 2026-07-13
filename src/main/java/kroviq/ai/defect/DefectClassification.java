package kroviq.ai.defect;

public enum DefectClassification {
    FUNCTIONAL_DEFECT("Functional Defect", "#dc3545"),
    AUTOMATION_DEFECT("Automation Defect", "#007bff"),
    ENVIRONMENT_ISSUE("Environment Issue", "#ffc107"),
    TEST_DATA_ISSUE("Test Data Issue", "#6f42c1"),
    INFRASTRUCTURE_ISSUE("Infrastructure Issue", "#fd7e14"),
    PRODUCT_ENHANCEMENT("Product Enhancement", "#28a745"),
    UNKNOWN_REVIEW_REQUIRED("Unknown — Review Required", "#6c757d");

    private final String displayName;
    private final String badgeColor;

    DefectClassification(String displayName, String badgeColor) {
        this.displayName = displayName;
        this.badgeColor = badgeColor;
    }

    public String getDisplayName() { return displayName; }

    public String getBadgeColor() { return badgeColor; }
}
