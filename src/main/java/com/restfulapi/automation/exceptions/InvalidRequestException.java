package com.restfulapi.automation.exceptions;

import java.util.List;

/** El payload no cumple las reglas de validación y no debe enviarse. */
public class InvalidRequestException extends FrameworkException {
    private final List<String> violations;

    public InvalidRequestException(List<String> violations) {
        super("Payload inválido: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
