package com.restfulapi.automation.exceptions;

/** La respuesta llegó pero no tiene el formato esperado (HTML, JSON corrupto, etc.). */
public class UnexpectedResponseException extends FrameworkException {
    public UnexpectedResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
