package kroviq.api.core;

import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Wraps a RestAssured Response with clean accessors and fluent assertions.
 *
 * Usage:
 *   ApiResponse response = ApiClient.execute(request);
 *   response.assertThat().statusCode(200).jsonPathNotNull("data.id");
 *   String id = response.jsonPathAsString("data.id");
 */
public class ApiResponse {

    private static final Logger logger = LogManager.getLogger(ApiResponse.class);

    private final Response      rawResponse;
    private final ApiRequest    sourceRequest;
    private final long          durationMs;

    public ApiResponse(Response rawResponse, ApiRequest sourceRequest, long durationMs) {
        this.rawResponse   = rawResponse;
        this.sourceRequest = sourceRequest;
        this.durationMs    = durationMs;
    }

    // ── Status ────────────────────────────────────────────────────────────────

    public int    getStatusCode()  { return rawResponse.getStatusCode(); }
    public String getStatusLine()  { return rawResponse.getStatusLine(); }
    public boolean isSuccessful()  { int c = getStatusCode(); return c >= 200 && c < 300; }
    public boolean isClientError() { int c = getStatusCode(); return c >= 400 && c < 500; }
    public boolean isServerError() { int c = getStatusCode(); return c >= 500; }

    // ── Headers ───────────────────────────────────────────────────────────────

    public String getHeader(String name) { return rawResponse.getHeader(name); }

    public Map<String, String> getHeaders() {
        Map<String, String> map = new LinkedHashMap<>();
        rawResponse.getHeaders().forEach(h -> map.put(h.getName(), h.getValue()));
        return Collections.unmodifiableMap(map);
    }

    // ── Body ──────────────────────────────────────────────────────────────────

    public String getBody()          { return rawResponse.getBody().asString(); }
    public byte[] getBodyAsBytes()   { return rawResponse.getBody().asByteArray(); }
    public String getContentType()   { return rawResponse.getContentType(); }
    public boolean isJson()          { String ct = getContentType(); return ct != null && ct.contains("application/json"); }
    public boolean isXml()           { String ct = getContentType(); return ct != null && (ct.contains("application/xml") || ct.contains("text/xml")); }

    // ── JSON Path ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public <T> T jsonPath(String path) {
        try {
            return (T) rawResponse.jsonPath().get(path);
        } catch (Exception e) {
            logger.debug("[ApiResponse] jsonPath('{}') returned error: {}", path, e.getMessage());
            return null;
        }
    }

    public String jsonPathAsString(String path) {
        Object val = jsonPath(path);
        return val != null ? String.valueOf(val) : null;
    }

    public int jsonPathAsInt(String path) {
        return rawResponse.jsonPath().getInt(path);
    }

    public boolean jsonPathAsBoolean(String path) {
        return rawResponse.jsonPath().getBoolean(path);
    }

    public List<Object> jsonPathAsList(String path) {
        return rawResponse.jsonPath().getList(path);
    }

    public Map<String, Object> jsonPathAsMap(String path) {
        return rawResponse.jsonPath().getMap(path);
    }

    // ── XML Path ──────────────────────────────────────────────────────────────

    public String xmlPath(String path) {
        return rawResponse.xmlPath().getString(path);
    }

    // ── Performance ───────────────────────────────────────────────────────────

    public long getDurationMs() { return durationMs; }
    public long getResponseTimeMs() { return rawResponse.getTimeIn(TimeUnit.MILLISECONDS); }

    // ── Source ────────────────────────────────────────────────────────────────

    public ApiRequest getSourceRequest() { return sourceRequest; }
    public Response raw() { return rawResponse; }

    // ── Assertions ────────────────────────────────────────────────────────────

    public ResponseAssert assertThat() {
        return new ResponseAssert(this);
    }

    @Override
    public String toString() {
        return String.format("ApiResponse[status=%d, duration=%dms, type=%s, bodyLen=%d]",
                getStatusCode(), durationMs, getContentType(), getBody().length());
    }
}
