package kroviq.ai.defect;

public class DefectTemplateRenderer {

    public static String renderHtmlCard(DefectDraft draft) {
        if (draft == null) return "";

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"defect-card\">\n");
        html.append("  <div class=\"defect-header\">\n");
        html.append("    <span class=\"defect-icon\">&#128196;</span>\n");
        html.append("    <span class=\"defect-title\">AI Defect Draft</span>\n");
        html.append("    <span class=\"defect-badge\" style=\"background:").append(draft.getDefectClassification().getBadgeColor()).append(";color:white;\">");
        html.append(escapeHtml(draft.getDefectClassification().getDisplayName())).append("</span>\n");
        html.append("  </div>\n");

        html.append("  <div class=\"defect-body\">\n");

        // Title
        html.append("    <div class=\"defect-section\">\n");
        html.append("      <span class=\"defect-label\">Defect Title</span>\n");
        html.append("      <p class=\"defect-text defect-title-text\">").append(escapeHtml(draft.getDefectTitle())).append("</p>\n");
        html.append("    </div>\n");

        // Classification + Severity + Owner row
        html.append("    <div class=\"defect-row\">\n");
        html.append("      <div class=\"defect-field\">\n");
        html.append("        <span class=\"defect-label\">Classification</span>\n");
        html.append("        <span class=\"defect-value\" style=\"color:").append(draft.getDefectClassification().getBadgeColor()).append(";\">");
        html.append(escapeHtml(draft.getDefectClassification().getDisplayName())).append("</span>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"defect-field\">\n");
        html.append("        <span class=\"defect-label\">Severity</span>\n");
        html.append("        <span class=\"defect-value defect-severity-").append(draft.getSeverity().toLowerCase()).append("\">");
        html.append(escapeHtml(draft.getSeverity())).append("</span>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"defect-field\">\n");
        html.append("        <span class=\"defect-label\">Likely Owner</span>\n");
        html.append("        <span class=\"defect-value\">").append(escapeHtml(draft.getLikelyOwner())).append("</span>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        // Executive Summary
        html.append("    <div class=\"defect-section\">\n");
        html.append("      <span class=\"defect-label\">Executive Summary</span>\n");
        html.append("      <p class=\"defect-text\">").append(escapeHtml(draft.getExecutiveSummary())).append("</p>\n");
        html.append("    </div>\n");

        // Reproduction Steps
        html.append("    <div class=\"defect-section\">\n");
        html.append("      <span class=\"defect-label\">Reproduction Steps</span>\n");
        html.append("      <pre class=\"defect-pre\">").append(escapeHtml(draft.getReproductionSteps())).append("</pre>\n");
        html.append("    </div>\n");

        // Expected vs Actual
        html.append("    <div class=\"defect-row\">\n");
        html.append("      <div class=\"defect-field defect-field-wide\">\n");
        html.append("        <span class=\"defect-label\">Expected Result</span>\n");
        html.append("        <p class=\"defect-text\">").append(escapeHtml(draft.getExpectedResult())).append("</p>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"defect-field defect-field-wide\">\n");
        html.append("        <span class=\"defect-label\">Actual Result</span>\n");
        html.append("        <p class=\"defect-text defect-actual\">").append(escapeHtml(draft.getActualResult())).append("</p>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        // Root Cause
        html.append("    <div class=\"defect-section\">\n");
        html.append("      <span class=\"defect-label\">Root Cause Analysis</span>\n");
        html.append("      <p class=\"defect-text\">").append(escapeHtml(draft.getProbableRootCause())).append("</p>\n");
        html.append("    </div>\n");

        // Flags
        html.append("    <div class=\"defect-flags\">\n");
        html.append("      <span class=\"defect-flag ").append(draft.isRetryRecommended() ? "defect-flag-retry" : "defect-flag-no").append("\">");
        html.append("Retry: ").append(draft.isRetryRecommended() ? "Recommended" : "Not Recommended").append("</span>\n");
        html.append("      <span class=\"defect-flag ").append(draft.isDefectRecommended() ? "defect-flag-defect" : "defect-flag-no").append("\">");
        html.append("Log Defect: ").append(draft.isDefectRecommended() ? "Yes" : "No").append("</span>\n");
        html.append("    </div>\n");

        // Environment
        if (!draft.getEnvironmentDetails().isEmpty()) {
            html.append("    <div class=\"defect-section defect-env\">\n");
            html.append("      <span class=\"defect-label\">Environment</span>\n");
            html.append("      <span class=\"defect-env-text\">").append(escapeHtml(draft.getEnvironmentDetails())).append("</span>\n");
            html.append("    </div>\n");
        }

        html.append("  </div>\n");
        html.append("</div>\n");

        return html.toString();
    }

    public static String renderMarkdown(DefectDraft draft) {
        if (draft == null) return "";

        StringBuilder md = new StringBuilder();
        md.append("## ").append(draft.getDefectTitle()).append("\n\n");
        md.append("**Classification:** ").append(draft.getDefectClassification().getDisplayName()).append("\n");
        md.append("**Severity:** ").append(draft.getSeverity()).append("\n");
        md.append("**Likely Owner:** ").append(draft.getLikelyOwner()).append("\n");
        md.append("**Module:** ").append(draft.getImpactedModule()).append("\n");
        md.append("**Test Case:** ").append(draft.getTestCaseId()).append("\n\n");

        md.append("### Executive Summary\n");
        md.append(draft.getExecutiveSummary()).append("\n\n");

        md.append("### Reproduction Steps\n");
        md.append(draft.getReproductionSteps()).append("\n\n");

        md.append("### Expected Result\n");
        md.append(draft.getExpectedResult()).append("\n\n");

        md.append("### Actual Result\n");
        md.append(draft.getActualResult()).append("\n\n");

        md.append("### Root Cause Analysis\n");
        md.append(draft.getProbableRootCause()).append("\n\n");

        md.append("### Recommendations\n");
        md.append("- Retry: ").append(draft.isRetryRecommended() ? "Recommended" : "Not Recommended").append("\n");
        md.append("- Log Defect: ").append(draft.isDefectRecommended() ? "Yes" : "No").append("\n\n");

        if (!draft.getEnvironmentDetails().isEmpty()) {
            md.append("### Environment\n");
            md.append(draft.getEnvironmentDetails()).append("\n\n");
        }

        if (!draft.getScreenshots().isEmpty()) {
            md.append("### Evidence\n");
            for (String screenshot : draft.getScreenshots()) {
                md.append("- Screenshot: ").append(screenshot).append("\n");
            }
        }

        return md.toString();
    }

    public static String renderPlainText(DefectDraft draft) {
        if (draft == null) return "";

        StringBuilder txt = new StringBuilder();
        txt.append("DEFECT DRAFT\n");
        txt.append("============\n\n");
        txt.append("Title: ").append(draft.getDefectTitle()).append("\n");
        txt.append("Classification: ").append(draft.getDefectClassification().getDisplayName()).append("\n");
        txt.append("Severity: ").append(draft.getSeverity()).append("\n");
        txt.append("Likely Owner: ").append(draft.getLikelyOwner()).append("\n");
        txt.append("Module: ").append(draft.getImpactedModule()).append("\n");
        txt.append("Test Case: ").append(draft.getTestCaseId()).append("\n\n");

        txt.append("EXECUTIVE SUMMARY\n");
        txt.append(draft.getExecutiveSummary()).append("\n\n");

        txt.append("REPRODUCTION STEPS\n");
        txt.append(draft.getReproductionSteps()).append("\n\n");

        txt.append("EXPECTED RESULT\n");
        txt.append(draft.getExpectedResult()).append("\n\n");

        txt.append("ACTUAL RESULT\n");
        txt.append(draft.getActualResult()).append("\n\n");

        txt.append("ROOT CAUSE\n");
        txt.append(draft.getProbableRootCause()).append("\n\n");

        txt.append("RECOMMENDATIONS\n");
        txt.append("Retry: ").append(draft.isRetryRecommended() ? "Recommended" : "Not Recommended").append("\n");
        txt.append("Log Defect: ").append(draft.isDefectRecommended() ? "Yes" : "No").append("\n\n");

        if (!draft.getEnvironmentDetails().isEmpty()) {
            txt.append("ENVIRONMENT\n");
            txt.append(draft.getEnvironmentDetails()).append("\n");
        }

        return txt.toString();
    }

    public static String getDefectCssStyles() {
        return ".defect-card { border: 1px solid #2c3e50; border-left: 4px solid #2c3e50; border-radius: 8px; margin: 15px 0; background: #f8f9fa; overflow: hidden; }\n" +
                ".defect-header { display: flex; align-items: center; gap: 10px; padding: 12px 16px; background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%); color: white; }\n" +
                ".defect-icon { font-size: 18px; }\n" +
                ".defect-title { font-weight: 700; font-size: 14px; text-transform: uppercase; letter-spacing: 1px; flex: 1; }\n" +
                ".defect-badge { font-size: 11px; padding: 3px 10px; border-radius: 12px; font-weight: 600; }\n" +
                ".defect-body { padding: 16px; }\n" +
                ".defect-row { display: flex; gap: 20px; margin-bottom: 14px; flex-wrap: wrap; }\n" +
                ".defect-field { flex: 1; min-width: 120px; }\n" +
                ".defect-field-wide { flex: 1; min-width: 200px; }\n" +
                ".defect-label { display: block; font-size: 11px; color: #888; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; font-weight: 600; }\n" +
                ".defect-value { font-size: 14px; font-weight: 600; color: #333; }\n" +
                ".defect-section { margin-bottom: 14px; }\n" +
                ".defect-text { font-size: 13px; color: #444; line-height: 1.5; margin-top: 4px; }\n" +
                ".defect-title-text { font-size: 15px; font-weight: 600; color: #2c3e50; }\n" +
                ".defect-actual { color: #c0392b; font-weight: 500; }\n" +
                ".defect-pre { font-family: 'Courier New', monospace; font-size: 12px; background: #fff; padding: 10px; border-radius: 4px; border: 1px solid #e0e0e0; white-space: pre-wrap; line-height: 1.6; margin-top: 4px; }\n" +
                ".defect-flags { display: flex; gap: 12px; margin-top: 12px; padding-top: 12px; border-top: 1px solid #e0e0e0; }\n" +
                ".defect-flag { font-size: 12px; padding: 4px 10px; border-radius: 4px; font-weight: 600; }\n" +
                ".defect-flag-retry { background: #fff3cd; color: #856404; }\n" +
                ".defect-flag-defect { background: #fdecea; color: #c0392b; }\n" +
                ".defect-flag-no { background: #eafaf1; color: #27ae60; }\n" +
                ".defect-env { padding: 8px 12px; background: #eef2f7; border-radius: 4px; border-left: 3px solid #667eea; }\n" +
                ".defect-env-text { font-size: 12px; color: #555; }\n" +
                ".defect-severity-critical { color: #c0392b; font-weight: 700; }\n" +
                ".defect-severity-high { color: #e74c3c; font-weight: 600; }\n" +
                ".defect-severity-medium { color: #f39c12; font-weight: 600; }\n" +
                ".defect-severity-low { color: #27ae60; font-weight: 600; }\n";
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
