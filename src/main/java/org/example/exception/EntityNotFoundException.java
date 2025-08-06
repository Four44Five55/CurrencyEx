package org.example.exception;

public class EntityNotFoundException extends RuntimeException {
     public EntityNotFoundException(String entityType, String identifier) {
        super(entityType + " with identifier '" + identifier + "' not found.");
    }
}
