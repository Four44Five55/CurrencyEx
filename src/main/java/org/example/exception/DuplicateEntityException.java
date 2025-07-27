package org.example.exception;

public class DuplicateEntityException extends Exception {

    public DuplicateEntityException(String entityName, String identifier) {
        super(String.format("%s with identifier '%s' already exists.", entityName, identifier));
    }
}
