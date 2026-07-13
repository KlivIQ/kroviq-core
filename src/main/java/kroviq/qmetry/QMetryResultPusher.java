package kroviq.qmetry;

import kroviq.reporting.TestCaseResult;
import kroviq.reporting.TestRunSummary;
import kroviq.reporting.StepResult;
import kroviq.reporting.managers.TestRunReportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QMetryResultPusher {

    private static final Logger logger = LogManager.getLogger(QMetryResultPusher.class);

    private static final String IMPORT_ENDPOINT = "/rest/qtm4j/automation/latest/importresult";
    private static final String TRACK_ENDPOINT = "/rest/qtm4j/automation/latest/importresult/track";
    private static final int POLL_INTERVAL_MS = 5000;
    private static final int MAX_POLL_ATTEMPTS = 24;

    public static void pushIfEnabled() {
        QMetryConfig config = QMetryConfig.load();

        if (!config.isEnabled()) {
            logger.info("QMetry push skipped (disabled)");
            System.out.println("[QMetry] Push skipped (disabled)");
            return;
        }

        if (!config.isValid()) {
            logger.error("QMetry push failed: config incomplete (baseUrl or apiKey missing)");
            System.err.println("[QMetry] Push failed: config incomplete (baseUrl or apiKey missing)");
            return;
        }

        logger.info("QMetry push started");
        System.out.println("[QMetry] Push started");
        try {
            TestRunSummary summary = TestRunReportManager.getInstance().getRunSummary();
            String runDir = TestRunReportManager.getInstance().getRunDirectory();

            File jsonFile = findOrGenerateCucumberJson(summary, runDir);
            if (jsonFile == null) {
                logger.error("QMetry push failed: could not prepare Cucumber JSON");
                System.err.println("[QMetry] Push failed: could not prepare Cucumber JSON");
                return;
            }

            QMetryApiClient api = new QMetryApiClient(config);
            uploadThreeStep(api, config.getBaseUrl(), jsonFile);
        } catch (Exception e) {
            logger.error("QMetry push failed: {}", e.getMessage(), e);
            System.err.println("[QMetry] Push failed: " + e.getMessage());
        }
    }

    // --- 3-step upload ---

    private static boolean uploadThreeStep(QMetryApiClient api, String baseUrl, File jsonFile) {
        try {
            // Step 1: Initiate
            String initUrl = baseUrl + IMPORT_ENDPOINT;
            String initBody = "{\"format\":\"cucumber\",\"attachFile\":true,\"isZip\":false}";

            logger.info("Initiating QMetry upload...");
            System.out.println("[QMetry] Initiating QMetry upload...");
            String initResponse = api.postJson(initUrl, initBody);

            String uploadUrl = extractField(initResponse, "url");
            String trackingId = extractField(initResponse, "trackingId");

            if (uploadUrl == null || trackingId == null) {
                logger.error("QMetry push failed: missing uploadUrl or trackingId in response");
                System.err.println("[QMetry] Push failed: missing uploadUrl or trackingId in response");
                return false;
            }

            // Step 2: Upload file
            logger.info("Uploading result file to QMetry...");
            System.out.println("[QMetry] Uploading result file to QMetry...");
            api.postFile(uploadUrl, jsonFile);

            // Step 3: Poll status
            logger.info("Polling QMetry status for trackingId: {}", trackingId);
            System.out.println("[QMetry] Polling QMetry status for trackingId: " + trackingId);
            boolean success = pollStatus(api, baseUrl + TRACK_ENDPOINT + "?trackingId=" + trackingId);

            if (success) {
                logger.info("QMetry push completed successfully. TrackingId: {}", trackingId);
                System.out.println("[QMetry] Push completed successfully. TrackingId: " + trackingId);
            } else {
                logger.error("QMetry push failed: import did not succeed. TrackingId: {}", trackingId);
                System.err.println("[QMetry] Push failed: import did not succeed. TrackingId: " + trackingId);
            }
            return success;

        } catch (IOException e) {
            logger.error("QMetry push failed: {}", e.getMessage(), e);
            System.err.println("[QMetry] Push failed: " + e.getMessage());
            return false;
        }
    }

    private static boolean pollStatus(QMetryApiClient api, String trackUrl) {
        for (int i = 1; i <= MAX_POLL_ATTEMPTS; i++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
                String response = api.get(trackUrl);

                String processStatus = extractField(response, "processStatus");
                String importStatus = extractField(response, "importStatus");

                if ("SUCCESS".equalsIgnoreCase(processStatus) && "SUCCESS".equalsIgnoreCase(importStatus)) {
                    return true;
                }
                if ("FAILED".equalsIgnoreCase(processStatus) || "FAILED".equalsIgnoreCase(importStatus)
                    || "ERROR".equalsIgnoreCase(processStatus) || "ERROR".equalsIgnoreCase(importStatus)) {
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (IOException e) {
                logger.error("QMetry poll error: {}", e.getMessage());
                System.err.println("[QMetry] Poll error: " + e.getMessage());
            }
        }

        logger.error("QMetry polling timed out. Check QMetry manually.");
        System.err.println("[QMetry] Polling timed out. Check QMetry manually.");
        return false;
    }

    // --- Cucumber JSON ---

    private static File findOrGenerateCucumberJson(TestRunSummary summary, String runDir) {
        File dir = new File(runDir);
        if (dir.isDirectory()) {
            File[] jsonFiles = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (jsonFiles != null) {
                for (File f : jsonFiles) {
                    if (f.length() > 0) {
                        logger.info("Using existing JSON: {}", f.getName());
                        System.out.println("[QMetry] Using existing JSON: " + f.getName());
                        return f;
                    }
                }
            }
        }

        logger.info("Generating Cucumber JSON from test results...");
        System.out.println("[QMetry] Generating Cucumber JSON from test results...");
        return generateCucumberJson(summary, runDir);
    }

    private static File generateCucumberJson(TestRunSummary summary, String runDir) {
        List<TestCaseResult> testCases = summary.getTestCases();
        if (testCases == null || testCases.isEmpty()) {
            return null;
        }

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < testCases.size(); i++) {
            TestCaseResult tc = testCases.get(i);
            if (i > 0) json.append(",");

            String featureUri = "features/" + tc.getTestCaseId() + ".feature";
            String tcId = tc.getTestCaseId();
            String scenarioName = escapeJson(tc.getScenarioName());
            long durationNanos = tc.getExecutionTimeSeconds() * 1_000_000_000L;

            json.append("{");
            json.append("\"uri\":\"").append(featureUri).append("\",");
            json.append("\"keyword\":\"Feature\",");
            json.append("\"name\":\"").append(scenarioName).append("\",");
            json.append("\"elements\":[{");
            json.append("\"keyword\":\"Scenario\",");
            json.append("\"name\":\"").append(scenarioName).append("\",");
            json.append("\"tags\":[{\"name\":\"@").append(tcId).append("\"}],");
            json.append("\"steps\":[");

            List<StepResult> steps = tc.getSteps();
            if (steps != null && !steps.isEmpty()) {
                for (int s = 0; s < steps.size(); s++) {
                    StepResult step = steps.get(s);
                    if (s > 0) json.append(",");
                    json.append("{");
                    json.append("\"keyword\":\"Given \",");
                    json.append("\"name\":\"").append(escapeJson(step.getStepDescription())).append("\",");
                    json.append("\"result\":{");
                    json.append("\"status\":\"").append(mapStatus(step.getStatus())).append("\",");
                    json.append("\"duration\":").append(durationNanos / Math.max(steps.size(), 1));
                    if (step.getErrorMessage() != null) {
                        json.append(",\"error_message\":\"").append(escapeJson(step.getErrorMessage())).append("\"");
                    }
                    json.append("}}");
                }
            } else {
                // Single synthetic step
                json.append("{");
                json.append("\"keyword\":\"Given \",");
                json.append("\"name\":\"Test execution\",");
                json.append("\"result\":{");
                json.append("\"status\":\"").append(mapStatus(tc.getStatus())).append("\",");
                json.append("\"duration\":").append(durationNanos);
                json.append("}}");
            }

            json.append("]}]}");
        }

        json.append("]");

        try {
            File outFile = new File(runDir, "qmetry_cucumber.json");
            try (FileWriter writer = new FileWriter(outFile)) {
                writer.write(json.toString());
            }
            return outFile;
        } catch (IOException e) {
            logger.error("QMetry push failed: could not write JSON -- {}", e.getMessage(), e);
            System.err.println("[QMetry] Push failed: could not write JSON -- " + e.getMessage());
            return null;
        }
    }

    // --- Utilities ---

    private static String mapStatus(String nafStatus) {
        if (nafStatus == null) return "undefined";
        switch (nafStatus.toUpperCase()) {
            case "PASSED": return "passed";
            case "FAILED": return "failed";
            case "SKIPPED": return "skipped";
            default: return "undefined";
        }
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private static String extractField(String json, String fieldName) {
        if (json == null) return null;
        Pattern p = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
