package com.acme.employee.dto;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public record EmployeeUpsertRequest(
        @NotEmpty(message = "attributes must not be empty")
        Map<String, Object> attributes) {
}

