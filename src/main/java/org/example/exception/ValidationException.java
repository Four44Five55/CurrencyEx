package org.example.exception;

import java.util.Map;

public class ValidationException extends Exception {
    private final Map<String, String> errors;

    public ValidationException(final Map<String, String> errors) {
        super("Validation exception");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
