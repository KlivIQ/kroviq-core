package kroviq.api.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread-safe per-scenario storage for API state.
 * Mirrors the design of TestContext — everything via ThreadLocal.
 *
 * Lifecycle: cleared in TestHooks @After to ensure scenario isolation.
 */
public final class ApiContext {

    private ApiContext() {}

    // ── Current Request / Response ────────────────────────────────────────────

    private static final ThreadLocal<ApiRequest>  currentRequest  = new ThreadLocal<>();
    private static final ThreadLocal<ApiResponse> currentResponse = new ThreadLocal<>();

    public static void setCurrentRequest(ApiRequest request) {
        currentRequest.set(request);
    }

    public static ApiRequest getCurrentRequest() {
        return currentRequest.get();
    }

    public static void setCurrentResponse(ApiResponse response) {
        currentResponse.set(response);
    }

    public static ApiResponse getCurrentResponse() {
        ApiResponse r = currentResponse.get();
        if (r == null) {
            throw new RuntimeException(
                "[ApiContext] No API response in context. " +
                "Execute an API request step before asserting on the response.");
        }
        return r;
    }

    public static boolean hasResponse() {
        return currentResponse.get() != null;
    }

    // ── Saved Values (response chaining) ──────────────────────────────────────

    private static final ThreadLocal<Map<String, String>> savedValues =
            ThreadLocal.withInitial(HashMap::new);

    public static void saveValue(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("[ApiContext] saveValue: key cannot be null or empty");
        }
        savedValues.get().put(key, value);
    }

    public static String getSavedValue(String key) {
        String value = savedValues.get().get(key);
        if (value == null) {
            throw new RuntimeException(String.format(
                "[ApiContext] No saved value for key '%s'. " +
                "Available keys: %s. " +
                "Use 'I save the JSON path ... as ...' step first.",
                key, savedValues.get().keySet()));
        }
        return value;
    }

    public static boolean hasSavedValue(String key) {
        return savedValues.get().containsKey(key);
    }

    public static Map<String, String> getAllSavedValues() {
        return Collections.unmodifiableMap(savedValues.get());
    }

    // ── Session Headers (persist across steps in same scenario) ───────────────

    private static final ThreadLocal<Map<String, String>> sessionHeaders =
            ThreadLocal.withInitial(HashMap::new);

    public static void setSessionHeader(String name, String value) {
        sessionHeaders.get().put(name, value);
    }

    public static void setBearerToken(String token) {
        setSessionHeader("Authorization", "Bearer " + token);
    }

    public static void clearSessionHeader(String name) {
        sessionHeaders.get().remove(name);
    }

    public static void clearBearerToken() {
        clearSessionHeader("Authorization");
    }

    public static Map<String, String> getSessionHeaders() {
        return Collections.unmodifiableMap(sessionHeaders.get());
    }

    // ── Base URL override (per-scenario) ──────────────────────────────────────

    private static final ThreadLocal<String> baseUrlOverride = new ThreadLocal<>();

    public static void setBaseUrl(String url) {
        baseUrlOverride.set(url);
    }

    public static String getBaseUrl() {
        return baseUrlOverride.get();
    }

    // ── Full cleanup (called in TestHooks @After) ──────────────────────────────

    public static void clearAll() {
        currentRequest.remove();
        currentResponse.remove();
        savedValues.get().clear();
        savedValues.remove();
        sessionHeaders.get().clear();
        sessionHeaders.remove();
        baseUrlOverride.remove();
    }
}
