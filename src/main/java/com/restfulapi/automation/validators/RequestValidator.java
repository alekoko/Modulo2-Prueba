package com.restfulapi.automation.validators;

import com.restfulapi.automation.config.ConfigManager;
import com.restfulapi.automation.exceptions.InvalidRequestException;
import com.restfulapi.automation.models.request.DeviceData;
import com.restfulapi.automation.models.request.ObjectRequest;
import com.restfulapi.automation.utils.JsonUtils;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/** Valida los campos de la petición*/
public final class RequestValidator {

    private RequestValidator() {
    }

    @Step("Validar campos de la petición antes del envío")
    public static void validate(ObjectRequest request) {
        List<String> violations = new ArrayList<>();
        if (request == null) {
            throw new InvalidRequestException(List.of("El payload no puede ser nulo"));
        }
        int maxName = ConfigManager.getInt("validation.name.max.length", 100);
        if (request.name() == null || request.name().isBlank()) {
            violations.add("'name' es obligatorio");
        } else if (request.name().length() > maxName) {
            violations.add("'name' excede " + maxName + " caracteres");
        }

        DeviceData data = request.data();
        if (data == null) {
            violations.add("'data' es obligatorio");
        } else {
            int minYear = ConfigManager.getInt("validation.year.min", 1970);
            int maxYear = Year.now().getValue() + 1;
            if (data.year() == null || data.year() < minYear || data.year() > maxYear) {
                violations.add("'data.year' debe estar entre " + minYear + " y " + maxYear);
            }
            if (data.price() == null || data.price() < 0) {
                violations.add("'data.price' es obligatorio y no puede ser negativo");
            }
            if (data.cpuModel() != null && data.cpuModel().isBlank()) {
                violations.add("'data.CPU model' no puede estar vacío");
            }
        }

        Allure.addAttachment("Payload validado", "application/json", JsonUtils.toJson(request), ".json");
        if (!violations.isEmpty()) {
            throw new InvalidRequestException(violations);
        }
    }
}
