package com.restfulapi.automation.auth;

import io.restassured.specification.RequestSpecification;

import java.util.List;
import java.util.stream.Collectors;

/** Combina estrategias (p.ej. API_KEY + BEARER, como exige restful-api.dev con auth-type=jwt). */
public final class CompositeAuth implements AuthStrategy {

    private final List<AuthStrategy> strategies;

    public CompositeAuth(List<AuthStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    @Override
    public RequestSpecification apply(RequestSpecification spec) {
        strategies.forEach(s -> s.apply(spec));
        return spec;
    }

    @Override
    public String description() {
        return strategies.stream().map(AuthStrategy::description).collect(Collectors.joining(" + "));
    }
}
