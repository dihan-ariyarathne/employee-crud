package com.acme.employee.dto;

import java.time.Instant;
import java.util.Map;

public record EmployeeResponse(
        String id,
        Map<String, Object> attributes,
        boolean deleted,
        Instant createdAt,
        Instant updatedAt) {
}

