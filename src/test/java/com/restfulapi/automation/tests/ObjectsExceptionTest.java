package com.restfulapi.automation.tests;

import com.restfulapi.automation.base.BaseTest;
import com.restfulapi.automation.builders.ObjectRequestBuilder;
import com.restfulapi.automation.models.request.ObjectRequest;
import com.restfulapi.automation.utils.TestDataLoader;
import com.restfulapi.automation.validators.Schemas;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

@Epic("restful-api.dev")
@Feature("Objects - Manejo de excepciones")
public class ObjectsExceptionTest extends BaseTest {

    @Test(priority = 5, description = "TC05 - Consultar objeto inexistente retorna 404 (GET /objects/{id})")
    @Story("Recurso no encontrado")
    @Severity(SeverityLevel.NORMAL)
    public void tc05GetNonExistentObject() {
        String id = TestDataLoader.getString("/objects/negative/nonExistentId");

        Response response = objectsService.getById(id);

        verifyCommon(response, HTTP_NOT_FOUND)
                .matchesSchema(Schemas.ERROR)
                .bodyFieldContains("error", id)
                .bodyFieldContains("error", TestDataLoader.getString("/expected/messages/notFound"))
                .assertAll();
    }

    @Test(priority = 6, description = "TC06 - Actualizar objeto reservado retorna 405 (PUT /objects/{id})")
    @Story("Operación no permitida")
    @Severity(SeverityLevel.NORMAL)
    public void tc06UpdateReservedObject() {
        String reservedId = TestDataLoader.getString("/objects/negative/reservedId");
        ObjectRequest payload = ObjectRequestBuilder.fromTestData("/objects/update").build();

        Response response = objectsService.update(reservedId, payload);

        verifyCommon(response, HTTP_METHOD_NOT_ALLOWED)
                .matchesSchema(Schemas.ERROR)
                .bodyFieldContains("error", TestDataLoader.getString("/expected/messages/reservedId"))
                .assertAll();
    }
}
