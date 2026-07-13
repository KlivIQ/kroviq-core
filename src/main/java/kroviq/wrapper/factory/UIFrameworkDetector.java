package kroviq.wrapper.factory;

import org.openqa.selenium.WebElement;
import java.util.Map;
import java.util.WeakHashMap;

public class UIFrameworkDetector {

    public enum UIFramework { ANTD, MUI, AG_GRID, ANGULAR_MATERIAL, PRIMENG, UNKNOWN }

    private static final ThreadLocal<Map<WebElement, UIFramework>> cache =
            ThreadLocal.withInitial(WeakHashMap::new);

    private UIFrameworkDetector() {}

    public static UIFramework detect(WebElement element) {
        Map<WebElement, UIFramework> localCache = cache.get();
        UIFramework cached = localCache.get(element);
        if (cached != null) return cached;

        UIFramework result = resolveFramework(element);
        localCache.put(element, result);
        return result;
    }

    public static void clearCache() {
        cache.get().clear();
    }

    private static UIFramework resolveFramework(WebElement element) {
        try {
            String tagName = element.getTagName();
            String classAttr = element.getAttribute("class");
            if (classAttr == null) classAttr = "";

            // AG Grid detection: class contains ag-root, ag-grid, ag-body-viewport, or ag-row
            if (classAttr.contains("ag-root") || classAttr.contains("ag-grid")
                    || classAttr.contains("ag-body-viewport") || classAttr.contains("ag-row")) {
                return UIFramework.AG_GRID;
            }

            // PrimeNG detection: tag starts with "p-" or class contains p-component
            if (tagName != null && tagName.startsWith("p-")) return UIFramework.PRIMENG;
            if (classAttr.contains("p-component") || classAttr.contains("p-element")) {
                return UIFramework.PRIMENG;
            }

            // Angular Material detection: tag starts with "mat-" or class contains mat-mdc/mat-select/mat-table
            if (tagName != null && tagName.startsWith("mat-")) return UIFramework.ANGULAR_MATERIAL;
            if (classAttr.contains("mat-mdc-") || classAttr.contains("mat-select")
                    || classAttr.contains("mat-table") || classAttr.contains("mat-dialog")) {
                return UIFramework.ANGULAR_MATERIAL;
            }

            // MUI detection: class contains "Mui" prefix or "MuiSelect", "MuiAutocomplete", etc.
            if (classAttr.contains("Mui")) return UIFramework.MUI;

            // AntD detection: class contains "ant-" prefix
            if (classAttr.contains("ant-")) return UIFramework.ANTD;

            // Check aria/role attributes for MUI patterns
            String role = element.getAttribute("role");
            if (role != null) {
                // MUI uses role="combobox" with specific aria patterns
                String ariaExpanded = element.getAttribute("aria-expanded");
                String ariaHaspopup = element.getAttribute("aria-haspopup");
                if ("combobox".equals(role) && ariaHaspopup != null) {
                    // Could be MUI or generic — check parent/wrapper class
                    return detectFromAncestor(element);
                }
            }

            // Check data attributes
            String dataTestId = element.getAttribute("data-testid");
            if (dataTestId != null && dataTestId.contains("Mui")) return UIFramework.MUI;

            // Angular Material fallback: check for matInput, matAutocomplete attributes
            String matAutocomplete = element.getAttribute("matAutocomplete");
            if (matAutocomplete != null) return UIFramework.ANGULAR_MATERIAL;
            String matInput = element.getAttribute("matInput");
            if (matInput != null) return UIFramework.ANGULAR_MATERIAL;

        } catch (Exception e) {
            // Fail-safe: return UNKNOWN on any detection error
        }
        return UIFramework.UNKNOWN;
    }

    private static UIFramework detectFromAncestor(WebElement element) {
        try {
            // Check the element's outerHTML for framework markers
            String outerHtml = element.getAttribute("outerHTML");
            if (outerHtml != null) {
                if (outerHtml.contains("Mui")) return UIFramework.MUI;
                if (outerHtml.contains("ant-")) return UIFramework.ANTD;
            }
        } catch (Exception e) {
            // Fail-safe
        }
        return UIFramework.UNKNOWN;
    }
}
