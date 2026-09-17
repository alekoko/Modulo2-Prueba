package com.restfulapi.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/** createdAt/updatedAt como JsonNode: el servicio devuelve epoch ms aunque la documentación indica ISO-8601. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ObjectResponse(String id, String name, Map<String, Object> data, JsonNode createdAt, JsonNode updatedAt) {
}
