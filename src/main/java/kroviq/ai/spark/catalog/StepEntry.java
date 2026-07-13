package kroviq.ai.spark.catalog;

import java.util.Collections;
import java.util.List;

public class StepEntry {
    private final String keyword;
    private final String pattern;
    private final StepCategory category;
    private final List<String> params;
    private final String source;
    private final String example;

    public StepEntry(String keyword, String pattern, StepCategory category,
                     List<String> params, String source, String example) {
        this.keyword = keyword;
        this.pattern = pattern;
        this.category = category;
        this.params = params != null ? Collections.unmodifiableList(params) : Collections.emptyList();
        this.source = source;
        this.example = example;
    }

    public String getKeyword() { return keyword; }
    public String getPattern() { return pattern; }
    public StepCategory getCategory() { return category; }
    public List<String> getParams() { return params; }
    public String getSource() { return source; }
    public String getExample() { return example; }
}
