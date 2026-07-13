package kroviq.utils;

import io.cucumber.java.Scenario;
import kroviq.model.FlexiField;
import kroviq.wrapper.core.TableRowContext;
import kroviq.ai.rca.RCAResult;
import kroviq.ai.defect.DefectDraft;
import kroviq.wrapper.desktop.core.DesktopEngine;
import kroviq.wrapper.desktop.session.DesktopSessionConfig;
import org.openqa.selenium.WebElement;
import java.util.*;

public class TestContext {
    private static final ThreadLocal<String> currentModule = new ThreadLocal<>();
    private static final ThreadLocal<String> currentTestCaseId = new ThreadLocal<>();
    private static final ThreadLocal<Scenario> currentScenario = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, List<FlexiField>>> flexiFieldStore =
            ThreadLocal.withInitial(LinkedHashMap::new);

    public static void setCurrentModule(String module) { currentModule.set(module); }
    public static String getCurrentModule() {
        String module = currentModule.get();
        if (module == null) {
            throw new RuntimeException("Module context not set. Ensure TestHooks @Before is configured.");
        }
        return module;
    }
    public static void clearCurrentModule() { currentModule.remove(); }

    public static void setCurrentTestCaseId(String id) { currentTestCaseId.set(id); }
    public static String getCurrentTestCaseId() { return currentTestCaseId.get(); }
    public static void clearCurrentTestCaseId() { currentTestCaseId.remove(); }

    public static void setCurrentScenario(Scenario scenario) { currentScenario.set(scenario); }
    public static Scenario getCurrentScenario() { return currentScenario.get(); }
    public static void clearCurrentScenario() { currentScenario.remove(); }

    // -- Flexi Field Storage --

    public static void storeFlexiFields(String blockName, List<FlexiField> fields) {
        if (blockName == null || blockName.isEmpty()) {
            throw new IllegalArgumentException("blockName cannot be null or empty");
        }
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("fields list cannot be null or empty for block '" + blockName + "'");
        }
        flexiFieldStore.get().put(blockName, Collections.unmodifiableList(new ArrayList<>(fields)));
    }

    public static List<FlexiField> getFlexiFields(String blockName) {
        List<FlexiField> fields = flexiFieldStore.get().get(blockName);
        if (fields == null) {
            throw new RuntimeException("No flexi fields stored for block '" + blockName
                    + "'. Available blocks: " + flexiFieldStore.get().keySet());
        }
        return fields;
    }

    public static FlexiField getFlexiFieldByKey(String blockName, String fieldKey) {
        List<FlexiField> fields = getFlexiFields(blockName);
        FlexiField match = null;
        for (FlexiField f : fields) {
            if (f.getFieldKey().equals(fieldKey)) {
                if (match != null) {
                    throw new RuntimeException("Duplicate fieldKey '" + fieldKey
                            + "' in block '" + blockName + "'. Keys must be unique within a block.");
                }
                match = f;
            }
        }
        if (match == null) {
            throw new RuntimeException("Flexi field with key '" + fieldKey
                    + "' not found in block '" + blockName + "'");
        }
        return match;
    }

    public static Map<String, List<FlexiField>> getAllFlexiFields() {
        return Collections.unmodifiableMap(flexiFieldStore.get());
    }

    public static void clearFlexiFields() { flexiFieldStore.get().clear(); }

    // -- Table Row Context --

    private static final ThreadLocal<TableRowContext> currentTableRowContext = new ThreadLocal<>();

    public static void setCurrentTableRow(TableRowContext context) { currentTableRowContext.set(context); }

    public static TableRowContext getCurrentTableRowContext() {
        TableRowContext ctx = currentTableRowContext.get();
        if (ctx == null) {
            throw new RuntimeException("No table row in context. Use a 'find row' step first.");
        }
        return ctx;
    }

    public static WebElement getCurrentTableRow() {
        return getCurrentTableRowContext().getCurrentRow();
    }

    public static boolean hasCurrentTableRow() { return currentTableRowContext.get() != null; }

    public static void clearCurrentTableRow() { currentTableRowContext.remove(); }

    // -- Desktop Engine Context --

    private static final ThreadLocal<DesktopEngine> desktopEngine = new ThreadLocal<>();
    private static final ThreadLocal<DesktopSessionConfig> desktopConfig = new ThreadLocal<>();

    public static void setDesktopEngine(DesktopEngine engine) { desktopEngine.set(engine); }
    public static DesktopEngine getDesktopEngine() { return desktopEngine.get(); }
    public static void clearDesktopEngine() { desktopEngine.remove(); }

    public static void setDesktopConfig(DesktopSessionConfig config) { desktopConfig.set(config); }
    public static DesktopSessionConfig getDesktopConfig() { return desktopConfig.get(); }
    public static void clearDesktopConfig() { desktopConfig.remove(); }

    // -- RCA Result Context --

    private static final ThreadLocal<RCAResult> lastRCAResult = new ThreadLocal<>();

    public static void setLastRCAResult(RCAResult result) { lastRCAResult.set(result); }
    public static RCAResult getLastRCAResult() { return lastRCAResult.get(); }
    public static boolean hasRCAResult() { return lastRCAResult.get() != null; }
    public static void clearLastRCAResult() { lastRCAResult.remove(); }

    // -- Defect Draft Context --

    private static final ThreadLocal<DefectDraft> lastDefectDraft = new ThreadLocal<>();

    public static void setLastDefectDraft(DefectDraft draft) { lastDefectDraft.set(draft); }
    public static DefectDraft getLastDefectDraft() { return lastDefectDraft.get(); }
    public static boolean hasDefectDraft() { return lastDefectDraft.get() != null; }
    public static void clearLastDefectDraft() { lastDefectDraft.remove(); }
}
