package com.restfulapi.automation.auth;

import io.restassured.specification.RequestSpecification;

public final class NoAuth implements AuthStrategy {
    @Override
    public RequestSpecification apply(RequestSpecification spec) {
        return spec;
    }

    @Override
    public String description() {
        return "NONE";
    }
}
