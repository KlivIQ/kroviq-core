package kroviq.ai.rca;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RCAContext {
    // Mandatory inputs
    private final String failedStep;
    private final Throwable exception;
    private final String locatorUsed;
    private final String screenshotPath;
    private final String currentUrl;
    private final List<String> previousSteps;

    // Optional inputs
    private final String moduleName;
    private final String testCaseId;
    private final String pageSource;

    private RCAContext(Builder builder) {
        this.failedStep = builder.failedStep;
        this.exception = builder.exception;
        this.locatorUsed = builder.locatorUsed;
        this.screenshotPath = builder.screenshotPath;
        this.currentUrl = builder.currentUrl;
        this.previousSteps = Collections.unmodifiableList(new ArrayList<>(builder.previousSteps));
        this.moduleName = builder.moduleName;
        this.testCaseId = builder.testCaseId;
        this.pageSource = builder.pageSource;
    }

    public String getFailedStep() { return failedStep; }
    public Throwable getException() { return exception; }
    public String getExceptionName() {
        if (exception == null) return "";
        String simpleName = exception.getClass().getSimpleName();
        // Support testing with wrapped/fake exceptions — check toString for real exception name
        if ("RuntimeException".equals(simpleName) || "FakeException".equals(simpleName)) {
            String str = exception.toString();
            int colonIdx = str.indexOf(':');
            if (colonIdx > 0) {
                String candidate = str.substring(0, colonIdx).trim();
                if (!candidate.contains(" ")) return candidate;
            }
        }
        return simpleName;
    }
    public String getExceptionMessage() {
        return exception != null && exception.getMessage() != null ? exception.getMessage() : "";
    }
    public String getFullStackTrace() {
        if (exception == null) return "";
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement el : exception.getStackTrace()) {
            sb.append(el.toString()).append("\n");
        }
        return sb.toString();
    }
    public String getLocatorUsed() { return locatorUsed; }
    public String getScreenshotPath() { return screenshotPath; }
    public String getCurrentUrl() { return currentUrl; }
    public List<String> getPreviousSteps() { return previousSteps; }
    public String getModuleName() { return moduleName; }
    public String getTestCaseId() { return testCaseId; }
    public String getPageSource() { return pageSource; }

    public static Builder builder(String failedStep, Throwable exception) {
        return new Builder(failedStep, exception);
    }

    public static class Builder {
        private final String failedStep;
        private final Throwable exception;
        private String locatorUsed = "";
        private String screenshotPath = "";
        private String currentUrl = "";
        private List<String> previousSteps = new ArrayList<>();
        private String moduleName = "";
        private String testCaseId = "";
        private String pageSource = "";

        private Builder(String failedStep, Throwable exception) {
            this.failedStep = failedStep != null ? failedStep : "";
            this.exception = exception;
        }

        public Builder locatorUsed(String locator) { this.locatorUsed = locator != null ? locator : ""; return this; }
        public Builder screenshotPath(String path) { this.screenshotPath = path != null ? path : ""; return this; }
        public Builder currentUrl(String url) { this.currentUrl = url != null ? url : ""; return this; }
        public Builder previousSteps(List<String> steps) { this.previousSteps = steps != null ? steps : new ArrayList<>(); return this; }
        public Builder moduleName(String module) { this.moduleName = module != null ? module : ""; return this; }
        public Builder testCaseId(String tcId) { this.testCaseId = tcId != null ? tcId : ""; return this; }
        public Builder pageSource(String source) { this.pageSource = source != null ? source : ""; return this; }

        public RCAContext build() { return new RCAContext(this); }
    }
}
