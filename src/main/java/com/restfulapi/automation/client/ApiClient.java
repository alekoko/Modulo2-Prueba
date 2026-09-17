package com.restfulapi.automation.client;

import com.restfulapi.automation.auth.AuthStrategy;
import com.restfulapi.automation.config.ConfigManager;
import com.restfulapi.automation.exceptions.ApiCallException;
import com.restfulapi.automation.utils.JsonUtils;
import io.qameta.allure.Allure;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

public final class ApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(ApiClient.class);

    private final AuthStrategy authStrategy;
    private final RequestSpecification baseSpec;
    private final int maxAttempts;
    private final long backoffMillis;

    public ApiClient(AuthStrategy authStrategy) {
        this.authStrategy = Objects.requireNonNull(authStrategy, "authStrategy");
        this.maxAttempts = Math.max(1, ConfigManager.getInt("http.retry.max.attempts", 1));
        this.backoffMillis = ConfigManager.getLong("http.retry.backoff.ms", 1000);

        RestAssuredConfig config = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", ConfigManager.getInt("http.connect.timeout.ms", 10_000))
                        .setParam("http.socket.timeout", ConfigManager.getInt("http.read.timeout.ms", 15_000)))
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, charset) -> JsonUtils.mapper()));

        this.baseSpec = new RequestSpecBuilder()
                .setBaseUri(ConfigManager.getRequired("base.url"))
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(config)
                .addFilter(new ApiLoggingFilter())
                .addFilter(new AllureRestAssured())
                .build();
    }

    public Response send(ApiRequest request) {
        String path = request.endpoint().path();
        Exception lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                RequestSpecification spec = RestAssured.given().spec(baseSpec);
                authStrategy.apply(spec);
                if (!request.pathParams().isEmpty()) {
                    spec.pathParams(request.pathParams());
                }
                if (!request.queryParams().isEmpty()) {
                    spec.queryParams(request.queryParams());
                }
                if (!request.headers().isEmpty()) {
                    spec.headers(request.headers());
                }
                if (request.body() != null) {
                    spec.body(request.body());
                }
                Response response = spec.request(request.method(), path);
                if (response.statusCode() == 429) {
                    LOG.warn("Límite de peticiones alcanzado (429) en {} {}", request.method(), path);
                }
                return response;
            } catch (Exception e) {
                lastError = e;
                if (!isNetworkFailure(e) || attempt == maxAttempts) {
                    break;
                }
                LOG.warn("Fallo de red en intento {}/{} para {} {}: {}. Reintentando...",
                        attempt, maxAttempts, request.method(), path, rootMessage(e));
                sleep(backoffMillis * attempt);
            }
        }

        String message = String.format("Fallo al invocar %s %s: %s", request.method(), path, rootMessage(lastError));
        LOG.error(message);
        Allure.addAttachment("Error de conexión / infraestructura", "text/plain", stackTrace(lastError), ".txt");
        throw new ApiCallException(message, lastError);
    }

    public static boolean isNetworkFailure(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof IOException) {
                return true;
            }
        }
        return false;
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root != null && root.getCause() != null) {
            root = root.getCause();
        }
        return root == null ? "desconocido" : root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    private static String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        if (t != null) {
            t.printStackTrace(new PrintWriter(sw));
        }
        return sw.toString();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
