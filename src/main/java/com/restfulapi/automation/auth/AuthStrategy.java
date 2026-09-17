package com.restfulapi.automation.auth;

import io.restassured.specification.RequestSpecification;

/** Patrón Strategy: cada mecanismo de autenticación decora la petición a su manera. */
public interface AuthStrategy {

    RequestSpecification apply(RequestSpecification spec);

    String description();
}
