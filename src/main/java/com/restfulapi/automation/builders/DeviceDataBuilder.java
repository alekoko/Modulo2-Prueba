package com.restfulapi.automation.builders;

import com.restfulapi.automation.models.request.DeviceData;

public final class DeviceDataBuilder {

    private Integer year;
    private Double price;
    private String cpuModel;
    private String hardDiskSize;
    private String color;

    private DeviceDataBuilder() {
    }

    public static DeviceDataBuilder aDeviceData() {
        return new DeviceDataBuilder();
    }

    public static DeviceDataBuilder from(DeviceData source) {
        return aDeviceData().withYear(source.year()).withPrice(source.price()).withCpuModel(source.cpuModel())
                .withHardDiskSize(source.hardDiskSize()).withColor(source.color());
    }

    public DeviceDataBuilder withYear(Integer year) {
        this.year = year;
        return this;
    }

    public DeviceDataBuilder withPrice(Double price) {
        this.price = price;
        return this;
    }

    public DeviceDataBuilder withCpuModel(String cpuModel) {
        this.cpuModel = cpuModel;
        return this;
    }

    public DeviceDataBuilder withHardDiskSize(String hardDiskSize) {
        this.hardDiskSize = hardDiskSize;
        return this;
    }

    public DeviceDataBuilder withColor(String color) {
        this.color = color;
        return this;
    }

    public DeviceData build() {
        return new DeviceData(year, price, cpuModel, hardDiskSize, color);
    }
}
