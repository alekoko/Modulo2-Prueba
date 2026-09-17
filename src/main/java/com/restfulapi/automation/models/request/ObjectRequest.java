package com.restfulapi.automation.models.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ObjectRequest(
        @JsonProperty("name") String name,
        @JsonProperty("data") DeviceData data) {
}
