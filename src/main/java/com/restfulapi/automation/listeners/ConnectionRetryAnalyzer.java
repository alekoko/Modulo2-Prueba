package com.restfulapi.automation.listeners;

import com.restfulapi.automation.client.ApiClient;
import com.restfulapi.automation.config.ConfigManager;
import com.restfulapi.automation.exceptions.ApiCallException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/** Reintenta un test SOLO si falló por infraestructura */
public class ConnectionRetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionRetryAnalyzer.class);
    private final int maxRetries = ConfigManager.getInt("test.retry.max", 1);
    private int retries;

    @Override
    public boolean retry(ITestResult result) {
        Throwable t = result.getThrowable();
        boolean infra = t instanceof ApiCallException || ApiClient.isNetworkFailure(t);
        if (infra && retries < maxRetries) {
            retries++;
            LOG.warn("Reintentando '{}' ({}/{}) por fallo de infraestructura: {}",
                    result.getMethod().getMethodName(), retries, maxRetries, t.getMessage());
            return true;
        }
        return false;
    }
}
