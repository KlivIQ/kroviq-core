package kroviq.qmetry;

import com.fasterxml.jackson.databind.JsonNode;
import kroviq.utils.RunManager;

public class QMetryConfig {

    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final String username;
    private final String password;
    private final String projectId;

    private QMetryConfig(JsonNode node) {
        if (node == null) {
            this.enabled = false;
            this.baseUrl = "";
            this.apiKey = "";
            this.username = "";
            this.password = "";
            this.projectId = "";
            return;
        }
        this.enabled = node.path("enabled").asBoolean(false);
        this.baseUrl = node.path("baseUrl").asText("").trim();
        this.apiKey = node.path("apiKey").asText("").trim();
        this.username = node.path("username").asText("").trim();
        this.password = node.path("password").asText("").trim();
        this.projectId = node.path("projectId").asText("").trim();
    }

    public static QMetryConfig load() {
        return new QMetryConfig(RunManager.getQMetryConfig());
    }

    public boolean isEnabled() { return enabled; }
    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getProjectId() { return projectId; }

    public boolean isValid() {
        return enabled
            && !baseUrl.isEmpty()
            && !apiKey.isEmpty();
    }
}
