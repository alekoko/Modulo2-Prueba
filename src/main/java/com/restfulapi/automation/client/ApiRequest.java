package com.restfulapi.automation.client;

import com.restfulapi.automation.endpoints.ApiEndpoint;
import io.restassured.http.Method;

import java.util.LinkedHashMap;
import java.util.Map;

public record ApiRequest(Method method,
                         ApiEndpoint endpoint,
                         Map<String, Object> pathParams,
                         Map<String, Object> queryParams,
                         Map<String, String> headers,
                         Object body) {

    public static Builder builder(Method method, ApiEndpoint endpoint) {
        return new Builder(method, endpoint);
    }

    public static final class Builder {
        private final Method method;
        private final ApiEndpoint endpoint;
        private final Map<String, Object> pathParams = new LinkedHashMap<>();
        private final Map<String, Object> queryParams = new LinkedHashMap<>();
        private final Map<String, String> headers = new LinkedHashMap<>();
        private Object body;

        private Builder(Method method, ApiEndpoint endpoint) {
            this.method = method;
            this.endpoint = endpoint;
        }

        public Builder pathParam(String name, Object value) {
            pathParams.put(name, value);
            return this;
        }

        public Builder queryParam(String name, Object value) {
            queryParams.put(name, value);
            return this;
        }

        public Builder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Builder body(Object body) {
            this.body = body;
            return this;
        }

        public ApiRequest build() {
            return new ApiRequest(method, endpoint, Map.copyOf(pathParams), Map.copyOf(queryParams),
                    Map.copyOf(headers), body);
        }
    }
}
