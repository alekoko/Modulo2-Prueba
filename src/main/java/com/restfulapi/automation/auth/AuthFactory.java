package com.restfulapi.automation.auth;

import com.restfulapi.automation.config.ConfigManager;
import com.restfulapi.automation.endpoints.ApiEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AuthFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AuthFactory.class);

    private AuthFactory() {
    }

    public static AuthStrategy fromConfig() {
        List<String> types = ConfigManager.getList("auth.types");
        List<AuthStrategy> strategies = new ArrayList<>();
        AuthStrategy apiKey = null;

        for (String raw : types.isEmpty() ? List.of("NONE") : types) {
            AuthType type = AuthType.valueOf(raw.toUpperCase(Locale.ROOT));
            switch (type) {
                case NONE -> { /* sin autenticación */ }
                case API_KEY -> {
                    apiKey = new ApiKeyAuth(
                            ConfigManager.get("auth.apikey.name", "x-api-key"),
                            ConfigManager.getRequired("auth.apikey.value"),
                            ApiKeyAuth.Location.valueOf(ConfigManager.get("auth.apikey.location", "HEADER")));
                    strategies.add(apiKey);
                }
                case BEARER -> strategies.add(new BearerTokenAuth(bearerSupplier(apiKey), bearerQueryParams()));
                case OAUTH2 -> strategies.add(new BearerTokenAuth(new OAuth2ClientCredentialsProvider(
                        ConfigManager.getRequired("auth.oauth2.token.url"),
                        ConfigManager.getRequired("auth.oauth2.client.id"),
                        ConfigManager.getRequired("auth.oauth2.client.secret"),
                        ConfigManager.get("auth.oauth2.scope", "")), Map.of()));
            }
        }
        AuthStrategy result = strategies.isEmpty() ? new NoAuth() : new CompositeAuth(strategies);
        LOG.info("Autenticación configurada: {}", result.description());
        return result;
    }

    private static java.util.function.Supplier<String> bearerSupplier(AuthStrategy apiKey) {
        var staticToken = ConfigManager.find("auth.bearer.token");
        if (staticToken.isPresent()) {
            return staticToken::get;
        }
        String loginUrl = ConfigManager.getRequired("base.url") + ApiEndpoint.AUTH_LOGIN.path();
        return new JwtLoginTokenProvider(loginUrl,
                ConfigManager.getRequired("auth.bearer.login.email"),
                ConfigManager.getRequired("auth.bearer.login.password"),
                apiKey == null ? new NoAuth() : apiKey);
    }

    private static Map<String, String> bearerQueryParams() {
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : ConfigManager.getList("auth.bearer.query.params")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0].trim(), kv[1].trim());
            }
        }
        return params;
    }
}
