package kroviq.ai.spark.model;

public class SparkInput {
    private final String brdContent;
    private final String moduleName;
    private final String businessContext;
    private final String pageName;

    private SparkInput(Builder builder) {
        this.brdContent = builder.brdContent;
        this.moduleName = builder.moduleName;
        this.businessContext = builder.businessContext;
        this.pageName = builder.pageName;
    }

    public String getBrdContent() { return brdContent; }
    public String getModuleName() { return moduleName; }
    public String getBusinessContext() { return businessContext; }
    public String getPageName() { return pageName != null ? pageName : moduleName + "Page"; }

    public static Builder builder(String brdContent, String moduleName) {
        return new Builder(brdContent, moduleName);
    }

    public static class Builder {
        private final String brdContent;
        private final String moduleName;
        private String businessContext = "";
        private String pageName;

        private Builder(String brdContent, String moduleName) {
            this.brdContent = brdContent;
            this.moduleName = moduleName;
        }

        public Builder businessContext(String ctx) { this.businessContext = ctx; return this; }
        public Builder pageName(String name) { this.pageName = name; return this; }
        public SparkInput build() { return new SparkInput(this); }
    }
}
