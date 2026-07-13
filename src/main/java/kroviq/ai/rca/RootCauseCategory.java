package kroviq.ai.rca;

public enum RootCauseCategory {
    LOCATOR_ISSUE("Locator Issue", "Automation"),
    TIMING_SYNC_ISSUE("Timing/Sync Issue", "Automation"),
    ASSERTION_FAILURE("Assertion Failure", "Application"),
    TEST_DATA_ISSUE("Test Data Issue", "Test Data"),
    ENVIRONMENT_ISSUE("Environment Issue", "Environment"),
    APPLICATION_DEFECT("Application Defect", "Application"),
    DRIVER_BROWSER_ISSUE("Driver/Browser Issue", "Infrastructure"),
    API_DEPENDENCY_FAILURE("API Dependency Failure", "Infrastructure"),
    NETWORK_TIMEOUT("Network Timeout", "Environment"),
    UNKNOWN("Unknown", "Unknown");

    private final String displayName;
    private final String defaultOwner;

    RootCauseCategory(String displayName, String defaultOwner) {
        this.displayName = displayName;
        this.defaultOwner = defaultOwner;
    }

    public String getDisplayName() { return displayName; }

    public String getDefaultOwner() { return defaultOwner; }
}
