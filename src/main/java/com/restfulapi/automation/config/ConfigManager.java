package com.restfulapi.automation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public final class ConfigManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z0-9_.\\-]+)(?::([^}]*))?}");
    private static final Properties PROPERTIES = new Properties();

    static {
        load("config/config.properties", true);
        load("config/endpoints.properties", true);
        load("config/email.properties", false);
        load("config/env/" + activeEnvironment() + ".properties", false);
        LOG.info("Configuración cargada para el entorno '{}'", activeEnvironment());
    }

    private ConfigManager() {
    }

    public static String activeEnvironment() {
        String env = System.getProperty("env");
        if (isBlank(env)) {
            env = System.getenv("TEST_ENV");
        }
        return isBlank(env) ? "qa" : env.trim().toLowerCase(Locale.ROOT);
    }

    public static Optional<String> find(String key) {
        String value = System.getProperty(key);
        if (isBlank(value)) {
            value = System.getenv(toEnvKey(key));
        }
        if (isBlank(value)) {
            value = PROPERTIES.getProperty(key);
        }
        if (value == null) {
            return Optional.empty();
        }
        String resolved = resolvePlaceholders(value.trim());
        return isBlank(resolved) ? Optional.empty() : Optional.of(resolved);
    }

    public static String get(String key, String defaultValue) {
        return find(key).orElse(defaultValue);
    }

    public static String getRequired(String key) {
        return find(key).orElseThrow(() -> new IllegalStateException(
                "Configuración requerida no encontrada: '" + key + "'. Defínala en properties, con -D"
                        + key + " o con la variable de entorno " + toEnvKey(key)));
    }

    public static int getInt(String key, int defaultValue) {
        return find(key).map(Integer::parseInt).orElse(defaultValue);
    }

    public static long getLong(String key, long defaultValue) {
        return find(key).map(Long::parseLong).orElse(defaultValue);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return find(key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    public static List<String> getList(String key) {
        return find(key)
                .map(v -> Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList())
                .orElse(List.of());
    }

    static String toEnvKey(String key) {
        return key.toUpperCase(Locale.ROOT).replaceAll("[.\\-]", "_");
    }

    private static String resolvePlaceholders(String value) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String fallback = matcher.group(2) == null ? "" : matcher.group(2);
            String replacement = System.getProperty(name);
            if (isBlank(replacement)) {
                replacement = System.getenv(name);
            }
            if (isBlank(replacement)) {
                replacement = fallback;
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static void load(String resource, boolean required) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                if (required) {
                    throw new IllegalStateException("No se encontró el archivo de configuración: " + resource);
                }
                LOG.debug("Archivo de configuración opcional no encontrado: {}", resource);
                return;
            }
            Properties props = new Properties();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            PROPERTIES.putAll(props);
        } catch (IOException e) {
            throw new UncheckedIOException("Error leyendo " + resource, e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}