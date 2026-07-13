package kroviq.ai.defect;

import kroviq.ai.rca.RCAResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefectContext {
    private final RCAResult rcaResult;
    private final String failedStep;
    private final String testCaseId;
    private final String moduleName;
    private final String scenarioName;
    private final List<String> previousSteps;
    private final List<String> screenshotPaths;
    private final String currentUrl;
    private final String environmentName;
    private final String browser;
    private final String exceptionMessage;

    private DefectContext(Builder builder) {
        this.rcaResult = builder.rcaResult;
        this.failedStep = builder.failedStep;
        this.testCaseId = builder.testCaseId;
        this.moduleName = builder.moduleName;
        this.scenarioName = builder.scenarioName;
        this.previousSteps = Collections.unmodifiableList(new ArrayList<>(builder.previousSteps));
        this.screenshotPaths = Collections.unmodifiableList(new ArrayList<>(builder.screenshotPaths));
        this.currentUrl = builder.currentUrl;
        this.environmentName = builder.environmentName;
        this.browser = builder.browser;
        this.exceptionMessage = builder.exceptionMessage;
    }

    public RCAResult getRcaResult() { return rcaResult; }
    public String getFailedStep() { return failedStep; }
    public String getTestCaseId() { return testCaseId; }
    public String getModuleName() { return moduleName; }
    public String getScenarioName() { return scenarioName; }
    public List<String> getPreviousSteps() { return previousSteps; }
    public List<String> getScreenshotPaths() { return screenshotPaths; }
    public String getCurrentUrl() { return currentUrl; }
    public String getEnvironmentName() { return environmentName; }
    public String getBrowser() { return browser; }
    public String getExceptionMessage() { return exceptionMessage; }

    public static Builder builder(RCAResult rcaResult) {
        return new Builder(rcaResult);
    }

    public static class Builder {
        private final RCAResult rcaResult;
        private String failedStep = "";
        private String testCaseId = "";
        private String moduleName = "";
        private String scenarioName = "";
        private List<String> previousSteps = new ArrayList<>();
        private List<String> screenshotPaths = new ArrayList<>();
        private String currentUrl = "";
        private String environmentName = "";
        private String browser = "";
        private String exceptionMessage = "";

        private Builder(RCAResult rcaResult) { this.rcaResult = rcaResult; }

        public Builder failedStep(String v) { this.failedStep = v != null ? v : ""; return this; }
        public Builder testCaseId(String v) { this.testCaseId = v != null ? v : ""; return this; }
        public Builder moduleName(String v) { this.moduleName = v != null ? v : ""; return this; }
        public Builder scenarioName(String v) { this.scenarioName = v != null ? v : ""; return this; }
        public Builder previousSteps(List<String> v) { this.previousSteps = v != null ? v : new ArrayList<>(); return this; }
        public Builder screenshotPaths(List<String> v) { this.screenshotPaths = v != null ? v : new ArrayList<>(); return this; }
        public Builder currentUrl(String v) { this.currentUrl = v != null ? v : ""; return this; }
        public Builder environmentName(String v) { this.environmentName = v != null ? v : ""; return this; }
        public Builder browser(String v) { this.browser = v != null ? v : ""; return this; }
        public Builder exceptionMessage(String v) { this.exceptionMessage = v != null ? v : ""; return this; }

        public DefectContext build() { return new DefectContext(this); }
    }
}
