package kroviq.ai.spark.model;

import java.util.Collections;
import java.util.List;

public class SparkOutput {
    private final List<String> filesWritten;
    private final List<String> warnings;
    private final ReusabilityStats reusability;
    private final String previewDir;

    public SparkOutput(List<String> filesWritten, List<String> warnings,
                       ReusabilityStats reusability, String previewDir) {
        this.filesWritten = filesWritten != null ? Collections.unmodifiableList(filesWritten) : Collections.emptyList();
        this.warnings = warnings != null ? Collections.unmodifiableList(warnings) : Collections.emptyList();
        this.reusability = reusability;
        this.previewDir = previewDir;
    }

    public List<String> getFilesWritten() { return filesWritten; }
    public List<String> getWarnings() { return warnings; }
    public ReusabilityStats getReusability() { return reusability; }
    public String getPreviewDir() { return previewDir; }
}
