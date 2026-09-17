package com.restfulapi.automation.tests;

import com.restfulapi.automation.base.BaseTest;
import com.restfulapi.automation.builders.ObjectRequestBuilder;
import com.restfulapi.automation.models.request.ObjectRequest;
import com.restfulapi.automation.models.response.ObjectResponse;
import com.restfulapi.automation.utils.JsonUtils;
import com.restfulapi.automation.utils.TestDataLoader;
import com.restfulapi.automation.validators.ResponseValidator;
import com.restfulapi.automation.validators.Schemas;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

@Epic("restful-api.dev")
@Feature("Objects - Flujos exitosos (CRUD)")
public class ObjectsSuccessTest extends BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectsSuccessTest.class);

    private String objectId;
    private ObjectRequest currentPayload;

    @Test(priority = 1, description = "TC01 - Crear objeto con payload válido (POST /objects)")
    @Story("Crear objeto")
    @Severity(SeverityLevel.BLOCKER)
    public void tc01CreateObject() {
        currentPayload = ObjectRequestBuilder.fromTestData("/objects/create").withUniqueNameSuffix().build();

        Response response = objectsService.create(currentPayload);
        objectId = extractId(response);

        verifyCommon(response, HTTP_OK)
                .matchesSchema(Schemas.OBJECT_CREATED)
                .bodyFieldNotBlank("id")
                .bodyFieldEquals("name", currentPayload.name())
                .dataMatches(currentPayload.data())
                .timestampIsRecent("createdAt", timestampTolerance)
                .assertAll();
    }

    @Test(priority = 2, description = "TC02 - Consultar objeto creado por id (GET /objects/{id})")
    @Story("Consultar objeto")
    @Severity(SeverityLevel.CRITICAL)
    public void tc02GetObjectById() {
        String id = ensureObjectExists();

        Response response = objectsService.getById(id);

        verifyCommon(response, HTTP_OK)
                .matchesSchema(Schemas.OBJECT)
                .bodyFieldEquals("id", id)
                .bodyFieldEquals("name", currentPayload.name())
                .dataMatches(currentPayload.data())
                .assertAll();
    }

    @Test(priority = 3, description = "TC03 - Actualizar objeto completo (PUT /objects/{id})")
    @Story("Actualizar objeto")
    @Severity(SeverityLevel.CRITICAL)
    public void tc03UpdateObject() {
        String id = ensureObjectExists();
        ObjectRequest update = ObjectRequestBuilder.fromTestData("/objects/update").withUniqueNameSuffix().build();

        Response response = objectsService.update(id, update);
        if (response.statusCode() == HTTP_OK) {
            currentPayload = update;
        }

        verifyCommon(response, HTTP_OK)
                .matchesSchema(Schemas.OBJECT_UPDATED)
                .bodyFieldEquals("id", id)
                .bodyFieldEquals("name", update.name())
                .dataMatches(update.data())
                .timestampIsRecent("updatedAt", timestampTolerance)
                .assertAll();
    }

    @Test(priority = 4, description = "TC04 - Eliminar objeto y verificar que ya no existe (DELETE /objects/{id})")
    @Story("Eliminar objeto")
    @Severity(SeverityLevel.CRITICAL)
    public void tc04DeleteObject() {
        String id = ensureObjectExists();

        Response response = objectsService.delete(id);
        if (response.statusCode() == HTTP_OK) {
            objectId = null;
        }
        verifyCommon(response, HTTP_OK)
                .matchesSchema(Schemas.OBJECT_DELETED)
                .bodyFieldContains("message", id)
                .bodyFieldContains("message", TestDataLoader.getString("/expected/messages/deleted"))
                .assertAll();

        Response afterDelete = objectsService.getById(id);
        ResponseValidator.verify(afterDelete)
                .statusCode(HTTP_NOT_FOUND)
                .matchesSchema(Schemas.ERROR)
                .assertAll();
    }

    /** Hace cada caso independiente: si TC01 no dejó un objeto, lo crea como precondición. */
    private String ensureObjectExists() {
        if (objectId != null) {
            return objectId;
        }
        currentPayload = ObjectRequestBuilder.fromTestData("/objects/create").withUniqueNameSuffix().build();
        Response response = objectsService.create(currentPayload);
        objectId = extractId(response);
        if (objectId == null) {
            throw new SkipException("Precondición no cumplida: no se pudo crear el objeto base. status="
                    + response.statusCode() + " body=" + response.asString());
        }
        return objectId;
    }

    private String extractId(Response response) {
        if (response.statusCode() != HTTP_OK) {
            return null;
        }
        try {
            return JsonUtils.fromResponse(response, ObjectResponse.class).id();
        } catch (RuntimeException e) {
            LOG.warn("No se pudo extraer el id: {}", e.getMessage());
            return null;
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() {
        if (objectId == null || objectsService == null) {
            return;
        }
        try {
            objectsService.delete(objectId);
            LOG.info("Limpieza: objeto {} eliminado", objectId);
        } catch (RuntimeException e) {
            LOG.warn("Limpieza fallida para {}: {}", objectId, e.getMessage());
        }
    }
}
