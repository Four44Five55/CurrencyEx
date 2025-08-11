package org.example.exception;

public class EntityNotFoundException extends EntityException  {
    public EntityNotFoundException(String entityName, String code) {
        super(String.format("%s c кодом '%s' не найдена.", entityName, code));
    }
}
