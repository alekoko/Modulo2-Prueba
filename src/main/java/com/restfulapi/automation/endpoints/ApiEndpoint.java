package com.restfulapi.automation.endpoints;

import com.restfulapi.automation.config.ConfigManager;


public enum ApiEndpoint {

    OBJECTS("endpoint.objects"),
    OBJECT_BY_ID("endpoint.object.by.id"),
    AUTH_LOGIN("endpoint.auth.login");

    private final String configKey;

    ApiEndpoint(String configKey) {
        this.configKey = configKey;
    }

    public String path() {
        return ConfigManager.getRequired(configKey);
    }
}
