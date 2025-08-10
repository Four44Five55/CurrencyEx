package org.example.exception;

import java.util.Map;

public class ValidationException extends ApplicationException  {
    private final Map<String, String> errors;

    public ValidationException(final Map<String, String> errors) {
        super("Ошибка валидации");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
