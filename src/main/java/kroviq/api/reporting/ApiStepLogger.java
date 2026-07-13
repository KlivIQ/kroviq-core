package kroviq.api.reporting;

import kroviq.api.core.ApiRequest;
import kroviq.api.core.ApiResponse;
import kroviq.reporting.StepReportingWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Bridges ApiClient executions into the existing Extent Report infrastructure.
 * Called automatically by ApiClient after every request.
 */
public final class ApiStepLogger {

    private static final Logger logger    = LogManager.getLogger(ApiStepLogger.class);
    private static final int    BODY_TRIM = 800;

    private ApiStepLogger() {}

    public static void log(ApiRequest request, ApiResponse response) {
        try {
            String result   = response.isSuccessful() ? "PASS" : "INFO";
            String message  = buildMessage(request, response);
            StepReportingWrapper.recordManualStep(message, result);
        } catch (Exception e) {
            logger.debug("[ApiStepLogger] Failed to write to report: {}", e.getMessage());
        }
    }

    private static String buildMessage(ApiRequest request, ApiResponse response) {
        StringBuilder sb = new StringBuilder();

        String icon = response.isSuccessful() ? "✓" : "✗";
        sb.append("<b>[API ")
          .append(icon)
          .append("]</b> ")
          .append(request.getMethod())
          .append(" <code>")
          .append(request.getEndpoint())
          .append("</code> → <b>")
          .append(response.getStatusCode())
          .append("</b>  (")
          .append(response.getDurationMs())
          .append(" ms)");

        if (!response.isSuccessful()) {
            if (request.getBody() != null && !request.getBody().isBlank()) {
                sb.append("<br><b>Request body:</b> <pre>")
                  .append(escapeHtml(truncate(request.getBody(), BODY_TRIM)))
                  .append("</pre>");
            }
            sb.append("<br><b>Response body:</b> <pre>")
              .append(escapeHtml(truncate(response.getBody(), BODY_TRIM)))
              .append("</pre>");
        }

        return sb.toString();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "(null)";
        return text.length() > max ? text.substring(0, max) + "...[truncated]" : text;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
