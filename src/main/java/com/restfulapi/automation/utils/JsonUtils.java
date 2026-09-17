package com.restfulapi.automation.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.restfulapi.automation.exceptions.UnexpectedResponseException;
import io.restassured.response.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public final class JsonUtils {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    private JsonUtils() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se pudo serializar a JSON", e);
        }
    }

    public static String pretty(String json) throws JsonProcessingException {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(MAPPER.readTree(json));
    }

    public static JsonNode readTree(String json) throws JsonProcessingException {
        return MAPPER.readTree(json);
    }

    public static JsonNode readResource(String classpathResource) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("Recurso no encontrado en classpath: " + classpathResource);
            }
            return MAPPER.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Error leyendo " + classpathResource, e);
        }
    }

    /** Deserializa la respuesta; si no es JSON válido lanza una excepción con status y body para diagnóstico. */
    public static <T> T fromResponse(Response response, Class<T> type) {
        String body = response.asString();
        try {
            return MAPPER.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new UnexpectedResponseException(String.format(
                    "Respuesta no deserializable a %s. status=%d, content-type=%s, body=%s",
                    type.getSimpleName(), response.statusCode(), response.getContentType(), abbreviate(body)), e);
        }
    }

    private static String abbreviate(String s) {
        return s == null ? "null" : (s.length() > 500 ? s.substring(0, 500) + "..." : s);
    }
}
