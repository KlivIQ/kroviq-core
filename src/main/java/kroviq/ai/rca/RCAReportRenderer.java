package kroviq.ai.rca;

public class RCAReportRenderer {

    public static String renderHtmlCard(RCAResult result) {
        if (result == null) return "";

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"rca-card\">\n");
        html.append("  <div class=\"rca-header\">\n");
        html.append("    <span class=\"rca-icon\">&#9889;</span>\n");
        html.append("    <span class=\"rca-title\">Failure RCA</span>\n");
        html.append("    <span class=\"rca-confidence ").append(getConfidenceClass(result.getConfidenceScore())).append("\">");
        html.append(result.getConfidenceScore()).append("% confidence</span>\n");
        html.append("  </div>\n");

        html.append("  <div class=\"rca-body\">\n");

        // Category + Owner row
        html.append("    <div class=\"rca-row\">\n");
        html.append("      <div class=\"rca-field\">\n");
        html.append("        <span class=\"rca-label\">Category</span>\n");
        html.append("        <span class=\"rca-value rca-category\">");
        html.append(escapeHtml(result.getCategory().getDisplayName())).append("</span>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"rca-field\">\n");
        html.append("        <span class=\"rca-label\">Likely Owner</span>\n");
        html.append("        <span class=\"rca-value rca-owner\">");
        html.append(escapeHtml(result.getLikelyOwner().getDisplayName())).append("</span>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"rca-field\">\n");
        html.append("        <span class=\"rca-label\">Severity</span>\n");
        html.append("        <span class=\"rca-value rca-severity ").append(getSeverityClass(result.getSuggestedSeverity())).append("\">");
        html.append(escapeHtml(result.getSuggestedSeverity())).append("</span>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        // Root Cause
        html.append("    <div class=\"rca-section\">\n");
        html.append("      <span class=\"rca-label\">Root Cause</span>\n");
        html.append("      <p class=\"rca-text\">").append(escapeHtml(result.getProbableRootCause())).append("</p>\n");
        html.append("    </div>\n");

        // Evidence
        html.append("    <div class=\"rca-section\">\n");
        html.append("      <span class=\"rca-label\">Evidence</span>\n");
        html.append("      <p class=\"rca-text rca-evidence\">").append(escapeHtml(result.getEvidenceObserved())).append("</p>\n");
        html.append("    </div>\n");

        // Recommended Action
        html.append("    <div class=\"rca-section\">\n");
        html.append("      <span class=\"rca-label\">Recommended Action</span>\n");
        html.append("      <p class=\"rca-text\">").append(escapeHtml(result.getRecommendedFix())).append("</p>\n");
        html.append("    </div>\n");

        // Flags row
        html.append("    <div class=\"rca-flags\">\n");
        html.append("      <span class=\"rca-flag ").append(result.isRetryRecommended() ? "rca-flag-yes" : "rca-flag-no").append("\">");
        html.append("Retry: ").append(result.isRetryRecommended() ? "Yes" : "No").append("</span>\n");
        html.append("      <span class=\"rca-flag ").append(result.isDefectWorthy() ? "rca-flag-yes" : "rca-flag-no").append("\">");
        html.append("Defect: ").append(result.isDefectWorthy() ? "Yes" : "No").append("</span>\n");
        html.append("    </div>\n");

        // Recurrence (only show if seen before)
        if (result.isSeenBefore()) {
            html.append("    <div class=\"rca-recurrence\">\n");
            html.append("      <span class=\"rca-label\">Recurrence</span>\n");
            html.append("      <span class=\"rca-recurrence-detail\">Seen Before: Yes | Frequency: ");
            html.append(escapeHtml(result.getFailureFrequency()));
            html.append(" | Recurring: ").append(result.isRecurringPattern() ? "Yes" : "No");
            html.append("</span>\n");
            html.append("    </div>\n");
        }

        html.append("  </div>\n");
        html.append("</div>\n");

        return html.toString();
    }

    public static String getRcaCssStyles() {
        return ".rca-card { border: 1px solid #e74c3c; border-left: 4px solid #e74c3c; border-radius: 8px; margin: 15px 0; background: #fff8f8; overflow: hidden; }\n" +
                ".rca-header { display: flex; align-items: center; gap: 10px; padding: 12px 16px; background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%); color: white; }\n" +
                ".rca-icon { font-size: 18px; }\n" +
                ".rca-title { font-weight: 700; font-size: 14px; text-transform: uppercase; letter-spacing: 1px; flex: 1; }\n" +
                ".rca-confidence { font-size: 12px; padding: 3px 8px; border-radius: 12px; background: rgba(255,255,255,0.2); }\n" +
                ".rca-confidence-high { background: rgba(39,174,96,0.3); }\n" +
                ".rca-confidence-medium { background: rgba(243,156,18,0.3); }\n" +
                ".rca-confidence-low { background: rgba(255,255,255,0.2); }\n" +
                ".rca-body { padding: 16px; }\n" +
                ".rca-row { display: flex; gap: 20px; margin-bottom: 14px; flex-wrap: wrap; }\n" +
                ".rca-field { flex: 1; min-width: 120px; }\n" +
                ".rca-label { display: block; font-size: 11px; color: #888; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; font-weight: 600; }\n" +
                ".rca-value { font-size: 14px; font-weight: 600; color: #333; }\n" +
                ".rca-category { color: #e74c3c; }\n" +
                ".rca-owner { color: #2c3e50; }\n" +
                ".rca-severity-critical { color: #c0392b; font-weight: 700; }\n" +
                ".rca-severity-high { color: #e74c3c; }\n" +
                ".rca-severity-medium { color: #f39c12; }\n" +
                ".rca-severity-low { color: #27ae60; }\n" +
                ".rca-section { margin-bottom: 12px; }\n" +
                ".rca-text { font-size: 13px; color: #444; line-height: 1.5; margin-top: 4px; }\n" +
                ".rca-evidence { font-family: 'Courier New', monospace; font-size: 12px; background: #f5f5f5; padding: 8px; border-radius: 4px; color: #555; }\n" +
                ".rca-flags { display: flex; gap: 12px; margin-top: 12px; padding-top: 12px; border-top: 1px solid #f0e0e0; }\n" +
                ".rca-flag { font-size: 12px; padding: 4px 10px; border-radius: 4px; font-weight: 600; }\n" +
                ".rca-flag-yes { background: #fdecea; color: #c0392b; }\n" +
                ".rca-flag-no { background: #eafaf1; color: #27ae60; }\n" +
                ".rca-recurrence { margin-top: 10px; padding: 8px 12px; background: #fef9e7; border-radius: 4px; border-left: 3px solid #f39c12; }\n" +
                ".rca-recurrence-detail { font-size: 12px; color: #7d6608; }\n";
    }

    private static String getConfidenceClass(int score) {
        if (score >= 80) return "rca-confidence-high";
        if (score >= 50) return "rca-confidence-medium";
        return "rca-confidence-low";
    }

    private static String getSeverityClass(String severity) {
        if (severity == null) return "";
        return switch (severity.toLowerCase()) {
            case "critical" -> "rca-severity-critical";
            case "high" -> "rca-severity-high";
            case "medium" -> "rca-severity-medium";
            case "low" -> "rca-severity-low";
            default -> "";
        };
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
