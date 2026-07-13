package kroviq.api.auth;

import io.restassured.builder.RequestSpecBuilder;

public class BearerTokenAuth implements ApiAuthStrategy {

    private final String token;

    public BearerTokenAuth(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("BearerTokenAuth: token cannot be null or blank");
        }
        this.token = token;
    }

    @Override
    public void apply(RequestSpecBuilder spec) {
        spec.addHeader("Authorization", "Bearer " + token);
    }
}
