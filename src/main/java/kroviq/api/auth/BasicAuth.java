package kroviq.api.auth;

import io.restassured.builder.RequestSpecBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicAuth implements ApiAuthStrategy {

    private final String username;
    private final String password;

    public BasicAuth(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void apply(RequestSpecBuilder spec) {
        String encoded = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        spec.addHeader("Authorization", "Basic " + encoded);
    }
}
