package kroviq.api.auth;

import io.restassured.builder.RequestSpecBuilder;

public class ApiKeyAuth implements ApiAuthStrategy {

    public enum Location { HEADER, QUERY_PARAM }

    private final String   keyName;
    private final String   keyValue;
    private final Location location;

    public ApiKeyAuth(String keyName, String keyValue) {
        this(keyName, keyValue, Location.HEADER);
    }

    public ApiKeyAuth(String keyName, String keyValue, Location location) {
        this.keyName  = keyName;
        this.keyValue = keyValue;
        this.location = location;
    }

    @Override
    public void apply(RequestSpecBuilder spec) {
        if (location == Location.HEADER) {
            spec.addHeader(keyName, keyValue);
        } else {
            spec.addQueryParam(keyName, keyValue);
        }
    }
}
