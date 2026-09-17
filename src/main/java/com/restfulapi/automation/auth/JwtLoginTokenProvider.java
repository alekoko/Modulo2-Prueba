package com.restfulapi.automation.auth;

import com.restfulapi.automation.exceptions.AuthenticationException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

public final class JwtLoginTokenProvider extends CachedTokenProvider {

    private final String loginUrl;
    private final String email;
    private final String password;
    private final AuthStrategy apiKeyAuth;

    public JwtLoginTokenProvider(String loginUrl, String email, String password, AuthStrategy apiKeyAuth) {
        this.loginUrl = loginUrl;
        this.email = email;
        this.password = password;
        this.apiKeyAuth = apiKeyAuth;
    }

    @Override
    protected TokenResult fetchToken() {
        RequestSpecification spec = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password));
        apiKeyAuth.apply(spec);
        Response response = spec.post(loginUrl);
        if (response.statusCode() != 200) {
            throw new AuthenticationException("Login JWT falló con status " + response.statusCode());
        }
        Number expiresIn = response.jsonPath().get("expiresIn");
        return new TokenResult(response.jsonPath().getString("token"),
                expiresIn == null ? 3600 : expiresIn.longValue());
    }
}
