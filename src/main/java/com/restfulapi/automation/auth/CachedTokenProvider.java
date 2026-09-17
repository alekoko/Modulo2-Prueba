package com.restfulapi.automation.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.function.Supplier;

/** Cachea un token y lo renueva automáticamente antes de su expiración. Thread-safe. */
public abstract class CachedTokenProvider implements Supplier<String> {

    private static final Logger LOG = LoggerFactory.getLogger(CachedTokenProvider.class);
    private static final long EXPIRY_SKEW_SECONDS = 30;

    private String token;
    private Instant expiresAt = Instant.EPOCH;

    @Override
    public synchronized String get() {
        if (token == null || Instant.now().isAfter(expiresAt)) {
            TokenResult result = fetchToken();
            token = result.token();
            expiresAt = Instant.now().plusSeconds(Math.max(0, result.expiresInSeconds() - EXPIRY_SKEW_SECONDS));
            LOG.info("Token obtenido dinámicamente ({}). Expira en {} s", getClass().getSimpleName(),
                    result.expiresInSeconds());
        }
        return token;
    }

    protected abstract TokenResult fetchToken();

    protected record TokenResult(String token, long expiresInSeconds) {
    }
}
