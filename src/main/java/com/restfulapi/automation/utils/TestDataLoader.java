package com.restfulapi.automation.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.restfulapi.automation.config.ConfigManager;

/** Acceso a datos de prueba externos (JSON) mediante JSON Pointer, p.ej. "/objects/create". */
public final class TestDataLoader {

    private static final JsonNode ROOT =
            JsonUtils.readResource(ConfigManager.get("testdata.file", "testdata/objects.json"));

    private TestDataLoader() {
    }

    public static JsonNode node(String pointer) {
        JsonNode node = ROOT.at(pointer);
        if (node.isMissingNode()) {
            throw new IllegalArgumentException("Dato de prueba no encontrado: " + pointer);
        }
        return node;
    }

    public static <T> T get(String pointer, Class<T> type) {
        return JsonUtils.mapper().convertValue(node(pointer), type);
    }

    public static String getString(String pointer) {
        return node(pointer).asText();
    }

    public static int getInt(String pointer) {
        return node(pointer).asInt();
    }
}
