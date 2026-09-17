package com.restfulapi.automation.validators;

import com.fasterxml.jackson.databind.JsonNode;
import com.restfulapi.automation.utils.JsonUtils;
import io.qameta.allure.Step;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.hamcrest.MatcherAssert;
import org.testng.asserts.SoftAssert;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ResponseValidator {

    private final Response response;
    private final SoftAssert softly = new SoftAssert();
    private JsonNode jsonBody;

    private ResponseValidator(Response response) {
        this.response = Objects.requireNonNull(response, "La respuesta es nula");
    }

    public static ResponseValidator verify(Response response) {
        return new ResponseValidator(response);
    }

    @Step("Validar status code = {expected}")
    public ResponseValidator statusCode(int expected) {
        softly.assertEquals(response.statusCode(), expected,
                "Status code inesperado. Body: " + response.asString() + " ->");
        return this;
    }

    @Step("Validar SLA: tiempo de respuesta <= {maxMillis} ms")
    public ResponseValidator responseTimeWithin(long maxMillis) {
        long elapsed = response.getTime();
        softly.assertTrue(elapsed <= maxMillis, "SLA incumplido: " + elapsed + " ms > " + maxMillis + " ms");
        return this;
    }

    @Step("Validar header Content-Type = application/json")
    public ResponseValidator contentTypeIsJson() {
        String contentType = response.getContentType();
        softly.assertTrue(contentType != null && contentType.toLowerCase(Locale.ROOT).contains("application/json"),
                "Content-Type inesperado: " + contentType);
        return this;
    }

    @Step("Validar headers obligatorios presentes: {headerNames}")
    public ResponseValidator headersPresent(List<String> headerNames) {
        headerNames.forEach(h -> softly.assertTrue(response.getHeaders().hasHeaderWithName(h), "Header ausente: " + h));
        return this;
    }

    @Step("Validar integridad: el body es un JSON bien formado")
    public ResponseValidator isValidJson() {
        body();
        return this;
    }

    @Step("Validar estructura contra JSON Schema: {schemaPath}")
    public ResponseValidator matchesSchema(String schemaPath) {
        try {
            MatcherAssert.assertThat(response.asString(), JsonSchemaValidator.matchesJsonSchemaInClasspath(schemaPath));
        } catch (AssertionError | RuntimeException e) {
            softly.fail("JSON Schema no cumple (" + schemaPath + "): " + e.getMessage());
        }
        return this;
    }

    @Step("Validar campo '{field}' = '{expected}'")
    public ResponseValidator bodyFieldEquals(String field, String expected) {
        JsonNode node = field(field);
        softly.assertEquals(node == null ? null : node.asText(), expected, "Valor inesperado en '" + field + "'");
        return this;
    }

    @Step("Validar campo '{field}' contiene '{fragment}'")
    public ResponseValidator bodyFieldContains(String field, String fragment) {
        JsonNode node = field(field);
        String actual = node == null ? null : node.asText();
        softly.assertTrue(actual != null && actual.contains(fragment),
                "'" + field + "' = '" + actual + "' no contiene '" + fragment + "'");
        return this;
    }

    @Step("Validar campo '{field}' presente y no vacío")
    public ResponseValidator bodyFieldNotBlank(String field) {
        JsonNode node = field(field);
        softly.assertTrue(node != null && !node.isNull() && !node.asText().isBlank(), "'" + field + "' vacío o ausente");
        return this;
    }

    @Step("Validar que 'data' de la respuesta refleja los campos enviados en la petición")
    public ResponseValidator dataMatches(Object expectedData) {
        JsonNode expected = JsonUtils.mapper().valueToTree(expectedData);
        JsonNode actual = field("data");
        if (actual == null || !actual.isObject()) {
            softly.fail("'data' ausente o no es un objeto en la respuesta");
            return this;
        }
        for (Iterator<String> it = expected.fieldNames(); it.hasNext(); ) {
            String name = it.next();
            softly.assertEquals(actual.get(name), expected.get(name), "Campo data['" + name + "'] no coincide");
        }
        return this;
    }

    @Step("Validar que '{field}' es una fecha válida y reciente (tolerancia {tolerance})")
    public ResponseValidator timestampIsRecent(String field, Duration tolerance) {
        JsonNode node = field(field);
        Instant value;
        if (node == null || node.isNull()) {
            softly.fail("'" + field + "' ausente");
            return this;
        } else if (node.isIntegralNumber()) {
            value = Instant.ofEpochMilli(node.asLong());
        } else {
            try {
                value = Instant.parse(node.asText());
            } catch (DateTimeParseException e) {
                softly.fail("'" + field + "' no es epoch ms ni ISO-8601: " + node);
                return this;
            }
        }
        Duration drift = Duration.between(value, Instant.now()).abs();
        softly.assertTrue(drift.compareTo(tolerance) <= 0,
                "'" + field + "' = " + value + " difiere " + drift.toSeconds() + " s de la hora actual (máx "
                        + tolerance.toSeconds() + " s)");
        return this;
    }

    @Step("Consolidar resultado de las aserciones")
    public void assertAll() {
        softly.assertAll();
    }

    private JsonNode body() {
        if (jsonBody == null) {
            try {
                jsonBody = JsonUtils.readTree(response.asString());
            } catch (Exception e) {
                softly.fail("El body no es JSON válido: " + e.getMessage() + " | body=" + response.asString());
            }
        }
        return jsonBody;
    }

    private JsonNode field(String name) {
        JsonNode root = body();
        return root == null ? null : root.get(name);
    }
}
