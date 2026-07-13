package kroviq.api.core;

import kroviq.api.auth.ApiAuthStrategy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent, immutable-style request builder.
 * Build once, execute via ApiClient.execute(request).
 *
 * Usage:
 *   ApiRequest.post("/users")
 *       .body(jsonBody)
 *       .header("X-Correlation-Id", uuid)
 *       .auth(new BearerTokenAuth(token))
 */
public class ApiRequest {

    private String method;
    private String endpoint;
    private final Map<String, String> headers      = new LinkedHashMap<>();
    private final Map<String, String> queryParams  = new LinkedHashMap<>();
    private final Map<String, String> pathParams   = new LinkedHashMap<>();
    private final Map<String, String> formParams   = new LinkedHashMap<>();
    private String body;
    private String contentType    = "application/json";
    private ApiAuthStrategy auth;
    private boolean logRequest    = true;
    private boolean logResponse   = true;
    private int timeoutSeconds    = 30;
    private String baseUrlOverride;

    private ApiRequest() {}

    // ── Static factories ──────────────────────────────────────────────────────

    public static ApiRequest get(String endpoint)    { return of("GET",    endpoint); }
    public static ApiRequest post(String endpoint)   { return of("POST",   endpoint); }
    public static ApiRequest put(String endpoint)    { return of("PUT",    endpoint); }
    public static ApiRequest patch(String endpoint)  { return of("PATCH",  endpoint); }
    public static ApiRequest delete(String endpoint) { return of("DELETE", endpoint); }
    public static ApiRequest head(String endpoint)   { return of("HEAD",   endpoint); }

    private static ApiRequest of(String method, String endpoint) {
        ApiRequest r = new ApiRequest();
        r.method   = method;
        r.endpoint = endpoint;
        return r;
    }

    // ── Builder methods ───────────────────────────────────────────────────────

    public ApiRequest header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public ApiRequest headers(Map<String, String> h) {
        headers.putAll(h);
        return this;
    }

    public ApiRequest queryParam(String name, String value) {
        queryParams.put(name, value);
        return this;
    }

    public ApiRequest pathParam(String name, String value) {
        pathParams.put(name, value);
        return this;
    }

    public ApiRequest formParam(String name, String value) {
        formParams.put(name, value);
        contentType = "application/x-www-form-urlencoded";
        return this;
    }

    public ApiRequest body(String body) {
        this.body = body;
        return this;
    }

    public ApiRequest contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public ApiRequest auth(ApiAuthStrategy strategy) {
        this.auth = strategy;
        return this;
    }

    public ApiRequest baseUrl(String baseUrl) {
        this.baseUrlOverride = baseUrl;
        return this;
    }

    public ApiRequest timeout(int seconds) {
        this.timeoutSeconds = seconds;
        return this;
    }

    public ApiRequest noLogging() {
        logRequest  = false;
        logResponse = false;
        return this;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getMethod()                   { return method; }
    public String getEndpoint()                 { return endpoint; }
    public Map<String, String> getHeaders()     { return Collections.unmodifiableMap(headers); }
    public Map<String, String> getQueryParams() { return Collections.unmodifiableMap(queryParams); }
    public Map<String, String> getPathParams()  { return Collections.unmodifiableMap(pathParams); }
    public Map<String, String> getFormParams()  { return Collections.unmodifiableMap(formParams); }
    public String getBody()                     { return body; }
    public String getContentType()              { return contentType; }
    public ApiAuthStrategy getAuth()            { return auth; }
    public boolean isLogRequest()               { return logRequest; }
    public boolean isLogResponse()              { return logResponse; }
    public int getTimeoutSeconds()              { return timeoutSeconds; }
    public String getBaseUrlOverride()          { return baseUrlOverride; }

    @Override
    public String toString() {
        return String.format("ApiRequest[%s %s]", method, endpoint);
    }
}
