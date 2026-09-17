package com.restfulapi.automation.models.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeviceData(
        @JsonProperty("year") Integer year,
        @JsonProperty("price") Double price,
        @JsonProperty("CPU model") String cpuModel,
        @JsonProperty("Hard disk size") String hardDiskSize,
        @JsonProperty("color") String color) {
}
