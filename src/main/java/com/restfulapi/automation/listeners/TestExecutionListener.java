package com.restfulapi.automation.listeners;

import com.restfulapi.automation.config.ConfigManager;
import com.restfulapi.automation.exceptions.ApiCallException;
import com.restfulapi.automation.exceptions.InvalidRequestException;
import com.restfulapi.automation.exceptions.UnexpectedResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class TestExecutionListener implements ITestListener, ISuiteListener {

    private static final Logger LOG = LoggerFactory.getLogger("EXECUTION");

    @Override
    public void onStart(ISuite suite) {
        Path results = Paths.get(System.getProperty("allure.results.directory", "target/allure-results"));
        try {
            Files.createDirectories(results);
            Properties env = new Properties();
            env.setProperty("Entorno", ConfigManager.activeEnvironment());
            env.setProperty("Base.URL", ConfigManager.get("base.url", "-"));
            env.setProperty("Autenticacion", ConfigManager.get("auth.types", "NONE"));
            env.setProperty("SLA.ms", ConfigManager.get("sla.response.time.ms", "-"));
            env.setProperty("Java", System.getProperty("java.version"));
            env.setProperty("SO", System.getProperty("os.name"));
            try (OutputStream out = Files.newOutputStream(results.resolve("environment.properties"))) {
                env.store(out, "Allure environment");
            }
            try (InputStream categories = getClass().getClassLoader().getResourceAsStream("allure/categories.json")) {
                if (categories != null) {
                    Files.copy(categories, results.resolve("categories.json"), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            LOG.warn("No se pudieron escribir los metadatos de Allure: {}", e.getMessage());
        }
        LOG.info("==== Inicio de suite '{}' | entorno={} ====", suite.getName(), ConfigManager.activeEnvironment());
    }

    @Override
    public void onTestStart(ITestResult result) {
        LOG.info("---- INICIO: {} ----", describe(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOG.info("---- PASÓ: {} ({} ms) ----", describe(result), result.getEndMillis() - result.getStartMillis());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Throwable t = result.getThrowable();
        LOG.error("---- FALLÓ [{}]: {} -> {} ----", classify(t), describe(result), t == null ? "" : t.getMessage());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        Throwable t = result.getThrowable();
        LOG.warn("---- OMITIDO: {} -> {} ----", describe(result), t == null ? "sin detalle" : t.getMessage());
    }

    @Override
    public void onFinish(ITestContext context) {
        LOG.info("==== Fin de '{}': pasaron={}, fallaron={}, omitidos={} ====", context.getName(),
                context.getPassedTests().size(), context.getFailedTests().size(), context.getSkippedTests().size());
    }

    private static String classify(Throwable t) {
        if (t instanceof ApiCallException) {
            return "INFRAESTRUCTURA";
        }
        if (t instanceof UnexpectedResponseException) {
            return "RESPUESTA_INESPERADA";
        }
        if (t instanceof InvalidRequestException) {
            return "PAYLOAD_INVALIDO";
        }
        return t instanceof AssertionError ? "FUNCIONAL" : "ERROR_TECNICO";
    }

    private static String describe(ITestResult result) {
        String description = result.getMethod().getDescription();
        return description != null && !description.isBlank() ? description : result.getMethod().getMethodName();
    }
}
