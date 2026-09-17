package com.restfulapi.automation.auth;

import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Bearer token. El token se obtiene de un {@link Supplier}: puede ser estático (variable de entorno)
 * o dinámico (login JWT u OAuth2), resolviéndose en cada petición para soportar expiración.
 */
public final class BearerTokenAuth implements AuthStrategy {

    private final Supplier<String> tokenSupplier;
    private final Map<String, String> extraQueryParams;

    public BearerTokenAuth(Supplier<String> tokenSupplier, Map<String, String> extraQueryParams) {
        this.tokenSupplier = tokenSupplier;
        this.extraQueryParams = Map.copyOf(extraQueryParams);
    }

    @Override
    public RequestSpecification apply(RequestSpecification spec) {
        extraQueryParams.forEach(spec::queryParam);
        return spec.header("Authorization", "Bearer " + tokenSupplier.get());
    }

    @Override
    public String description() {
        return "BEARER";
    }
}
