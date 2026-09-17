package com.restfulapi.automation.auth;

import io.restassured.specification.RequestSpecification;

public final class ApiKeyAuth implements AuthStrategy {

    public enum Location { HEADER, QUERY }

    private final String name;
    private final String value;
    private final Location location;

    public ApiKeyAuth(String name, String value, Location location) {
        this.name = name;
        this.value = value;
        this.location = location;
    }

    @Override
    public RequestSpecification apply(RequestSpecification spec) {
        return location == Location.HEADER ? spec.header(name, value) : spec.queryParam(name, value);
    }

    @Override
    public String description() {
        return "API_KEY(" + location + ":" + name + ")";
    }
}
