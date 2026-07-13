package kroviq.api.core;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import kroviq.api.auth.ApiAuthStrategy;
import kroviq.api.reporting.ApiStepLogger;
import kroviq.utils.RunManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Core API executor. Wraps RestAssured — consumers never import RestAssured directly.
 *
 * URL resolution order:
 *   1. request.baseUrl()         — per-call override
 *   2. ApiContext.getBaseUrl()   — per-scenario override
 *   3. RunManager.getApiBaseUrl()— RunManager.json "apiBaseUrl"
 *   4. RunManager.getEnvironmentURL() — fallback to app URL
 */
public final class ApiClient {

    private static final Logger logger = LogManager.getLogger(ApiClient.class);

    static {
        RestAssured.useRelaxedHTTPSValidation();
    }

    private ApiClient() {}

    public static ApiResponse execute(ApiRequest request) {
        String baseUrl = resolveBaseUrl(request);

        RequestSpecBuilder specBuilder = new RequestSpecBuilder();
        specBuilder.setBaseUri(baseUrl);
        specBuilder.setContentType(request.getContentType());

        // Session-level headers (lower priority)
        Map<String, String> sessionHeaders = ApiContext.getSessionHeaders();
        if (!sessionHeaders.isEmpty()) {
            specBuilder.addHeaders(sessionHeaders);
        }

        // Request-level headers (override session on collision)
        if (!request.getHeaders().isEmpty()) {
            specBuilder.addHeaders(request.getHeaders());
        }

        if (!request.getQueryParams().isEmpty()) {
            specBuilder.addQueryParams(request.getQueryParams());
        }

        if (!request.getPathParams().isEmpty()) {
            specBuilder.addPathParams(request.getPathParams());
        }

        if (!request.getFormParams().isEmpty()) {
            specBuilder.addFormParams(request.getFormParams());
        }

        if (request.getBody() != null && !request.getBody().isEmpty()
                && request.getFormParams().isEmpty()) {
            specBuilder.setBody(request.getBody());
        }

        ApiAuthStrategy auth = request.getAuth();
        if (auth != null) {
            auth.apply(specBuilder);
        }

        int timeoutMs = request.getTimeoutSeconds() * 1000;
        RestAssuredConfig config = RestAssured.config()
                .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", timeoutMs)
                        .setParam("http.socket.timeout", timeoutMs));
        specBuilder.setConfig(config);

        if (request.isLogRequest()) {
            logRequest(request, baseUrl, sessionHeaders);
        }

        RequestSpecification spec = specBuilder.build();
        long startTime = System.currentTimeMillis();
        Response rawResponse;

        try {
            rawResponse = dispatch(request, spec);
        } catch (Exception e) {
            String msg = String.format("[ApiClient] Request failed: %s %s — %s",
                    request.getMethod(), request.getEndpoint(), e.getMessage());
            logger.error(msg);
            throw new RuntimeException(msg, e);
        }

        long durationMs = System.currentTimeMillis() - startTime;
        ApiResponse apiResponse = new ApiResponse(rawResponse, request, durationMs);

        if (request.isLogResponse()) {
            logResponse(apiResponse);
        }

        ApiContext.setCurrentRequest(request);
        ApiContext.setCurrentResponse(apiResponse);
        ApiStepLogger.log(request, apiResponse);

        return apiResponse;
    }

    private static Response dispatch(ApiRequest request, RequestSpecification spec) {
        String endpoint = request.getEndpoint();
        return switch (request.getMethod().toUpperCase()) {
            case "GET"     -> RestAssured.given().spec(spec).get(endpoint);
            case "POST"    -> RestAssured.given().spec(spec).post(endpoint);
            case "PUT"     -> RestAssured.given().spec(spec).put(endpoint);
            case "PATCH"   -> RestAssured.given().spec(spec).patch(endpoint);
            case "DELETE"  -> RestAssured.given().spec(spec).delete(endpoint);
            case "HEAD"    -> RestAssured.given().spec(spec).head(endpoint);
            case "OPTIONS" -> RestAssured.given().spec(spec).options(endpoint);
            default -> throw new IllegalArgumentException(
                    "Unsupported HTTP method: " + request.getMethod());
        };
    }

    private static String resolveBaseUrl(ApiRequest request) {
        if (request.getBaseUrlOverride() != null && !request.getBaseUrlOverride().isBlank()) {
            return request.getBaseUrlOverride();
        }
        String ctxUrl = ApiContext.getBaseUrl();
        if (ctxUrl != null && !ctxUrl.isBlank()) {
            return ctxUrl;
        }
        try {
            String apiUrl = RunManager.getApiBaseUrl();
            if (apiUrl != null && !apiUrl.isBlank()) {
                return apiUrl;
            }
        } catch (Exception e) {
            logger.debug("[ApiClient] apiBaseUrl not in RunManager, falling back to environmentURL");
        }
        return RunManager.getEnvironmentURL();
    }

    private static void logRequest(ApiRequest request, String baseUrl,
                                   Map<String, String> sessionHeaders) {
        logger.info("[API -->] {} {}{}", request.getMethod(), baseUrl, request.getEndpoint());
        if (!sessionHeaders.isEmpty()) {
            logger.debug("[API] Session headers: {}", sessionHeaders.keySet());
        }
        if (!request.getHeaders().isEmpty()) {
            logger.debug("[API] Request headers: {}", request.getHeaders().keySet());
        }
        if (!request.getQueryParams().isEmpty()) {
            logger.debug("[API] Query params: {}", request.getQueryParams());
        }
        if (request.getBody() != null && !request.getBody().isBlank()) {
            logger.debug("[API] Body: {}", request.getBody());
        }
    }

    private static void logResponse(ApiResponse response) {
        String icon = response.isSuccessful() ? "✓" : "✗";
        logger.info("[API <--] {} {} ({}ms)",
                icon, response.getStatusCode(), response.getDurationMs());
        if (!response.isSuccessful()) {
            logger.warn("[API] Non-2xx response body: {}",
                    response.getBody().substring(0, Math.min(response.getBody().length(), 500)));
        } else {
            logger.debug("[API] Response body: {}", response.getBody());
        }
    }
}
