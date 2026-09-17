package com.restfulapi.automation.client;

import com.restfulapi.automation.config.ConfigManager;
import com.restfulapi.automation.utils.JsonUtils;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ApiLoggingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger("API");

    private final Set<String> maskedHeaders = ConfigManager.getList("log.mask.headers").stream()
            .map(h -> h.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());

    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        LOG.info("""

                >>>>>>>>>> REQUEST [{}] >>>>>>>>>>
                {} {}
                Headers: {}
                Body: {}""",
                traceId, request.getMethod(), request.getURI(), headers(request.getHeaders()), body(request.getBody()));
        try {
            Response response = ctx.next(request, responseSpec);
            LOG.info("""

                    <<<<<<<<<< RESPONSE [{}] <<<<<<<<<<
                    Status: {} | Tiempo: {} ms
                    Headers: {}
                    Body: {}""",
                    traceId, response.getStatusLine(), response.getTime(), headers(response.getHeaders()),
                    response.asPrettyString());
            return response;
        } catch (RuntimeException e) {
            LOG.error("!!!!!!!!!! SIN RESPUESTA [{}] {} {} -> {}", traceId, request.getMethod(), request.getURI(),
                    e.toString());
            throw e;
        }
    }

    private String headers(Headers headers) {
        return headers.asList().stream()
                .map(h -> h.getName() + "=" + (maskedHeaders.contains(h.getName().toLowerCase(Locale.ROOT))
                        ? "****" : h.getValue()))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String body(Object body) {
        if (body == null) {
            return "<vacío>";
        }
        String raw = body.toString();
        try {
            return JsonUtils.pretty(raw);
        } catch (Exception e) {
            return raw;
        }
    }
}
