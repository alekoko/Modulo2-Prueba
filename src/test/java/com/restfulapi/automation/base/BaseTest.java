package com.restfulapi.automation.base;

import com.restfulapi.automation.auth.AuthFactory;
import com.restfulapi.automation.client.ApiClient;
import com.restfulapi.automation.config.ConfigManager;
import com.restfulapi.automation.services.ObjectsService;
import com.restfulapi.automation.utils.TestDataLoader;
import com.restfulapi.automation.validators.ResponseValidator;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;

import java.time.Duration;
import java.util.List;

public abstract class BaseTest {

    protected ObjectsService objectsService;
    protected long slaMillis;
    protected List<String> requiredHeaders;
    protected Duration timestampTolerance;

    protected static final int HTTP_OK = TestDataLoader.getInt("/expected/status/ok");
    protected static final int HTTP_NOT_FOUND = TestDataLoader.getInt("/expected/status/notFound");
    protected static final int HTTP_METHOD_NOT_ALLOWED = TestDataLoader.getInt("/expected/status/methodNotAllowed");

    @BeforeClass(alwaysRun = true)
    public void setUpApiClient() {
        objectsService = new ObjectsService(new ApiClient(AuthFactory.fromConfig()));
        slaMillis = ConfigManager.getLong("sla.response.time.ms", 3000);
        requiredHeaders = ConfigManager.getList("validation.required.headers");
        timestampTolerance = Duration.ofMinutes(ConfigManager.getLong("validation.timestamp.tolerance.minutes", 10));
    }

    /** Validaciones transversales a todos los casos: status, SLA, headers e integridad JSON. */
    protected ResponseValidator verifyCommon(Response response, int expectedStatus) {
        return ResponseValidator.verify(response)
                .statusCode(expectedStatus)
                .responseTimeWithin(slaMillis)
                .contentTypeIsJson()
                .headersPresent(requiredHeaders)
                .isValidJson();
    }
}
