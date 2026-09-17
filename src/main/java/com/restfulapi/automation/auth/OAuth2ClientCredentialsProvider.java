package com.restfulapi.automation.auth;

import com.restfulapi.automation.exceptions.AuthenticationException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/** OAuth2 grant "client_credentials" (RFC 6749 §4.4) con caché y renovación del access_token. */
public final class OAuth2ClientCredentialsProvider extends CachedTokenProvider {

    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String scope;

    public OAuth2ClientCredentialsProvider(String tokenUrl, String clientId, String clientSecret, String scope) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
    }

    @Override
    protected TokenResult fetchToken() {
        RequestSpecification spec = RestAssured.given()
                .contentType(ContentType.URLENC)
                .auth().preemptive().basic(clientId, clientSecret)
                .formParam("grant_type", "client_credentials");
        if (scope != null && !scope.isBlank()) {
            spec.formParam("scope", scope);
        }
        Response response = spec.post(tokenUrl);
        if (response.statusCode() != 200) {
            throw new AuthenticationException("OAuth2 token endpoint respondió " + response.statusCode());
        }
        Number expiresIn = response.jsonPath().get("expires_in");
        return new TokenResult(response.jsonPath().getString("access_token"),
                expiresIn == null ? 300 : expiresIn.longValue());
    }
}
