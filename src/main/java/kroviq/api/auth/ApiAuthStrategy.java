package kroviq.api.auth;

import io.restassured.builder.RequestSpecBuilder;

/**
 * Contract for all authentication strategies applied to API requests.
 *
 * Built-in implementations:
 *   BearerTokenAuth  — Authorization: Bearer {token}
 *   BasicAuth        — Authorization: Basic {base64(user:pass)}
 *   ApiKeyAuth       — Custom header or query param
 */
@FunctionalInterface
public interface ApiAuthStrategy {
    void apply(RequestSpecBuilder spec);
}
