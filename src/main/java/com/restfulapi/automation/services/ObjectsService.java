package com.restfulapi.automation.services;

import com.restfulapi.automation.client.ApiClient;
import com.restfulapi.automation.client.ApiRequest;
import com.restfulapi.automation.endpoints.ApiEndpoint;
import com.restfulapi.automation.models.request.ObjectRequest;
import com.restfulapi.automation.validators.RequestValidator;
import io.qameta.allure.Step;
import io.restassured.http.Method;
import io.restassured.response.Response;

/** Capa de servicio: expresa las operaciones de negocio del recurso /objects. */
public class ObjectsService {

    private final ApiClient client;

    public ObjectsService(ApiClient client) {
        this.client = client;
    }

    @Step("POST objects: crear '{request.name}'")
    public Response create(ObjectRequest request) {
        RequestValidator.validate(request);
        return client.send(ApiRequest.builder(Method.POST, ApiEndpoint.OBJECTS).body(request).build());
    }

    @Step("GET objects/{id}")
    public Response getById(String id) {
        return client.send(ApiRequest.builder(Method.GET, ApiEndpoint.OBJECT_BY_ID).pathParam("id", id).build());
    }

    @Step("PUT objects/{id}: actualizar a '{request.name}'")
    public Response update(String id, ObjectRequest request) {
        RequestValidator.validate(request);
        return client.send(ApiRequest.builder(Method.PUT, ApiEndpoint.OBJECT_BY_ID)
                .pathParam("id", id).body(request).build());
    }

    @Step("DELETE objects/{id}")
    public Response delete(String id) {
        return client.send(ApiRequest.builder(Method.DELETE, ApiEndpoint.OBJECT_BY_ID).pathParam("id", id).build());
    }
}
