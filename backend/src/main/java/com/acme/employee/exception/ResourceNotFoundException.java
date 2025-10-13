package com.acme.employee.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, String id) {
        super(resource + " with id %s not found".formatted(id));
    }
}

