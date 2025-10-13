package com.acme.employee.dto;

import java.util.Map;
import java.util.Optional;

import java.util.Objects;

public record EmployeeQueryParams(
        int page,
        int size,
        String sortField,
        String sortDirection,
        String searchTerm,
        Map<String, String> filters) {

    public EmployeeQueryParams {
        filters = filters == null ? Map.of() : Map.copyOf(filters);
    }

    public int pageOrDefault() {
        return page >= 0 ? page : 0;
    }

    public int sizeOrDefault() {
        return size > 0 ? size : 20;
    }

    public Optional<String> sortFieldOptional() {
        return Optional.ofNullable(sortField);
    }

    public Optional<String> sortDirectionOptional() {
        return Optional.ofNullable(sortDirection);
    }

    public Optional<String> searchTermOptional() {
        return Optional.ofNullable(searchTerm);
    }
}
