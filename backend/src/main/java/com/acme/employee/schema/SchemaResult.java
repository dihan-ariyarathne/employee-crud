package com.acme.employee.schema;

import java.time.Instant;
import java.util.Map;

public record SchemaResult(
        String collection,
        int sampleSize,
        Instant generatedAt,
        Map<String, SchemaField> fields) {
}

