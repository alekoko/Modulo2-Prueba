package com.restfulapi.automation.builders;

import com.restfulapi.automation.models.request.ObjectRequest;
import com.restfulapi.automation.utils.TestDataLoader;

import java.util.UUID;
import java.util.function.UnaryOperator;

public final class ObjectRequestBuilder {

    private String name;
    private DeviceDataBuilder data = DeviceDataBuilder.aDeviceData();

    private ObjectRequestBuilder() {
    }

    public static ObjectRequestBuilder anObject() {
        return new ObjectRequestBuilder();
    }

    public static ObjectRequestBuilder fromTestData(String jsonPointer) {
        ObjectRequest template = TestDataLoader.get(jsonPointer, ObjectRequest.class);
        ObjectRequestBuilder builder = anObject().withName(template.name());
        builder.data = template.data() == null ? null : DeviceDataBuilder.from(template.data());
        return builder;
    }

    public ObjectRequestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ObjectRequestBuilder withUniqueNameSuffix() {
        this.name = name + " [" + UUID.randomUUID().toString().substring(0, 8) + "]";
        return this;
    }

    public ObjectRequestBuilder withData(UnaryOperator<DeviceDataBuilder> customizer) {
        this.data = customizer.apply(data == null ? DeviceDataBuilder.aDeviceData() : data);
        return this;
    }

    public ObjectRequestBuilder withoutData() {
        this.data = null;
        return this;
    }

    public ObjectRequest build() {
        return new ObjectRequest(name, data == null ? null : data.build());
    }
}
