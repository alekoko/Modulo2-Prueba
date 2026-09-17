package com.restfulapi.automation.exceptions;

/** Fallo de infraestructura (conexión, timeout, DNS, TLS) al invocar un servicio. */
public class ApiCallException extends FrameworkException {
    public ApiCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
