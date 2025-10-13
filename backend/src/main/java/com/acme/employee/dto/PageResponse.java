package com.acme.employee.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int page,
        int size,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {
}

