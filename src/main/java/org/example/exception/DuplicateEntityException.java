package org.example.exception;

public class DuplicateEntityException extends EntityException  {
    public DuplicateEntityException(String entityName, String code) {
        super(String.format("%s с кодом '%s' уже существует.", entityName, code));
    }
}
