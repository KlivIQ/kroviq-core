package kroviq.ai.spark.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GherkinResult {
    private final String featureContent;
    private final Map<String, Map<String, String>> testData;
    private final String constantsContent;
    private final List<String> customStepsNeeded;
    private final ReusabilityStats reusability;
    private final String moduleName;

    public GherkinResult(String featureContent, Map<String, Map<String, String>> testData,
                         String constantsContent, List<String> customStepsNeeded,
                         ReusabilityStats reusability, String moduleName) {
        this.featureContent = featureContent;
        this.testData = testData != null ? Collections.unmodifiableMap(testData) : Collections.emptyMap();
        this.constantsContent = constantsContent;
        this.customStepsNeeded = customStepsNeeded != null ? Collections.unmodifiableList(customStepsNeeded) : Collections.emptyList();
        this.reusability = reusability;
        this.moduleName = moduleName;
    }

    public String getFeatureContent() { return featureContent; }
    public Map<String, Map<String, String>> getTestData() { return testData; }
    public String getConstantsContent() { return constantsContent; }
    public List<String> getCustomStepsNeeded() { return customStepsNeeded; }
    public ReusabilityStats getReusability() { return reusability; }
    public String getModuleName() { return moduleName; }
}
