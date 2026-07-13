package kroviq.api.core;

import kroviq.api.utils.SchemaValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Fluent assertion API for ApiResponse.
 * All assertions throw AssertionError on failure — compatible with Cucumber and RCA.
 */
public class ResponseAssert {

    private static final Logger logger = LogManager.getLogger(ResponseAssert.class);
    private final ApiResponse response;

    ResponseAssert(ApiResponse response) {
        this.response = response;
    }

    // ── Status ────────────────────────────────────────────────────────────────

    public ResponseAssert statusCode(int expected) {
        int actual = response.getStatusCode();
        if (actual != expected) {
            fail("Status code mismatch. Expected: %d, Actual: %d%nBody: %s",
                    expected, actual, truncate(response.getBody(), 800));
        }
        pass("Status code %d ✓", expected);
        return this;
    }

    public ResponseAssert statusCodeBetween(int min, int max) {
        int actual = response.getStatusCode();
        if (actual < min || actual > max) {
            fail("Status %d not in range [%d, %d]%nBody: %s",
                    actual, min, max, truncate(response.getBody(), 800));
        }
        pass("Status %d in [%d, %d] ✓", actual, min, max);
        return this;
    }

    public ResponseAssert isSuccessful() { return statusCodeBetween(200, 299); }
    public ResponseAssert isCreated() { return statusCode(201); }
    public ResponseAssert isNoContent() { return statusCode(204); }
    public ResponseAssert isBadRequest() { return statusCode(400); }
    public ResponseAssert isUnauthorized() { return statusCode(401); }
    public ResponseAssert isForbidden() { return statusCode(403); }
    public ResponseAssert isNotFound() { return statusCode(404); }

    // ── Body ──────────────────────────────────────────────────────────────────

    public ResponseAssert bodyContains(String text) {
        if (!response.getBody().contains(text)) {
            fail("Body does not contain: '%s'%nActual: %s", text, truncate(response.getBody(), 800));
        }
        pass("Body contains '%s' ✓", text);
        return this;
    }

    public ResponseAssert bodyNotContains(String text) {
        if (response.getBody().contains(text)) {
            fail("Body unexpectedly contains: '%s'", text);
        }
        pass("Body does not contain '%s' ✓", text);
        return this;
    }

    public ResponseAssert bodyIsNotEmpty() {
        String body = response.getBody();
        if (body == null || body.trim().isEmpty()) {
            fail("Response body is empty or null");
        }
        pass("Body is not empty ✓");
        return this;
    }

    // ── JSON Path ─────────────────────────────────────────────────────────────

    public ResponseAssert jsonPathEquals(String path, String expected) {
        String actual = response.jsonPathAsString(path);
        if (!expected.equals(actual)) {
            fail("JSON path '%s' mismatch. Expected: '%s', Actual: '%s'", path, expected, actual);
        }
        pass("$.%s == '%s' ✓", path, expected);
        return this;
    }

    public ResponseAssert jsonPathNotNull(String path) {
        Object value = response.jsonPath(path);
        if (value == null) {
            fail("JSON path '%s' is null.%nBody: %s", path, truncate(response.getBody(), 800));
        }
        pass("$.%s is not null ✓", path);
        return this;
    }

    public ResponseAssert jsonPathIsNull(String path) {
        Object value = response.jsonPath(path);
        if (value != null) {
            fail("JSON path '%s' expected null but was: '%s'", path, value);
        }
        pass("$.%s is null ✓", path);
        return this;
    }

    public ResponseAssert jsonPathContains(String path, String substring) {
        String actual = response.jsonPathAsString(path);
        if (actual == null || !actual.contains(substring)) {
            fail("JSON path '%s' value '%s' does not contain '%s'", path, actual, substring);
        }
        pass("$.%s contains '%s' ✓", path, substring);
        return this;
    }

    public ResponseAssert jsonPathEqualsIgnoreCase(String path, String expected) {
        String actual = response.jsonPathAsString(path);
        if (!expected.equalsIgnoreCase(actual)) {
            fail("JSON path '%s' case-insensitive mismatch. Expected: '%s', Actual: '%s'",
                    path, expected, actual);
        }
        pass("$.%s equalsIgnoreCase '%s' ✓", path, expected);
        return this;
    }

    public ResponseAssert jsonPathIsTrue(String path) {
        Object val = response.jsonPath(path);
        if (!(val instanceof Boolean) || !((Boolean) val)) {
            fail("JSON path '%s' expected true but was: %s", path, val);
        }
        pass("$.%s is true ✓", path);
        return this;
    }

    public ResponseAssert jsonPathIsFalse(String path) {
        Object val = response.jsonPath(path);
        if (!(val instanceof Boolean) || ((Boolean) val)) {
            fail("JSON path '%s' expected false but was: %s", path, val);
        }
        pass("$.%s is false ✓", path);
        return this;
    }

    // ── Arrays ────────────────────────────────────────────────────────────────

    public ResponseAssert jsonArrayNotEmpty(String path) {
        List<Object> list = response.jsonPathAsList(path);
        if (list == null || list.isEmpty()) {
            fail("JSON array at '%s' is null or empty.%nBody: %s",
                    path, truncate(response.getBody(), 800));
        }
        pass("$.%s array has %d item(s) ✓", path, list.size());
        return this;
    }

    public ResponseAssert jsonArrayIsEmpty(String path) {
        List<Object> list = response.jsonPathAsList(path);
        if (list != null && !list.isEmpty()) {
            fail("JSON array at '%s' expected empty but has %d item(s)", path, list.size());
        }
        pass("$.%s array is empty ✓", path);
        return this;
    }

    public ResponseAssert jsonArraySize(String path, int expectedSize) {
        List<Object> list = response.jsonPathAsList(path);
        int actual = list == null ? 0 : list.size();
        if (actual != expectedSize) {
            fail("JSON array at '%s' size mismatch. Expected: %d, Actual: %d",
                    path, expectedSize, actual);
        }
        pass("$.%s array size == %d ✓", path, expectedSize);
        return this;
    }

    public ResponseAssert jsonArraySizeAtLeast(String path, int min) {
        List<Object> list = response.jsonPathAsList(path);
        int actual = list == null ? 0 : list.size();
        if (actual < min) {
            fail("JSON array at '%s' has %d items, expected at least %d", path, actual, min);
        }
        pass("$.%s array size %d >= %d ✓", path, actual, min);
        return this;
    }

    // ── Headers ───────────────────────────────────────────────────────────────

    public ResponseAssert headerExists(String name) {
        if (response.getHeader(name) == null) {
            fail("Expected header '%s' not present in response", name);
        }
        pass("Header '%s' exists ✓", name);
        return this;
    }

    public ResponseAssert headerEquals(String name, String expected) {
        String actual = response.getHeader(name);
        if (!expected.equalsIgnoreCase(actual)) {
            fail("Header '%s' mismatch. Expected: '%s', Actual: '%s'", name, expected, actual);
        }
        pass("Header '%s' == '%s' ✓", name, expected);
        return this;
    }

    public ResponseAssert headerContains(String name, String substring) {
        String actual = response.getHeader(name);
        if (actual == null || !actual.contains(substring)) {
            fail("Header '%s' value '%s' does not contain '%s'", name, actual, substring);
        }
        pass("Header '%s' contains '%s' ✓", name, substring);
        return this;
    }

    public ResponseAssert contentTypeIsJson() {
        return contentTypeContains("application/json");
    }

    public ResponseAssert contentTypeContains(String expected) {
        String actual = response.getContentType();
        if (actual == null || !actual.contains(expected)) {
            fail("Content-Type '%s' does not contain '%s'", actual, expected);
        }
        pass("Content-Type contains '%s' ✓", expected);
        return this;
    }

    // ── Performance ───────────────────────────────────────────────────────────

    public ResponseAssert responseTimeBelow(long maxMs) {
        long actual = response.getDurationMs();
        if (actual > maxMs) {
            fail("Response time %dms exceeds limit %dms", actual, maxMs);
        }
        pass("Response time %dms < %dms ✓", actual, maxMs);
        return this;
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    public ResponseAssert matchesJsonSchema(String schemaFileName) {
        SchemaValidator.validate(response.getBody(), schemaFileName);
        pass("JSON schema '%s' validated ✓", schemaFileName);
        return this;
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void pass(String msg, Object... args) {
        logger.info("[API Assert] " + String.format(msg, args));
    }

    private void fail(String msg, Object... args) {
        String formatted = String.format(msg.replace("%n", System.lineSeparator()), args);
        String full = "[API Assert FAILED] " + formatted;
        logger.error(full);
        throw new AssertionError(full);
    }

    private String truncate(String text, int max) {
        if (text == null) return "(null)";
        return text.length() > max ? text.substring(0, max) + "...[truncated]" : text;
    }
}
