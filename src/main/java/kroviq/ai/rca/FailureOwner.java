package kroviq.ai.rca;

public enum FailureOwner {
    AUTOMATION("Automation"),
    APPLICATION("Application"),
    ENVIRONMENT("Environment"),
    TEST_DATA("Test Data"),
    INFRASTRUCTURE("Infrastructure"),
    UNKNOWN("Unknown");

    private final String displayName;

    FailureOwner(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
